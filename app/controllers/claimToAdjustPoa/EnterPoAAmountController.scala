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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitCurrencyFormatter
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterPoAAmountController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                           val auth: AuthenticatorPredicate,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        Future successful Ok(
          s"to be implemented: /report-quarterly/income-and-expenses/view/${if (isAgent) "agents" else ""}adjust-poa/enter-poa-amount")
    }

}