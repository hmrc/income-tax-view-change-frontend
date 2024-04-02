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

import auth.HeaderExtractor
import config.FrontendAppConfig
import forms.FeedbackForm
import play.api.Logger
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.mvc.Request
import uk.gov.hmrc.hmrcfrontend.views.Utils.urlEncode
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeedbackConnector @Inject()(val http: HttpClient,
                                  val config: FrontendAppConfig,
                                  val itvcHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
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
    println(s"Actual: $hc")
    val x = itvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc)
    println(s"Actual2: $x")
    x
  }

  def submit(formData: FeedbackForm)
            (implicit request: Request[_]): Future[Either[Int, Unit]] = {
    val ref: String = request.headers.get(REFERER).getOrElse("N/A")

    implicit val hc: HeaderCarrier = partialsReadyHeaderCarrier
    val data = formData.toFormMap(ref)
    println(s"Client: $hc ${http} $data")
    http.POSTForm[HttpResponse](feedbackServiceSubmitUrl, data).map {
      resp =>
        println("Boom...")
        resp.status match {
          case OK =>
            Logger("application").info(s"[FeedbackConnector][submit] - RESPONSE status: ${resp.status}")
            Right(())
          case status =>
            Logger("application").error(s"[FeedbackConnector][submit] - RESPONSE status: ${resp.status}")
            Left(status)
        }
    }
  }
}
