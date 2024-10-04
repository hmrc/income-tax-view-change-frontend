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
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.claimToAdjustPoa.routes._
import forms.adjustPoa.EnterPoaAmountForm
import models.claimToAdjustPoa.{Increase, PaymentOnAccountViewModel}
import models.core.{CheckMode, Mode, Nino, NormalMode}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.claimToAdjust.{ClaimToAdjustUtils, JourneyCheckerClaimToAdjust}
import utils.{AuthenticatorPredicate, ErrorRecovery}
import views.html.claimToAdjustPoa.EnterPoAAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterPoAAmountController @Inject()(val authActions: AuthActions,
                                         val auth: AuthenticatorPredicate,
                                         val poaSessionService: PaymentOnAccountSessionService,
                                         view: EnterPoAAmountView,
                                         val claimToAdjustService: ClaimToAdjustService,
                                         implicit val itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                        (implicit val appConfig: FrontendAppConfig,
                                         override implicit val controllerComponents: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends FrontendBaseController with FeatureSwitching with ClaimToAdjustUtils with I18nSupport with JourneyCheckerClaimToAdjust with ErrorRecovery {

  def show(isAgent: Boolean, mode: Mode): Action[AnyContent] =
    authActions.individualOrAgentWithClient async {
      implicit user =>
        ifAdjustPoaIsEnabled(isAgent) {
          withSessionData() { session =>
            claimToAdjustService.getPoaViewModelWithAdjustmentReason(Nino(user.nino)).map {
              case Right(viewModel) =>
                val filledForm = session.newPoAAmount.fold(EnterPoaAmountForm.form)(value =>
                  EnterPoaAmountForm.form.fill(EnterPoaAmountForm(value))
                )
                Ok(view(filledForm, viewModel, isAgent, EnterPoAAmountController.submit(isAgent, mode)))
              case Left(ex) =>
                logAndRedirect(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
            }
          }
        } recover {
          case ex: Exception =>
            logAndRedirect(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
        }
    }

  def submit(isAgent: Boolean, mode: Mode): Action[AnyContent] = authActions.individualOrAgentWithClient async {
    implicit request =>
      ifAdjustPoaIsEnabled(isAgent) {
        claimToAdjustService.getPoaViewModelWithAdjustmentReason(Nino(request.nino)).flatMap {
          case Right(viewModel) =>
            handleForm(viewModel, isAgent, mode)
          case Left(ex) =>
            Future.successful(logAndRedirect(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}"))
        }
      }
  }

  def handleForm(viewModel: PaymentOnAccountViewModel, isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    EnterPoaAmountForm.checkValueConstraints(EnterPoaAmountForm.form.bindFromRequest(), viewModel.totalAmountOne, viewModel.relevantAmountOne).fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, viewModel, isAgent, EnterPoAAmountController.submit(isAgent, mode)))),
      validForm =>
        poaSessionService.setNewPoAAmount(validForm.amount).flatMap {
          case Left(ex) =>
            Future.successful(logAndRedirect(s"Error while setting mongo data : ${ex.getMessage} - ${ex.getCause}"))
          case Right(_) => getRedirect(viewModel, validForm.amount, isAgent, mode)
        }
    )
  }

  def getRedirect(viewModel: PaymentOnAccountViewModel, newPoaAmount: BigDecimal, isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    (viewModel.totalAmountLessThanPoa, newPoaAmount > viewModel.totalAmountOne) match {
      case (true, true) => hasIncreased(isAgent)
      case (true, _) => hasDecreased(isAgent, mode)
      case _ => Future.successful(Redirect(CheckYourAnswersController.show(isAgent)))
    }
  }

  private def hasIncreased(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    poaSessionService.setAdjustmentReason(Increase).map {
      case Left(ex) =>
        logAndRedirect(s"Error while setting adjustment reason to increase : ${ex.getMessage} - ${ex.getCause}")
      case Right(_) =>
        Redirect(CheckYourAnswersController.show(isAgent))
    }
  }

  //user has decreased but could have increased:
  private def hasDecreased(isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    if (mode == NormalMode)
      Future.successful(Redirect(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode)))
    else {
      poaSessionService.getMongo.map {
        case Right(Some(mongoData)) => mongoData.poaAdjustmentReason match {
          case Some(reason) if reason != Increase => Redirect(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent))
          case _ => Redirect(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, CheckMode))
        }
        case _ =>
         logAndRedirect(s"No active mongo data found")
      }
    }
  }

}