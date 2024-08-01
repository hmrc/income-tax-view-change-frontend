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

package config

import exceptions.{AgentException, IndividualException}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.errorPages.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.Future
import scala.language.implicitConversions

trait ShowInternalServerError {
  def showInternalServerError()(implicit request: Request[_]): Result
}

class ItvcErrorHandler @Inject()(val errorTemplate: ErrorTemplate,
                                 val messagesApi: MessagesApi) extends FrontendErrorHandler with Logging with I18nSupport with ShowInternalServerError {

  private implicit def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")

  private def logError(request: RequestHeader, ex: Throwable): Unit = {
    val prefix = ex match {
      case _ :AgentException => "Agent "
      case _ :IndividualException => "Individual "
      case _ => ""
    }
    logger.error(s"${prefix}Error for (${request.method}) [${request.uri}] -> ${ex.getCause.getClass.getSimpleName}: ${ex.getMessage}")
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logError(request, exception)

    val isAgent = exception match {
      case e :AgentException => true
      case _ => false
    }

    Future.successful(InternalServerError(errorTemplate(
      messagesApi.preferred(request)("standardError.heading"),
      messagesApi.preferred(request)("standardError.heading"),
      messagesApi.preferred(request)("standardError.message"),
      isAgent = isAgent)(rhToRequest(request), messagesApi.preferred(request))))
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit r: Request[_]): Html =
    errorTemplate(pageTitle, heading, message, isAgent = false)

  def showInternalServerError()(implicit request: Request[_]): Result = InternalServerError(standardErrorTemplate(
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.heading"),
    messagesApi.preferred(request)("standardError.message")
  ))
}
