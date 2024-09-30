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

package controllers.agent.errors

import com.google.inject.{Inject, Singleton}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.BaseAgentController
import controllers.predicates.agent.AgentAuthenticationPredicate.{defaultAgentPredicates, timeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.agent.errorPages.AgentError

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentErrorController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                     agentErrorView: AgentError)
                                    (implicit mcc: MessagesControllerComponents,
                                     val appConfig: FrontendAppConfig,
                                     val itvcErrorHandler: AgentItvcErrorHandler,
                                     val ec: ExecutionContext) extends BaseAgentController with I18nSupport {

  val show: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(timeoutPredicate) { implicit request =>
    implicit user =>
      Future.successful(Ok(agentErrorView()))
  }
}
