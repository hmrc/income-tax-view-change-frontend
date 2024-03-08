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

package controllers.manageBusinesses.add

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.SessionTimeoutPredicate
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthenticatorPredicate
import views.html.manageBusinesses.add.AddProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AddPropertyController @Inject()(auth: AuthenticatorPredicate,
                                      val addProperty: AddProperty,
                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                      val checkSessionTimeout: SessionTimeoutPredicate)
                                     (implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(isAgent)
  }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val backUrl = Some(controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url)
    val postAction = controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent)
    Future.successful(Ok(addProperty(isAgent, backUrl, postAction)))
  }
}
