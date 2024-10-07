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

import auth.authV2.AuthActions
import cats.data.EitherT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.CannotGoBackPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.claimToAdjustPoa.RecalculatePoaHelper
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.YouCannotGoBackView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class YouCannotGoBackController @Inject()(val authActions: AuthActions,
                                          val claimToAdjustService: ClaimToAdjustService,
                                          val poaSessionService: PaymentOnAccountSessionService,
                                          val view: YouCannotGoBackView)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val individualErrorHandler: ItvcErrorHandler,
                                          implicit val agentErrorHandler: AgentItvcErrorHandler,
                                          override implicit val controllerComponents: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with FeatureSwitching with RecalculatePoaHelper with WithSessionAndPoa {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.individualOrAgentWithClient async {
    implicit user =>
      withSessionDataAndPoa(journeyState = CannotGoBackPage) {(_, poa) =>
        EitherT.rightT(Ok(view(
          isAgent = user.isAgent(),
          poaTaxYear = poa.taxYear
        )))
      } recover {
        case ex: Exception =>
          logAndRedirect(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
      }
  }
}
