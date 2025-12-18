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
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, WhatYouNeedToKnowViewModel}
import models.core.NormalMode
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ErrorRecovery
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.WhatYouNeedToKnowView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToKnowController @Inject()(val authActions: AuthActions,
                                            val view: WhatYouNeedToKnowView,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val poaSessionService: PaymentOnAccountSessionService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            val individualErrorHandler: ItvcErrorHandler,
                                            val agentErrorHandler: AgentItvcErrorHandler,
                                            val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with WithSessionAndPoa with ErrorRecovery {

  def getRedirect(poa: PaymentOnAccountViewModel)(implicit user: MtdItUser[_]): String = {
    (if (poa.totalAmountLessThanPoa) {
      controllers.claimToAdjustPoa.routes.EnterPoaAmountController.show(user.isAgent(), NormalMode)
    } else {
      controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(user.isAgent(), NormalMode)
    }).url
  }

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
    implicit user =>
      withSessionDataAndPoa() { (_, poa) =>
        val viewModel = WhatYouNeedToKnowViewModel(poa.taxYear, poa.partiallyPaidAndTotalAmountLessThanPoa, getRedirect(poa))
        EitherT.rightT(Ok(view(user.isAgent(), viewModel)))
      } recover logAndRedirect
  }

}
