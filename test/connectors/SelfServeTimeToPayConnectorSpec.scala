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

package connectors

import models.core.{SelfServeTimeToPayJourneyErrorResponse, SelfServeTimeToPayJourneyResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future

class SelfServeTimeToPayConnectorSpec extends BaseConnectorSpec {

  val successResponse = HttpResponse(status = Status.CREATED,
    json = Json.toJson(SelfServeTimeToPayJourneyResponseModel("journeyId", "http://www.redirect-url.com")), headers = Map.empty)
  val successResponseBadJson = HttpResponse(status = Status.CREATED, json = Json.parse("{}"), headers = Map.empty)
  val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

  object TestSelfServeTimeToPayConnector extends SelfServeTimeToPayConnector(mockHttpClientV2, appConfig)

  "Calling .startPaymentJourney as an individual user" should {

    "return a PaymentJourneyModel" when {

      "a 201 response is received with valid json" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))
        val result = TestSelfServeTimeToPayConnector.startSelfServeTimeToPayJourney().futureValue
        result shouldBe SelfServeTimeToPayJourneyResponseModel("journeyId", "http://www.redirect-url.com")
      }
    }

    "return a PaymentJourneyErrorResponse" when {

      "a non 200 response is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result = TestSelfServeTimeToPayConnector.startSelfServeTimeToPayJourney().futureValue
        result shouldBe SelfServeTimeToPayJourneyErrorResponse(400, "Error Message")
      }

      "a 201 response with invalid json is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result = TestSelfServeTimeToPayConnector.startSelfServeTimeToPayJourney().futureValue
        result shouldBe SelfServeTimeToPayJourneyErrorResponse(201, "Invalid Json")
      }
    }
  }
}
