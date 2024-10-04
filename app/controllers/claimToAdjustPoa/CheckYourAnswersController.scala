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
import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.claimToAdjustPoa.routes._
import models.claimToAdjustPoa.{PoAAmendmentData, SelectYourReason}
import models.core.CheckMode
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.claimToAdjustPoa.{ClaimToAdjustPoaCalculationService, RecalculatePoaHelper}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import utils.{AuthenticatorPredicate, ErrorRecovery}
import views.html.claimToAdjustPoa.CheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authActions: AuthActions,
                                           val claimToAdjustService: ClaimToAdjustService,
                                           val poaSessionService: PaymentOnAccountSessionService,
                                           val ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                           val auth: AuthenticatorPredicate,
                                           val checkYourAnswers: CheckYourAnswers,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit override val controllerComponents: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends FrontendBaseController with ClaimToAdjustUtils with RecalculatePoaHelper with I18nSupport with WithSessionAndPoa with ErrorRecovery{

  def show(isAgent: Boolean): Action[AnyContent] =
    authActions.individualOrAgentWithClient async {
      implicit user =>
        withSessionDataAndPoa() { (session, poa) =>
          withValidSession(isAgent, session) { (reason, amount) =>
            EitherT.rightT(
              Ok(
                checkYourAnswers(
                  isAgent = isAgent,
                  poaReason = reason,
                  taxYear = poa.taxYear,
                  adjustedFirstPoaAmount = amount,
                  adjustedSecondPoaAmount = amount,
                  redirectUrl = ConfirmationForAdjustingPoaController.show(isAgent).url,
                  changePoaAmountUrl = EnterPoAAmountController.show(isAgent, CheckMode).url,
                  changePoaReasonUrl = SelectYourReasonController.show(isAgent, CheckMode).url
                )
              )
            )
          }
        } recover logAndRedirect
    }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit request =>
      handleSubmitPoaData(
        claimToAdjustService = claimToAdjustService,
        ctaCalculationService = ctaCalculationService,
        poaSessionService = poaSessionService,
        isAgent = isAgent
      ) recover logAndRedirect
  }

  private def withValidSession(isAgent: Boolean, session: PoAAmendmentData)
                              (block: (SelectYourReason, BigDecimal) => EitherT[Future, Throwable, Result])
                              (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {

    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    (session.poaAdjustmentReason, session.newPoAAmount) match {
      case (Some(reason), Some(amount)) =>
        block(reason, amount)
      case (None, _) =>
        Logger("application").error(s"Payment On Account Adjustment reason missing from session")
        EitherT.rightT(errorHandler.showInternalServerError())
      case (_, None) =>
        Logger("application").error(s"New Payment on Account missing from session")
        EitherT.rightT(errorHandler.showInternalServerError())
    }
  }

}
