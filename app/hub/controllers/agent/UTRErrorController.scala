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

package hub.controllers.agent

import common.auth.AuthActions
import common.config.{AgentItvcErrorHandler, FrontendAppConfig}
import common.config.featureswitch.FeatureSwitching
import common.utils.sessionUtils.SessionKeys
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import hub.views.html.agent.errorPages.UTRErrorView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UTRErrorController @Inject()(utrError: UTRErrorView,
                                   val authActions: AuthActions)
                                  (implicit mcc: MessagesControllerComponents,
                                   val appConfig: FrontendAppConfig,
                                   val itvcErrorHandler: AgentItvcErrorHandler,
                                   val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with Logging {

  lazy val postAction =
    if (appConfig.isNewHubUrl)
      routes.EnterClientsUTRController.submitNewUrl()
    else
      hub.controllers.agent.routes.UTRErrorController.submit()


  def show: Action[AnyContent] = authActions.asAgent().async { implicit user =>
    logger.warn("Agent shown the cannot-view-client page")
    Future.successful(Ok(utrError(
      postAction = postAction
    )))
  }

  def submit: Action[AnyContent] = handleSubmit

  def submitNewUrl: Action[AnyContent] = handleSubmit

  def handleSubmit: Action[AnyContent] = authActions.asAgent().async { implicit user =>
    Future.successful(
      Redirect(appConfig.enterClientsUTRUrl).removingFromSession(SessionKeys.clientUTR)
    )
  }

}
