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

package connectors

import config.FrontendAppConfig
import forms.FeedbackForm
import play.api.Logger
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.mvc.Request
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.hmrcfrontend.views.Utils.urlEncode
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeedbackConnector @Inject()(val http: HttpClient,
                                  val config: FrontendAppConfig,
                                  val itvcHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter,
                                 )(implicit val ec: ExecutionContext) extends RawResponseReads with HeaderNames {

  private val feedbackServiceSubmitUrl: String =
    s"${
      config.contactFrontendBaseUrl
    }/contact/beta-feedback/submit?" +
      s"service=${
        urlEncode(config.contactFormServiceIdentifier)
      }"

  implicit val readForm: HttpReads[HttpResponse] = (method: String, url: String, response: HttpResponse) => response

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc = itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    itvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc)
  }

  def submit(formData: FeedbackForm)
            (implicit request: Request[_]): Future[Either[Int, Unit]] = {
    val ref: String = request.headers.get(REFERER).getOrElse("N/A")

    http.POSTForm[HttpResponse](feedbackServiceSubmitUrl,
      formData.toFormMap(ref))(readForm, partialsReadyHeaderCarrier, ec).map {
      resp =>
        resp.status match {
          case OK =>
            Logger("application").info(s"OK....")
            Right(())
          case status =>
            Logger("application").error(s"Unexpected status code from feedback form: $status")
            Left(status)
        }
    }
  }
}
