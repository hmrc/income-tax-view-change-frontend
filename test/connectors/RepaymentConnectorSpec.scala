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

import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{ACCEPTED, UNAUTHORIZED}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future

class RepaymentConnectorSpec extends BaseConnectorSpec {

  val nino = "AA010101Q"
  val fullAmount = BigDecimal("303.00")
  val port = 9172
  val host = "http://localhost"

  def expectedNextUrl(amount: BigDecimal): String = s"$host:$port/self-assessment-repayment-frontend/$amount/select-amount"

  val successResponse =
    HttpResponse(
      status = Status.ACCEPTED,
      json = Json.toJson(RepaymentJourneyModel(expectedNextUrl(fullAmount))),
      headers = Map.empty
    )

  val successResponseBadJson =
    HttpResponse(
      status = Status.ACCEPTED,
      json = Json.parse("{}"),
      headers = Map.empty
    )

  val unauthorizedResponse =
    HttpResponse(
      status = Status.UNAUTHORIZED,
      body = "Unauthorized Error Message"
    )

  object TestPayApiConnector extends RepaymentConnector(mockHttpClientV2, appConfig)

  ".startRepayment()" should {

    "return a RepaymentResponse" when {

      "a 202 response is received with valid json" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(successResponse))

        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyModel(expectedNextUrl(fullAmount))
      }
    }

    "return a RepaymentJourneyErrorResponse" when {

      "a non 202 response is received" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(unauthorizedResponse))

        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(UNAUTHORIZED, "Unauthorized Error Message")
      }

      "a 202 response with invalid json is received" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(successResponseBadJson))

        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
      }
    }
  }

  ".view()" should {

    "return a RepaymentResponse" when {

      "a 202 response is received with valid json" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(successResponse))

        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyModel(expectedNextUrl(fullAmount))
      }
    }

    "return a RepaymentJourneyErrorResponse" when {

      "a non 202 response is received" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(unauthorizedResponse))

        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyErrorResponse(UNAUTHORIZED, "Unauthorized Error Message")
      }

      "a 202 response with invalid json is received" in {

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.successful(successResponseBadJson))

        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
      }
    }
  }

}
