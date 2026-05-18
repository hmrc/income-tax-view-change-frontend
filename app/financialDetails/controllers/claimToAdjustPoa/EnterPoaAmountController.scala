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

package financialDetails.controllers.claimToAdjustPoa

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import financialDetails.forms.adjustPoa.EnterPoaAmountForm
import financialDetails.models.claimToAdjustPoa.Increase
import financialDetails.models.claimToAdjustPoa.viewModels.PaymentOnAccountViewModel
import financialDetails.services.PaymentOnAccountSessionService
import financialDetails.services.claimToAdjustPoa.ClaimToAdjustService
import financialDetails.utils.ErrorRecovery
import financialDetails.utils.claimToAdjust.JourneyCheckerClaimToAdjust
import models.core.{CheckMode, Mode, Nino, NormalMode}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import financialDetails.views.html.claimToAdjustPoa.EnterPoaAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterPoaAmountController @Inject()(val authActions: AuthActions,
                                         val poaSessionService: PaymentOnAccountSessionService,
                                         view: EnterPoaAmountView,
                                         val claimToAdjustService: ClaimToAdjustService)
                                        (implicit val appConfig: FrontendAppConfig,
                                         val individualErrorHandler: ItvcErrorHandler,
                                         val agentErrorHandler: AgentItvcErrorHandler,
                                         val mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with JourneyCheckerClaimToAdjust with ErrorRecovery {

  def show(isAgent: Boolean, mode: Mode): Action[AnyContent] =
    authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
      implicit user =>
        withSessionData() { session =>
          claimToAdjustService.getPoaViewModelWithAdjustmentReason(Nino(user.nino)).map {
            case Right(viewModel) =>
              val filledForm = session.newPoaAmount.fold(EnterPoaAmountForm.form)(value =>
                EnterPoaAmountForm.form.fill(EnterPoaAmountForm(value))
              )
              Ok(view(filledForm, viewModel, user.isAgent, routes.EnterPoaAmountController.submit(user.isAgent, mode)))
            case Left(ex) =>
              logAndRedirect(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}")
          }
        }
    }

  def submit(isAgent: Boolean, mode: Mode): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
    implicit user =>
      claimToAdjustService.getPoaViewModelWithAdjustmentReason(Nino(user.nino)).flatMap {
        case Right(viewModel) =>
          handleForm(viewModel, mode)
        case Left(ex) =>
          Future.successful(logAndRedirect(s"Error while retrieving charge history details : ${ex.getMessage} - ${ex.getCause}"))
      }
  }

  def handleForm(viewModel: PaymentOnAccountViewModel, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    EnterPoaAmountForm.checkValueConstraints(EnterPoaAmountForm.form.bindFromRequest(), viewModel.totalAmountOne, viewModel.relevantAmountOne).fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, viewModel, user.isAgent, routes.EnterPoaAmountController.submit(user.isAgent, mode)))),
      validForm =>
        poaSessionService.setNewPoaAmount(validForm.amount).flatMap {
          case Left(ex) =>
            Future.successful(logAndRedirect(s"Error while setting mongo data : ${ex.getMessage} - ${ex.getCause}"))
          case Right(_) => getRedirect(viewModel, validForm.amount, mode)
        }
    )
  }

  def getRedirect(viewModel: PaymentOnAccountViewModel, newPoaAmount: BigDecimal, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    (viewModel.totalAmountLessThanPoa, newPoaAmount > viewModel.totalAmountOne) match {
      case (true, true) => hasIncreased()
      case (true, _) => hasDecreased(mode)
      case _ => Future.successful(Redirect(routes.CheckYourAnswersController.show(user.isAgent)))
    }
  }

  private def hasIncreased()(implicit user: MtdItUser[_]): Future[Result] = {
    poaSessionService.setAdjustmentReason(Increase).map {
      case Left(ex) =>
        logAndRedirect(s"Error while setting adjustment reason to increase : ${ex.getMessage} - ${ex.getCause}")
      case Right(_) =>
        Redirect(routes.CheckYourAnswersController.show(user.isAgent))
    }
  }

  //user has decreased but could have increased:
  private def hasDecreased(mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    if (mode == NormalMode)
      Future.successful(Redirect(routes.SelectYourReasonController.show(user.isAgent, NormalMode)))
    else {
      poaSessionService.getMongo.map {
        case Right(Some(mongoData)) => mongoData.poaAdjustmentReason match {
          case Some(reason) if reason != Increase => Redirect(routes.CheckYourAnswersController.show(user.isAgent))
          case _ => Redirect(routes.SelectYourReasonController.show(user.isAgent, CheckMode))
        }
        case _ =>
          logAndRedirect(s"No active mongo data found")
      }
    }
  }
}