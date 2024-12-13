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

import audit.AuditingService
import auth.authV2.AuthActions
import cats.data.EitherT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.claimToAdjustPoa.ConfirmationForAdjustingPoaViewModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.claimToAdjustPoa.{ClaimToAdjustPoaCalculationService, RecalculatePoaHelper}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationForAdjustingPoaController @Inject()(val authActions: AuthActions,
                                                      val claimToAdjustService: ClaimToAdjustService,
                                                      val poaSessionService: PaymentOnAccountSessionService,
                                                      val ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                                      val view: ConfirmationForAdjustingPoa,
                                                      val auditingService: AuditingService)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      val individualErrorHandler: ItvcErrorHandler,
                                                      val agentErrorHandler: AgentItvcErrorHandler,
                                                      val mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with RecalculatePoaHelper with WithSessionAndPoa {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
    implicit user =>
      withSessionDataAndPoa() { (sessionData, poa) =>
        sessionData.newPoaAmount match {
          case Some(value) =>
            val isAmountZero: Boolean = value.equals(BigDecimal(0))
            val viewModel = ConfirmationForAdjustingPoaViewModel(poa.taxYear, isAmountZero)
            EitherT.rightT(Ok(view(user.isAgent(), viewModel)))
          case None =>

            EitherT.rightT(logAndRedirect(s"Error, New PoA Amount was not found in session"))
        }
      } recover logAndRedirect
  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
    implicit user =>
      handleSubmitPoaData(
        claimToAdjustService = claimToAdjustService,
        ctaCalculationService = ctaCalculationService,
        poaSessionService = poaSessionService,
        auditingService = auditingService
      ) recover logAndRedirect
  }

}
