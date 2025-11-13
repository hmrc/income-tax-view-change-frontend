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

import audit.AuditingService
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future

class PayApiConnectorSpec extends BaseConnectorSpec {

  val successResponse = HttpResponse(status = Status.CREATED,
    json = Json.toJson(PaymentJourneyModel("journeyId", "http://www.redirect-url.com")), headers = Map.empty)
  val successResponseBadJson = HttpResponse(status = Status.CREATED, json = Json.parse("{}"), headers = Map.empty)
  val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")
  lazy val mockAuditingService: AuditingService = mock(classOf[AuditingService])

  object TestPayApiConnector extends PayApiConnector(mockHttpClientV2, appConfig)

  "Calling .startPaymentJourney as an individual user" should {

    "return a PaymentJourneyModel" when {

      "a 201 response is received with valid json" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = false).futureValue
        result shouldBe PaymentJourneyModel("journeyId", "http://www.redirect-url.com")
      }
    }

    "return a PaymentJourneyErrorResponse" when {

      "a non 200 response is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = false).futureValue
        result shouldBe PaymentJourneyErrorResponse(400, "Error Message")
      }

      "a 201 response with invalid json is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = false).futureValue
        result shouldBe PaymentJourneyErrorResponse(201, "Invalid Json")
      }
    }
  }

  "Calling .startPaymentJourney as an Agent user" should {

    "return a PaymentJourneyModel" when {

      "a 201 response is received with valid json" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = true).futureValue
        result shouldBe PaymentJourneyModel("journeyId", "http://www.redirect-url.com")
      }
    }

    "return a PaymentJourneyErrorResponse" when {

      "a non 200 response is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = true).futureValue
        result shouldBe PaymentJourneyErrorResponse(400, "Error Message")
      }

      "a 201 response with invalid json is received" in {
        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000, isAgent = true).futureValue
        result shouldBe PaymentJourneyErrorResponse(201, "Invalid Json")
      }
    }
  }
}
