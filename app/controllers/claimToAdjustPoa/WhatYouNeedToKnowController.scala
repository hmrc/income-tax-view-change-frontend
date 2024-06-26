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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.NormalMode
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, WhatYouNeedToKnowViewModel}
import models.core.{Nino, NormalMode}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToKnowController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            val view: WhatYouNeedToKnow,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            auth: AuthenticatorPredicate,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val poaSessionService: PaymentOnAccountSessionService,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with ClaimToAdjustUtils with WithSessionAndPoa {

  def getRedirect(isAgent: Boolean, poa: PaymentOnAccountViewModel): String = {
    (if (poa.totalAmountLessThanPoa) {
      controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent, NormalMode)
    } else {
      controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode)
    }).url
  }

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withSessionDataAndPoa() { (_, poa) =>
        val viewModel = WhatYouNeedToKnowViewModel(poa.taxYear, poa.partiallyPaidAndTotalAmountLessThanPoa, getRedirect(isAgent, poa))
        EitherT.rightT(Ok(view(isAgent, viewModel)))
      } recover {
        case ex: Exception =>
          Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
      }
  }

}
