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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse._
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, MainIncomeLower}
import models.core.CorrelationId
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.Json

class ClaimToAdjustPoaConnectorSpec extends AnyWordSpec with ComponentSpecBase {

  val request = ClaimToAdjustPoaRequest(
    "AA0000A",
    "2024",
    1000.015,
    MainIncomeLower
  )

  val successRequestBody = """ {
              "nino": "AA0000A",
              "taxYear": "2024",
              "amount": 1000.02,
              "poaAdjustmentReason": "001"
              }"""

  val failureResponseBody = """
          {
             "message": "INVALID_REQUEST"
          }
          """

  val timeout: PatienceConfig = PatienceConfig(5.seconds)
  lazy val connector: ClaimToAdjustPoaConnector = app.injector.instanceOf[ClaimToAdjustPoaConnector]

  "ClaimToAdjustPoaConnector" when {

    "calling postClaimToAdjustPoa" should {

      val processingDate = "2024-01-31T09:27:17Z"

      "use correlationID when provided in header" in {

        val responseBody = s"""
          {
             "processingDate": "$processingDate"
          }
          """

        WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", CREATED, responseBody)

        val correlationId = CorrelationId()

        val updatedHc = hc.copy(otherHeaders = hc.otherHeaders ++ Seq(correlationId.asHeader()))

        val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
          connector.postClaimToAdjustPoa(request)(updatedHc).futureValue

        result shouldBe Right(ClaimToAdjustPoaSuccess(processingDate))

        WiremockHelper.verifyPost(s"/submit-claim-to-adjust-poa",
          Some(Json.stringify(Json.parse(successRequestBody))),
          (CorrelationId.correlationId, correlationId.id.toString))
      }


      "generate correlationID when none provided" in {

        val responseBody = s"""
          {
             "processingDate": "$processingDate"
          }
          """

        WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", CREATED, responseBody)

        val correlationId = CorrelationId()

        val updatedHc = hc.copy(otherHeaders = hc.otherHeaders ++ Seq(correlationId.asHeader()))

        val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
          connector.postClaimToAdjustPoa(request)(updatedHc).futureValue

        result shouldBe Right(ClaimToAdjustPoaSuccess(processingDate))

        verify(postRequestedFor(
          urlEqualTo(s"/submit-claim-to-adjust-poa"))
          .withHeader(CorrelationId.correlationId, matching("[\\d\\w-]+")))
      }


      "return a successful response" in {

        WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", CREATED, s"""
          {
             "processingDate": "$processingDate"
          }
          """)

        val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
          connector.postClaimToAdjustPoa(request).futureValue

        result shouldBe Right(ClaimToAdjustPoaSuccess(processingDate))
      }

      "return an error" when {

        "successful response cannot be parsed" in {

          WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", CREATED,
            s"""
          {
             "invalid": "response"
          }
          """)

          val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
            connector.postClaimToAdjustPoa(request).futureValue

          result shouldBe Left(ClaimToAdjustPoaInvalidJson)
        }

        "failure response cannot be parsed" in {

          WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", BAD_REQUEST, s"""
          {
             "invalid": "response"
          }
          """)

          val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
            connector.postClaimToAdjustPoa(request).futureValue

          result shouldBe Left(ClaimToAdjustPoaInvalidJson)
        }

        "an error response is received" in {

          WiremockHelper.stubPost(s"/submit-claim-to-adjust-poa", BAD_REQUEST, s"""
          {
             "message": "INVALID_REQUEST"
          }
          """)

          val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
            connector.postClaimToAdjustPoa(request).futureValue

          result shouldBe Left(ClaimToAdjustPoaError("INVALID_REQUEST"))
        }

        "a server error is received" in {

          stubFor(post(urlEqualTo(s"/submit-claim-to-adjust-poa"))
            .willReturn(
              serverError()
            ))

          val result: Either[ClaimToAdjustPoaFailure, ClaimToAdjustPoaSuccess] =
            connector.postClaimToAdjustPoa(request).futureValue

          result shouldBe Left(UnexpectedError)
        }
      }
    }
  }
}
