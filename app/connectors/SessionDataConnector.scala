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
import models.sessionData.SessionDataGetResponse.SessionGetResponse
import models.sessionData.SessionDataModel
import models.sessionData.SessionDataPostResponse.SessionDataPostResponse
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataConnector @Inject()(val appConfig: FrontendAppConfig,
                                     val http: HttpClientV2
                                    )(implicit ec: ExecutionContext) extends RawResponseReads {

  def getSessionData()(implicit hc: HeaderCarrier): Future[SessionGetResponse] = {
    lazy val url = s"${appConfig.incomeTaxSessionDataUrl}/income-tax-session-data/"

    http
      .get(url"$url")
      .execute[SessionGetResponse]

  }

  def postSessionData(sessionDataModel: SessionDataModel)(implicit hc: HeaderCarrier): Future[SessionDataPostResponse] = {
    lazy val url = s"${appConfig.incomeTaxSessionDataUrl}/income-tax-session-data/"

    val body = Json.toJson[SessionDataModel](sessionDataModel)

    http
      .post(url"$url")
      .withBody(body)
      .execute[SessionDataPostResponse]

  }
}
