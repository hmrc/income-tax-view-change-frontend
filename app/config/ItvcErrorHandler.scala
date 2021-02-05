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

package config

import controllers.Assets.{InternalServerError, Ok}
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import views.html.templates.error_template

import scala.concurrent.ExecutionContext

class ItvcErrorHandler @Inject()(implicit val ec: ExecutionContext,
                                 val messagesApi: MessagesApi,
                                 val config: FrontendAppConfig) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]):
  play.twirl.api.HtmlFormat.Appendable =
    error_template(pageTitle, heading, message)(implicitly, implicitly, config)

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
