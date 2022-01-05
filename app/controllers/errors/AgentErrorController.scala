/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.errors

import com.google.inject.{Inject, Singleton}
import controllers.BaseController
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html.errorPages.AgentError

import scala.concurrent.ExecutionContext

@Singleton
class AgentErrorController @Inject()(checkSessionTimeout: SessionTimeoutPredicate,
                                     authenticate: AuthenticationPredicate,
                                     agentErrorView: AgentError)
                                    (implicit override val executionContext: ExecutionContext,
                                     mcc: MessagesControllerComponents) extends BaseController with I18nSupport {
  val show: Action[AnyContent] = (checkSessionTimeout andThen authenticate) { implicit request =>
    Ok(agentErrorView())
  }
}
