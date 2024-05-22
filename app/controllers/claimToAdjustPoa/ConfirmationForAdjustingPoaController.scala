/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.claimToAdjustPoa

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes.HomeController
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{PoAAmendmentData, SelectYourReason}
import models.core.Nino
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustPoaCalculationService, ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationForAdjustingPoaController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                      claimToAdjustService: ClaimToAdjustService,
                                                      val sessionService: PaymentOnAccountSessionService,
                                                      val calculationService: ClaimToAdjustPoaCalculationService,
                                                      val view: ConfirmationForAdjustingPoa,
                                                      implicit val itvcErrorHandler: ItvcErrorHandler,
                                                      auth: AuthenticatorPredicate,
                                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def isAmountZeroFromSession(implicit hc: HeaderCarrier): Future[Boolean] = sessionService.getMongo(hc, ec).flatMap {
    case Right(Some(newPoaData: PoAAmendmentData)) =>
      Future.successful(newPoaData.newPoAAmount.contains(BigDecimal(0)))
    case _ => Future.failed(new Exception(s"failed to retrieve session data."))
  }

  def dataFromSession(implicit hc: HeaderCarrier): Future[PoAAmendmentData] = sessionService.getMongo(hc, ec).flatMap {
    case Right(Some(newPoaData: PoAAmendmentData)) =>
      Future.successful(newPoaData)
    case _ => Future.failed(new Exception(s"failed to retrieve session data."))
  }

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      if (isEnabled(AdjustPaymentsOnAccount)) {
        {
          for {
            poaMaybe <- claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino))
            isAmountZero <- isAmountZeroFromSession
          } yield (poaMaybe, isAmountZero)
        } flatMap {
          case (Right(Some(poa)), isAmountZero) =>
            Future.successful(Ok(view(isAgent, poa.taxYear, isAmountZero)))
          case (Right(None), isAmountZero) => Logger("application").error(s"Failed to create PaymentOnAccount model, isAmountZero: $isAmountZero")
            Future.successful(showInternalServerError(isAgent))
          case (Left(ex), isAmountZero) =>
            Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}. isAmountZero: $isAmountZero")
            Future.failed(ex)
        }
      } else {
        Future.successful(
          Redirect(
            if (isAgent) HomeController.showAgent
            else HomeController.show()
          )
        )
      }.recover {
        case ex: Exception =>
          Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      if (isEnabled(AdjustPaymentsOnAccount)) {
        {
          for {
            poaMaybe <- claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino))
            otherData <- dataFromSession
          } yield (poaMaybe, otherData)
        } flatMap {
          case (Right(Some(poa)), otherData) =>
            val a: Option[(BigDecimal, SelectYourReason)] = {
              for {
                amount <- otherData.newPoAAmount
                reason <- otherData.poaAdjustmentReason
              } yield (amount, reason)
            }
            a match {
              case Some(x) =>
                calculationService.recalculate(user.nino, poa.taxYear, x._1, x._2) map {
                  case Left(ex: Throwable) => Redirect(controllers.routes.NextUpdatesController.show()) // to be changed
                  case Right(_) => Redirect(controllers.routes.HomeController.show()) // to be changed
                }
              case None =>
                Future.successful(showInternalServerError(isAgent))
            }
          case (Right(None), otherData) => Logger("application").error(s"Failed to create PaymentOnAccount model, otherData: $otherData")
            Future.successful(showInternalServerError(isAgent))
          case (Left(ex), otherData) =>
            Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}. otherData: $otherData")
            Future.failed(ex)
        }
      } else {
        Future.successful(
          Redirect(
            if (isAgent) HomeController.showAgent
            else HomeController.show()
          )
        )
      }
  }

}
