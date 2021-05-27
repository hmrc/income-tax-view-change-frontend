/*
 * Copyright 2021 HM Revenue & Customs
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

import config.featureswitch.{AgentViewer, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.BaseAgentController
import controllers.agent.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.UTRError

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UTRErrorController @Inject()(utrError: UTRError,
                                    val authorisedFunctions: AuthorisedFunctions)
                                  (implicit mcc: MessagesControllerComponents,
                                   val appConfig: FrontendAppConfig,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   val ec: ExecutionContext)
  extends BaseAgentController with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = Authenticated.asyncWithoutClientAuth() { implicit request =>
    implicit user =>
    if (isEnabled(AgentViewer)) {
      val clientUTR: Option[String] = request.session.get(SessionKeys.clientUTR)
      clientUTR match {
        case Some(clientUtr) =>
          Future.successful(Ok(utrError(
            clientUtr = clientUtr,
            postAction = controllers.agent.routes.UTRErrorController.submit()
          )))
        case None => Future.successful(Redirect(routes.EnterClientsUTRController.show()))
      }
    } else {
      Future.failed(new NotFoundException("[UTRErrorPageController][show] - Agent viewer is disabled"))
    }
  }

  def submit: Action[AnyContent] = Authenticated.asyncWithoutClientAuth() { implicit request =>
    implicit user =>
    if (isEnabled(AgentViewer)) {
      Future.successful(
        Redirect(routes.EnterClientsUTRController.show().url).removingFromSession(SessionKeys.clientUTR)
      )
    } else {
      Future.failed(new NotFoundException("[UTRErrorPageController][submit] - Agent viewer is disabled"))
    }
  }



}
