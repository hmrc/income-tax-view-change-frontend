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

import models.nrs.NrsSubmissionFailure.NrsErrorResponse
import models.nrs.NrsSuccessResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, TOO_MANY_REQUESTS}
import play.api.libs.json.Json
import testConstants.NrsUtils
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future

class NrsConnectorSpec extends BaseConnectorSpec {

  private val CLIENT_CLOSED_REQUEST = 499

  val successResponse: HttpResponse = HttpResponse(
    status = ACCEPTED,
    json = Json.toJson(NrsSuccessResponse("submissionId")),
    headers = Map.empty
  )

  val internalServerErrorResponse: HttpResponse = HttpResponse(
    status = INTERNAL_SERVER_ERROR,
    body = "",
    headers = Map.empty
  )

  val tooManyRequestsErrorResponse: HttpResponse = HttpResponse(
    status = TOO_MANY_REQUESTS,
    body = "",
    headers = Map.empty
  )

  val clientClosedErrorResponse: HttpResponse = HttpResponse(
    status = CLIENT_CLOSED_REQUEST,
    body = "",
    headers = Map.empty
  )

  val badRequestErrorResponse: HttpResponse = HttpResponse(
    status = BAD_REQUEST,
    body = "",
    headers = Map.empty
  )

  object TestNrsConnector extends NrsConnector(mockHttpClientV2, appConfig)

  "Calling .submit()" should {

    "return a NrsSuccessResponse" when {

      s"a $ACCEPTED response is received with valid json" in {

        val nonZeroRemainingAttempts = 10

        val expected = Right(NrsSuccessResponse("submissionId"))

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any())).thenReturn(Future.successful(successResponse))

        val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, nonZeroRemainingAttempts).futureValue

        result shouldBe expected
      }
    }

    "return a NrsErrorResponse" when {

      "the number of retries is zero" in {

        val zeroRemainingAttempts = 0

        val expected = Left(NrsErrorResponse(INTERNAL_SERVER_ERROR))

        when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any())).thenReturn(Future.successful(internalServerErrorResponse))

        val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, zeroRemainingAttempts).futureValue

        result shouldBe expected
      }
    }

    s"return a NrsErrorResponse after 3 retries following 3 $INTERNAL_SERVER_ERROR responses" in {

      val numberOfRetries = 3

      val expectedAttempts = numberOfRetries + 1

      val expected = Left(NrsErrorResponse(INTERNAL_SERVER_ERROR))

      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(
          Future.successful(internalServerErrorResponse),
          Future.successful(internalServerErrorResponse),
          Future.successful(internalServerErrorResponse)
        )

      val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, numberOfRetries).futureValue

      result shouldBe expected

      verify(mockRequestBuilder, times(expectedAttempts)).execute(any[HttpReads[HttpResponse]], any())
    }

    s"execute a retry if response received is $TOO_MANY_REQUESTS" in {

      val numberOfRetries = 1

      val expectedAttempts = numberOfRetries + 1

      val expected = Left(NrsErrorResponse(TOO_MANY_REQUESTS))

      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(
          Future.successful(tooManyRequestsErrorResponse)
        )

      val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, numberOfRetries).futureValue

      result shouldBe expected

      verify(mockRequestBuilder, times(expectedAttempts)).execute(any[HttpReads[HttpResponse]], any())
    }

    s"execute a retry if response received is $CLIENT_CLOSED_REQUEST" in {

      val numberOfRetries = 1

      val expectedAttempts = numberOfRetries + 1

      val expected = Left(NrsErrorResponse(CLIENT_CLOSED_REQUEST))

      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(
          Future.successful(clientClosedErrorResponse)
        )

      val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, numberOfRetries).futureValue

      result shouldBe expected

      verify(mockRequestBuilder, times(expectedAttempts)).execute(any[HttpReads[HttpResponse]], any())
    }

    s"execute a retry if response received is 5xx" in {

      val numberOfRetries = 1

      val expectedAttempts = numberOfRetries + 1

      val expected = Left(NrsErrorResponse(INTERNAL_SERVER_ERROR))

      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(
          Future.successful(internalServerErrorResponse)
        )

      val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, numberOfRetries).futureValue

      result shouldBe expected

      verify(mockRequestBuilder, times(expectedAttempts)).execute(any[HttpReads[HttpResponse]], any())
    }

    s"not execute a retry if response received is 4xx" in {

      val numberOfRetries = 1

      val expectedAttempts = 1

      val expected = Left(NrsErrorResponse(BAD_REQUEST))

      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(
          Future.successful(badRequestErrorResponse)
        )

      val result = TestNrsConnector.submit(NrsUtils.nrsSubmission, numberOfRetries).futureValue

      result shouldBe expected

      verify(mockRequestBuilder, times(expectedAttempts)).execute(any[HttpReads[HttpResponse]], any())
    }
  }
}
