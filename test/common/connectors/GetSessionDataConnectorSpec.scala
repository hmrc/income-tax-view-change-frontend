/*
 * Copyright 2025 HM Revenue & Customs
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

package common.connectors

import common.models.sessionData.SessionDataGetResponse.SessionDataGetSuccess
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future


class GetSessionDataConnectorSpec extends BaseConnectorSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
  }

  val connector = new GetSessionDataConnector(appConfig, mockHttpClientV2)

  "getSessionData" should {
    "return SessionDataGetSuccess" when {
      s"a $OK response is received with valid JSON" in {
        val successResponse = SessionDataGetSuccess("mtditid", "nino", "utr", "sessionId")

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.toJson(successResponse), Map.empty)))

        connector.getSessionData().map { result =>
          result shouldBe successResponse
        }
      }
    }
  }
}
