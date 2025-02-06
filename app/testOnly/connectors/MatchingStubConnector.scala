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

package testOnly.connectors

import connectors.RawResponseReads
import play.api.libs.json.{JsValue, Json}
import testOnly.TestOnlyAppConfig
import testOnly.models.StubClientDetailsModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingStubConnector @Inject() (
    val appConfig: TestOnlyAppConfig,
    val http:      HttpClientV2
  )(
    implicit ec: ExecutionContext)
    extends RawResponseReads {

  def stubClient(clientDetails: StubClientDetailsModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.matchingStubUrl}/dynamic-cid"
    val json: JsValue = Json.obj(
      "data" -> Json.obj(
        "nino" -> Json.obj(
          "value" -> clientDetails.nino
        ),
        "sautr" -> Json.obj(
          "value" -> clientDetails.utr
        ),
        "firstName" -> Json.obj(
          "value" -> "Test"
        ),
        "lastName" -> Json.obj(
          "value" -> "User"
        ),
        "dob" -> Json.obj(
          "value" -> "01011980"
        )
      ),
      "testId"     -> "ITVC",
      "name"       -> "CID",
      "service"    -> "find",
      "resultCode" -> clientDetails.status,
      "timeToLive" -> 43200000
    )
    http
      .post(url"$url")
      .withBody(json)
      .execute[HttpResponse]
  }

}
