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

package controllers.adjustPoa

import auth.MtdItUser
import cats.data.EitherT
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.adjustPoa.SelectYourReasonFormProvider
import models.core.Nino
import models.paymentOnAccount.{PaymentOnAccount, PoAAmendmentData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import viewmodels.adjustPoa.checkAnswers.SelectYourReason
import views.html.adjustPoa.SelectYourReasonView

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

      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

      withSessionAndPoa(isAgent) { (session, poa) =>

        // TODO: if amount has been entered, and is > current amount, autofill reason 005 and redirect

        val form = formProvider.apply()
        Ok(view(
          selectYourReasonForm = session.poaAdjustmentReason.fold(form)(form.fill),
          taxYear = poa.taxYear,
          isAgent = isAgent,
          isChange = isChange,
          backUrl = "TODO",
          useFallbackLink = true))
      } fold (
        ex => {
          Logger("application").error(s"[SelectYourReasonController][show]: ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
        },
        view => view
      )
    }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>

      formProvider.apply()
        .bindFromRequest()
        .fold(
          formWithErrors => withSessionAndPoa(isAgent) { (_, poa) =>
            BadRequest(view(formWithErrors, poa.taxYear, isAgent, isChange, "TODO", true))
          },
          value => saveValueAndRedirect(isChange, isAgent, value))
        .fold(
          ex => {
            val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
            Logger("application").error(s"[SelectYourReasonController][handleRequest] ${ex.getMessage} - ${ex.getCause}")
            errorHandler.showInternalServerError()
          },
          result => result
      )
  }

  private def withSessionAndPoa (isAgent: Boolean)
                                (block: (PoAAmendmentData,PaymentOnAccount) => Result)
                                (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {

    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    for {
      session <- EitherT(sessionService.getMongo)
      poa <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
    } yield {
      (session, poa) match {
        case (Some(s), Some(p)) =>
          block(s, p)
        case (None, _) =>
          Logger("application").error(s"[SelectYourReasonController][withSessionAndPoa] session missing")
          errorHandler.showInternalServerError()
        case (_, None) =>
          Logger("application").error(s"[SelectYourReasonController][withSessionAndPoa] POA missing")
          errorHandler.showInternalServerError()
      }
    }
  }

  private def saveValueAndRedirect(isChange: Boolean, isAgent: Boolean, value: SelectYourReason)
                                  (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {
    for {
      _ <- EitherT(sessionService.setAdjustmentReason(value))
      redirect <- withSessionAndPoa(isAgent) { (session, _) =>
        (isChange, session.newPoAAmount) match {
          // TODO: direct to CYA
          case (false, Some(_)) => Redirect(controllers.routes.HomeController.show())
          // TODO: direct to amount page
          case (false, None) => Redirect(controllers.routes.HomeController.show())
          // TODO: direct to CYA
          case (true, _) => Redirect(controllers.routes.HomeController.show())
        }
      }
    } yield {
      redirect
    }
  }
}
