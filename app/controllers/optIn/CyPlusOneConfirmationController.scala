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

package controllers.optIn

import auth.FrontendAuthorisedFunctions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import jakarta.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.optIn.OptInService
import utils.AuthenticatorPredicate
import views.html.optIn.CyPlusOneConfirmation
import scala.concurrent.{ExecutionContext, Future}

class CyPlusOneConfirmationController @Inject()(val optInService: OptInService,
                                                val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val auth: AuthenticatorPredicate,
                                                val confirmationView: CyPlusOneConfirmation)
                                               (implicit val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {


  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      Future.successful(Ok(confirmationView(isAgent)))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      Future.successful(Ok)
  }
}
