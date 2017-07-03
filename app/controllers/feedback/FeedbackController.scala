/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.feedback

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import config.{AppConfig, WSHttp}
import play.api.Logger
import play.api.http.{Status => HttpStatus}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, RequestHeader}
import play.twirl.api.Html
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.partials._
import views.html.feedback.feedback_thankyou

import scala.concurrent.Future

@Singleton
class FeedbackController @Inject()(implicit val applicationConfig: AppConfig,
                                   val wsHttp: WSHttp,
                                   val messagesApi: MessagesApi
                                  ) extends FrontendController with PartialRetriever with I18nSupport {
  override val httpGet = wsHttp
  val httpPost = wsHttp

  private val TICKET_ID = "ticketId"

  implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = new CachedStaticHtmlPartialRetriever {
    override val httpGet: HttpGet = wsHttp
  }


  implicit val formPartialRetriever: FormPartialRetriever = new FormPartialRetriever {
    override def httpGet: HttpGet = wsHttp

    override def crypto: (String) => String = cookie => SessionCookieCryptoFilter.encrypt(cookie)
  }

  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit().url

  protected def loadPartial(url: String)(implicit request: RequestHeader): HtmlPartial = ???


  private def feedbackFormPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/?submitUrl=${urlEncode(localSubmitUrl)}" +
      s"&service=${urlEncode(applicationConfig.contactFormServiceIdentifier)}&referer=${urlEncode(contactFormReferer)}"

  private def feedbackHmrcSubmitPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form?resubmitUrl=${urlEncode(localSubmitUrl)}"

  private def feedbackThankYouPartialUrl(ticketId: String)(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def show: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      (request.session.get(REFERER), request.headers.get(REFERER)) match {
        case (None, Some(ref)) => Ok(views.html.feedback.feedback(feedbackFormPartialUrl, None)).withSession(request.session + (REFERER -> ref))
        case _ => Ok(views.html.feedback.feedback(feedbackFormPartialUrl, None))
      }
  }

  def submit: Action[AnyContent] = UnauthorisedAction.async {
    implicit request =>
      request.body.asFormUrlEncoded.map { formData =>
        httpPost.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(rds = readPartialsForm, hc = partialsReadyHeaderCarrier).map {
          resp =>
            resp.status match {
              case HttpStatus.OK => Redirect(routes.FeedbackController.thankyou()).withSession(request.session + (TICKET_ID -> resp.body))
              case HttpStatus.BAD_REQUEST => BadRequest(views.html.feedback.feedback(feedbackFormPartialUrl, Some(Html(resp.body))))
              case status => Logger.error(s"Unexpected status code from feedback form: $status"); InternalServerError
            }
        }
      }.getOrElse {
        Logger.error("Trying to submit an empty feedback form")
        Future.successful(InternalServerError)
      }
  }

  def thankyou: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedback_thankyou(feedbackThankYouPartialUrl(ticketId), referer)).withSession(request.session - REFERER)
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = PlaHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    PlaHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

  object PlaHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter {
    override val crypto = encryptCookieString _

    def encryptCookieString(cookie: String): String = {
      SessionCookieCryptoFilter.encrypt(cookie)
    }
  }

  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}