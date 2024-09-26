/*
 * Copyright 2023 HM Revenue & Customs
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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.claimToAdjustPoa.routes._
import forms.adjustPoa.SelectYourReasonFormProvider
import models.claimToAdjustPoa.{Increase, PaymentOnAccountViewModel, SelectYourReason}
import models.core.{Mode, NormalMode}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.SelectYourReasonView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectYourReasonController @Inject()(
                                            val view: SelectYourReasonView,
                                            val formProvider: SelectYourReasonFormProvider,
                                            val poaSessionService: PaymentOnAccountSessionService,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            auth: AuthenticatorPredicate)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with ClaimToAdjustUtils with WithSessionAndPoa {

  def show(isAgent: Boolean, mode: Mode): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      withSessionDataAndPoa() { (session, poa) =>
        session.newPoAAmount match {
          case Some(amount) if amount >= poa.totalAmount =>
            saveValueAndRedirect(mode, isAgent, Increase, poa)
          case _ =>
            val form = formProvider.apply()
            EitherT.rightT(Ok(view(
              selectYourReasonForm = session.poaAdjustmentReason.fold(form)(form.fill),
              taxYear = poa.taxYear,
              isAgent = isAgent,
              mode = mode,
              useFallbackLink = true)))
        }
      }
  }

  def submit(isAgent: Boolean, mode: Mode): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit request =>
      withSessionDataAndPoa() { (_, poa) =>
        formProvider.apply()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              EitherT.rightT(BadRequest(view(formWithErrors, poa.taxYear, isAgent, mode, true)))
            ,
            value => saveValueAndRedirect(mode, isAgent, value, poa)
          )
      }
  }


  private def saveValueAndRedirect(mode: Mode, isAgent: Boolean, value: SelectYourReason, poa: PaymentOnAccountViewModel)
                                  (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {
    for {
      res <- EitherT(poaSessionService.setAdjustmentReason(value))
    } yield {
      res match {
        case _ => (mode, poa.totalAmountLessThanPoa) match {
          case (NormalMode, false) if value != Increase => Redirect(EnterPoAAmountController.show(isAgent, NormalMode))
          case (_, _) => Redirect(CheckYourAnswersController.show(isAgent))
        }
      }
    }
  }

}
