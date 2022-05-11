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

package controllers.feedback

import config.{FormPartialProvider, FrontendAppConfig}
import forms.FeedbackForm
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.govukfrontend.views.Implicits.RichInput
import uk.gov.hmrc.govukfrontend.views.viewmodels.input.Input
import uk.gov.hmrc.hmrcfrontend.views.Implicits.RichCharacterCount
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.charactercount.CharacterCount
import uk.gov.hmrc.hmrcfrontend.views.implicits.RichCharacterCountSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.partials._
import views.html.feedback.{Feedback, FeedbackThankYou}

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(implicit val config: FrontendAppConfig,
                                   implicit val ec: ExecutionContext,
                                   val feedbackView: Feedback,
                                   val feedbackThankYouView: FeedbackThankYou,
                                   httpClient: HttpClient,
                                   val sessionCookieCrypto: SessionCookieCrypto,
                                   val formPartialRetriever: FormPartialProvider,
                                   val itvcHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter,
                                   mcc: MessagesControllerComponents
                                  ) extends FrontendController(mcc) with I18nSupport {

  private val TICKET_ID = "ticketId"

  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit().url


  private def feedbackFormPartialUrl(implicit request: Request[AnyContent]) =
    s"${config.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/?submitUrl=${urlEncode(localSubmitUrl)}" +
      s"&service=${urlEncode(config.contactFormServiceIdentifier)}&referer=${urlEncode(contactFormReferer)}"

  private def feedbackHmrcSubmitPartialUrl(implicit request: Request[AnyContent]) =
    s"${config.contactFrontendPartialBaseUrl}/contact/beta-feedback/form?resubmitUrl=${urlEncode(localSubmitUrl)}"

  private def feedbackThankYouPartialUrl(ticketId: String)(implicit request: Request[AnyContent]) =
    s"${config.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def show: Action[AnyContent] = Action {
    implicit request =>
      val feedback = feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submit())
      request.headers.get(REFERER) match {
        case Some(ref) => Ok(feedback).withSession(request.session + (REFERER -> ref))
        case _ => Ok(feedback)
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      FeedbackForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(feedbackView(feedbackForm = hasErrors, postAction = routes.FeedbackController.submit()))),
        data => Future.successful(Redirect(routes.FeedbackController.thankyou()))
      )
//      request.body.asFormUrlEncoded.map { formData =>
//        httpClient.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(
//          rds = readPartialsForm, hc = partialsReadyHeaderCarrier, ec).map {
//          resp =>
//            resp.status match {
//              case HttpStatus.OK => Redirect(routes.FeedbackController.thankyou()).withSession(request.session + (TICKET_ID -> resp.body))
//              case HttpStatus.BAD_REQUEST => BadRequest(feedbackView(FeedbackForm.form, postAction = routes.FeedbackController.submit()))
//              case status => Logger("application").error(s"Unexpected status code from feedback form: $status"); InternalServerError
//            }
//        }
//      }.getOrElse {
//        Logger("application").error("Trying to submit an empty feedback form")
//        Future.successful(InternalServerError)
//      }
  }
  def thankyou: Action[AnyContent] = Action {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedbackThankYouView(referer)).withSession(request.session - REFERER)
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc = itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    itvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc)
  }

  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}
