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

package config

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.errorPages.templates.ErrorTemplate

import javax.inject.Inject

class AgentItvcErrorHandler @Inject()(val errorTemplate: ErrorTemplate,
                                      val messagesApi: MessagesApi) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit r: Request[_]): Html =
    errorTemplate(pageTitle, heading, message, isAgent = true)

  def showInternalServerError()(implicit request: Request[_]): Result = InternalServerError(standardErrorTemplate(
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.message")
  ))

  def showOkTechnicalDifficulties()(implicit request: Request[_]): Result = Ok(standardErrorTemplate(
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.message")
  ))

}
