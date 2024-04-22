/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.AmendPaymentOnAccount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendPOAController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                   val auth: AuthenticatorPredicate,
                                   view: AmendPaymentOnAccount,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                  (implicit val appConfig: FrontendAppConfig,
                                   implicit override val mcc: MessagesControllerComponents,
                                   val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        Future.successful(Ok(view(isAgent)))
    }

  def submit(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        Future.successful(Ok(view(isAgent)))
    }
}
