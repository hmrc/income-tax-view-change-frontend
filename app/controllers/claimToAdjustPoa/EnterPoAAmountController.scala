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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.adjustPoa.EnterPoaAmountForm
import models.claimToAdjustPoa.{Increase, PoAAmendmentData, PoAAmountViewModel}
import models.core.Nino
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, ClaimToAdjustUtils}
import views.html.claimToAdjustPoa.EnterPoAAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterPoAAmountController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                         val auth: AuthenticatorPredicate,
                                         val sessionService: PaymentOnAccountSessionService,
                                         view: EnterPoAAmountView,
                                         claimToAdjustService: ClaimToAdjustService,
                                         implicit val itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                        (implicit val appConfig: FrontendAppConfig,
                                         implicit override val mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with ClaimToAdjustUtils{

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        ifAdjustPoaIsEnabled(isAgent) {
          withvalidSession(isAgent) { _ =>
            claimToAdjustService.getEnterPoAAmountViewModel(Nino(user.nino)).map {
              case Right(viewModel) =>
                Ok(view(EnterPoaAmountForm.form, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoAAmountController.submit(isAgent)))
              case Left(ex) =>
                Logger("application").error(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
                showInternalServerError(isAgent)
            }
          }
        }.recover {
          case ex: Exception =>
            Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
    }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit request =>
      ifAdjustPoaIsEnabled(isAgent) {
        handleSubmitRequest(isAgent)
      }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
      claimToAdjustService.getEnterPoAAmountViewModel(Nino(user.nino)).flatMap {
        case Right(viewModel) =>
          EnterPoaAmountForm.checkValueConstraints(EnterPoaAmountForm.form.bindFromRequest(), viewModel.totalAmountOne, viewModel.relevantAmountOne).fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoAAmountController.submit(isAgent)))),
            validForm =>
              sessionService.setNewPoAAmount(validForm.amount).flatMap {
                case Left(ex) => Logger("application").error(s"Error while setting mongo data : ${ex.getMessage} - ${ex.getCause}")
                  Future.successful(showInternalServerError(isAgent))
                case Right(_) => getRedirect(viewModel, validForm.amount, isAgent)
              }
          )        case Left(ex) =>
        Logger("application").error(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
          Future.successful(showInternalServerError(isAgent))
      }
  }

  def getRedirect(viewModel: PoAAmountViewModel, newPoaAmount: BigDecimal, isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    (viewModel.totalAmountLessThanPoa, newPoaAmount > viewModel.totalAmountOne) match {
      case (true, true) => sessionService.setAdjustmentReason(Increase).map{
        case Left(ex) => Logger("application").error(s"Error while setting adjustment reason to increase : ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
        case Right(_) =>
          Redirect(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent))
      }
      case (true, _) => Future.successful(Redirect(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, isChange = false)))
      case _ => Future.successful(Redirect(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent)))
    }
  }

  def withvalidSession(isAgent:Boolean)(block: (PoAAmendmentData) => Future[Result])(implicit user: MtdItUser[_]) = {
    sessionService.getMongo.flatMap {
      case Right(Some(data)) => block(data)
      case Right(None) => Logger("application").error(s"No mongo data found")
        Future.successful(showInternalServerError(isAgent))
      case Left(ex) => Logger("application").error(s"Error while retrieving mongo data : ${ex.getMessage} - ${ex.getCause}")
        Future.successful(showInternalServerError(isAgent))
    }
  }

}
