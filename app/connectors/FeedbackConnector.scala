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
import uk.gov.hmrc.hmrcfrontend.views.Utils.urlEncode
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import play.api.libs.ws.DefaultBodyWritables.writeableOf_urlEncodedForm
import java.net.{URI, URL}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeedbackConnector @Inject()(val http: HttpClientV2,
                                  val config: FrontendAppConfig,
                                  val itvcHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
                                 )(implicit val ec: ExecutionContext) extends RawResponseReads with HeaderNames {

  val feedbackServiceSubmitUrl: URL =
    new URI(s"${config.contactFrontendBaseUrl}/contact/beta-feedback/submit?service=${urlEncode(config.contactFormServiceIdentifier)}").toURL

  implicit val readForm: HttpReads[HttpResponse] = (method: String, url: String, response: HttpResponse) => response

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc = itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    itvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc)
  }

  def submit(formData: FeedbackForm)
            (implicit request: Request[_]): Future[Either[Int, Unit]] = {
    val ref: String = request.headers.get(REFERER).getOrElse("N/A")
    val data = formData.toFormMap(ref)
    http.post(feedbackServiceSubmitUrl)(partialsReadyHeaderCarrier)
      .withBody(data)
      .execute[HttpResponse]
      .map {resp =>
          resp.status match {
            case OK =>
              Logger("application").info(s"RESPONSE status: ${resp.status}")
              Right(())
            case status =>
              Logger("application").error(s"RESPONSE status: ${resp.status}")
              Left(status)
          }
    }
  }
}
