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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, WhatYouNeedToKnowViewModel}
import models.core.NormalMode
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ErrorRecovery
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToKnowController @Inject()(val authActions: AuthActions,
                                            val view: WhatYouNeedToKnow,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val poaSessionService: PaymentOnAccountSessionService,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            override implicit val controllerComponents: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with ClaimToAdjustUtils with WithSessionAndPoa with ErrorRecovery {

  def getRedirect(isAgent: Boolean, poa: PaymentOnAccountViewModel): String = {
    (if (poa.totalAmountLessThanPoa) {
      controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent, NormalMode)
    } else {
      controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode)
    }).url
  }

  def show(isAgent: Boolean): Action[AnyContent] = authActions.individualOrAgentWithClient async {
    implicit user =>
      withSessionDataAndPoa() { (_, poa) =>
        val viewModel = WhatYouNeedToKnowViewModel(poa.taxYear, poa.partiallyPaidAndTotalAmountLessThanPoa, getRedirect(isAgent, poa))
        EitherT.rightT(Ok(view(isAgent, viewModel)))
      } recover {
        case ex: Exception =>
          logAndRedirect(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
      }
  }

}
