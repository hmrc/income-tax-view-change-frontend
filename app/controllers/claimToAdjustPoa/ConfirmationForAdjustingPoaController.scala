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

import cats.data.EitherT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.claimToAdjustPoa.ConfirmationForAdjustingPoaViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.claimToAdjustPoa.{ClaimToAdjustPoaCalculationService, RecalculatePoaHelper}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationForAdjustingPoaController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                      val claimToAdjustService: ClaimToAdjustService,
                                                      val poaSessionService: PaymentOnAccountSessionService,
                                                      val ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                                      val view: ConfirmationForAdjustingPoa,
                                                      implicit val itvcErrorHandler: ItvcErrorHandler,
                                                      auth: AuthenticatorPredicate,
                                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with RecalculatePoaHelper with WithSessionAndPoa {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withSessionDataAndPoa() { (sessionData, poa) =>
        sessionData.newPoAAmount match {
          case Some(value) =>
            val isAmountZero: Boolean = value.equals(BigDecimal(0))
            val viewModel = ConfirmationForAdjustingPoaViewModel(poa.taxYear, isAmountZero)
            EitherT.rightT(Ok(view(isAgent, viewModel)))
          case None =>
            Logger("application").error(s"Error, New PoA Amount was not found in session")
            EitherT.rightT(showInternalServerError(isAgent))
        }
      } recover {
        case ex: Exception =>
          Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      handleSubmitPoaData(
        claimToAdjustService = claimToAdjustService,
        ctaCalculationService = ctaCalculationService,
        poaSessionService = poaSessionService,
        isAgent = isAgent
      )
  }

}
