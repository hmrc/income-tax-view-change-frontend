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
import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes
import controllers.routes.HomeController
import forms.adjustPoa.EnterPoaAmountForm
import models.core.Nino
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ClaimToAdjustService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.EnterPoAAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterPoAAmountController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                         val auth: AuthenticatorPredicate,
                                         view: EnterPoAAmountView,
                                         claimToAdjustService: ClaimToAdjustService,
                                         implicit val itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                        (implicit val appConfig: FrontendAppConfig,
                                         implicit override val mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        if (isEnabled(AdjustPaymentsOnAccount)) {
          claimToAdjustService.getEnterPoAAmountViewModel(Nino(user.nino)).map {
            case Right(viewModel) =>
              Ok(view(EnterPoaAmountForm.form, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoAAmountController.submit(isAgent)))
            case Left(ex) =>
              Logger("application").error(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
              showInternalServerError(isAgent)
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
    implicit request =>
      handleSubmitRequest(isAgent)
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] =
      claimToAdjustService.getEnterPoAAmountViewModel(Nino(user.nino)).map {
        case Right(viewModel) =>
          EnterPoaAmountForm.checkValueConstraints(EnterPoaAmountForm.form.bindFromRequest(), viewModel.adjustedAmountOne, viewModel.initialAmountOne).fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoAAmountController.submit(isAgent))),
            validForm =>
                Redirect(
                  if (isAgent) routes.HomeController.showAgent
                  else         routes.HomeController.show()
                )
          )        case Left(ex) =>
        Logger("application").error(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
        showInternalServerError(isAgent)
      }
      //TODO: Redirect logic, see SelectYourReasonController


}
