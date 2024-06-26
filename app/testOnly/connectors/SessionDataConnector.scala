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

package testOnly.connectors

import config.FrontendAppConfig
import connectors.RawResponseReads
import play.api.libs.json.{JsValue, Json}
import testOnly.models.SessionDataModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataConnector @Inject()(val appConfig: FrontendAppConfig,
                                     val http: HttpClient
                                    )(implicit ec: ExecutionContext) extends RawResponseReads {

  def getSessionData(sessionId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.incomeTaxSessionDataUrl}/income-tax-session-data/$sessionId"

    http.GET[HttpResponse](url)(httpReads, hc, ec)
  }

  def postSessionData(sessionDataModel: SessionDataModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.incomeTaxSessionDataUrl}/income-tax-session-data/"

    val body = Json.toJson[SessionDataModel](sessionDataModel)

    http.POST[JsValue, HttpResponse](url, body)
  }

}
