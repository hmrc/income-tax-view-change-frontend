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

import auth.MtdItUser
import cats.data.EitherT
import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoAAmendmentData}
import models.core.Nino
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.CheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                           val auth: AuthenticatorPredicate,
                                           val sessionService: PaymentOnAccountSessionService,
                                           val checkYourAnswers: CheckYourAnswers,
                                           val claimToAdjustService: ClaimToAdjustService,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        ifAdjustPoaIsEnabled(isAgent) {
          withSessionAndPoa(isAgent) { (session, poa) =>
            (session.poaAdjustmentReason, session.newPoAAmount, poa) match {
              case (Some(adjustmentReason), Some(newAmount), viewModel) =>
                EitherT.rightT(Ok(
                  checkYourAnswers(
                    isAgent = isAgent,
                    taxYear = viewModel.taxYear,
                    adjustedFirstPoaAmount = newAmount,
                    adjustedSecondPoaAmount = newAmount,
                    poaReason = adjustmentReason.messagesKey,
                    redirectUrl = controllers.claimToAdjustPoa.routes.ConfirmationController.show(isAgent).url,
                    changePoaReasonUrl = controllers.claimToAdjustPoa.routes.ChangePoaReason.show(isAgent).url,
                    changePoaAmountUrl = controllers.claimToAdjustPoa.routes.ChangePoaAmount.show(isAgent).url
                  )
                ))
              case _ =>
                EitherT.rightT(InternalServerError)
            }
          } fold(
            logAndShowErrorPage(isAgent),
            view => view
          )
        }
    }

  private def ifAdjustPoaIsEnabled(isAgent: Boolean)
                                  (block: Future[Result])
                                  (implicit user: MtdItUser[_]): Future[Result] = {
    if (isEnabled(AdjustPaymentsOnAccount)) {
      block
    } else {
      Future.successful(
        Redirect(
          if (isAgent) controllers.routes.HomeController.showAgent
          else controllers.routes.HomeController.show()
        )
      )
    }
  }

  private def withSessionAndPoa(isAgent: Boolean)
                               (block: (PoAAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                               (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {

    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    for {
      session <- EitherT(sessionService.getMongo)
      poa <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
      result <- (session, poa) match {
        case (Some(s), Some(p)) =>
          block(s, p)
        case (None, _) =>
          Logger("application").error(s"Session missing")
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
          x
        case (_, None) =>
          Logger("application").error(s"POA missing")
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
          x
      }
    } yield result
  }

  private def logAndShowErrorPage(isAgent: Boolean)(ex: Throwable)(implicit request: Request[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
    errorHandler.showInternalServerError()
  }
}
