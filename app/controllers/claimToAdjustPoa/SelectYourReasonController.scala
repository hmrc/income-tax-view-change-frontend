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
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes
import forms.adjustPoa.SelectYourReasonFormProvider
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, PaymentOnAccountViewModel, PoAAmendmentData, SelectYourReason}
import models.core.Nino
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.SelectYourReasonView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SelectYourReasonController @Inject()(
                                            val view: SelectYourReasonView,
                                            val formProvider: SelectYourReasonFormProvider,
                                            val sessionService: PaymentOnAccountSessionService,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching  {

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      ifAdjustPoaIsEnabled(isAgent) {
        withSessionAndPoa(isAgent) { (session, poa) =>
          (session.newPoAAmount) match {
            case Some(amount) if amount >= poa.totalAmount =>
              saveValueAndRedirect(isChange, isAgent, Increase)
            case _ =>
              val form = formProvider.apply()
              EitherT.rightT(Ok(view(
                selectYourReasonForm = session.poaAdjustmentReason.fold(form)(form.fill),
                taxYear = poa.taxYear,
                isAgent = isAgent,
                isChange = isChange,
                useFallbackLink = true)))
          }
        } fold(
          logAndShowErrorPage(isAgent),
          view => view
        )
      }
  }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit request =>
      ifAdjustPoaIsEnabled(isAgent) {
        formProvider.apply()
          .bindFromRequest()
          .fold(
            formWithErrors => withSessionAndPoa(isAgent) { (_, poa) =>
              EitherT.rightT(BadRequest(view(formWithErrors, poa.taxYear, isAgent, isChange, true)))
            },
            value => saveValueAndRedirect(isChange, isAgent, value))
          .fold(
            logAndShowErrorPage(isAgent),
            result => result
          )
      }
  }

  private def withSessionAndPoa (isAgent: Boolean)
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


  private def saveValueAndRedirect(isChange: Boolean, isAgent: Boolean, value: SelectYourReason)
                                  (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {
    for {
      _ <- EitherT(sessionService.setAdjustmentReason(value))
      redirect <- withSessionAndPoa(isAgent) { (session, poa) =>
        (isChange, poa.totalAmountLessThanPoa) match {
          case (false, false) => EitherT.rightT(Redirect(controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent)))
          case (_,_) => EitherT.rightT(Redirect(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent)))
        }
      }
    } yield {
      redirect
    }
  }

  private def ifAdjustPoaIsEnabled(isAgent: Boolean)
                                  (block: Future[Result])
                                  (implicit user: MtdItUser[_]): Future[Result] = {
    if(isEnabled(AdjustPaymentsOnAccount)) {
      block
    } else {
      Future.successful(
        Redirect(
          if (isAgent) routes.HomeController.showAgent
          else         routes.HomeController.show()
        )
      )
    }
  }

  private def logAndShowErrorPage(isAgent: Boolean)(ex: Throwable)(implicit request: Request[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
    errorHandler.showInternalServerError()
  }

}
