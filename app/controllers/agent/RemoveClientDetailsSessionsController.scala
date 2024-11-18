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

package controllers.agent

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.sessionUtils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthenticatorPredicate
import auth.authV2.AuthActions
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveClientDetailsSessionsController @Inject()(val auth: AuthenticatorPredicate,
                                                      val authActions: AuthActions)
                                                     (implicit mcc: MessagesControllerComponents,
                                                      val appConfig: FrontendAppConfig,
                                                      val itvcErrorHandler: AgentItvcErrorHandler,
                                                      val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {


  def show: Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient { implicit user =>
    Redirect(controllers.agent.routes.EnterClientsUTRController.show.url)
      .removingFromSession(
        SessionKeys.clientFirstName,
        SessionKeys.clientLastName,
        SessionKeys.clientMTDID,
        SessionKeys.clientUTR,
        SessionKeys.clientNino,
        SessionKeys.isSupportingAgent,
        SessionKeys.confirmedClient
      )

  }
}
