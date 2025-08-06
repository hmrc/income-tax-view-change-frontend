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

import _root_.helpers.ComponentSpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import models.nrs.NrsSubmissionFailure.{NrsErrorResponse, NrsExceptionThrown}
import models.nrs.NrsSuccessResponse
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.MimeTypes
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import testConstants.NrsUtils
import uk.gov.hmrc.http.HeaderCarrier

class NrsConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  val url: String = "/nrs-orchestrator/submission"

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val connector: NrsConnector = app.injector.instanceOf[NrsConnector]

  "NrsConnectorSpec" when {
    ".submit()" when {
      "ACCEPTED - 202" should {
        "return a successful response when provided valid headers and body" in {

          val requestBody = Json.toJson(NrsUtils.nrsSubmission)
          val expectedResponse = Right(NrsSuccessResponse("submissionId"))
          lazy val result = connector.submit(NrsUtils.nrsSubmission)

          stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo("dummy-api-key"))
              .withRequestBody(equalToJson(requestBody.toString(), true, false))
              .willReturn(aResponse()
                .withBody(NrsUtils.successResponseJson)
                .withStatus(ACCEPTED)
              )
          )

          result.futureValue shouldBe expectedResponse
        }

        "return NrsExceptionThrown when unparsable JSON returned" in {

          val requestBody = """{ "badKey": "badValue" }"""
          val expectedResponse = Left(NrsExceptionThrown)
          lazy val result = connector.submit(NrsUtils.nrsSubmission)

          stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo("dummy-api-key"))
              .willReturn(aResponse()
                .withBody(requestBody)
                .withStatus(ACCEPTED)
              )
          )

          result.futureValue shouldBe expectedResponse
        }

        "return NrsExceptionThrown when provided no body" in {

          val expectedResponse = Left(NrsExceptionThrown)
          lazy val result = connector.submit(NrsUtils.nrsSubmission)

          stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo("dummy-api-key"))
              .willReturn(aResponse()
                .withStatus(ACCEPTED)
              )
          )

          result.futureValue shouldBe expectedResponse
        }
      }

      "4xx response" should {
        "return a NrsErrorResponse" in {

          val requestBody = NrsUtils.successResponseJson
          val expectedResponse = Left(NrsErrorResponse(BAD_REQUEST))
          lazy val result = connector.submit(NrsUtils.nrsSubmission)

          stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo("dummy-api-key"))
              .willReturn(aResponse()
                .withBody(requestBody)
                .withStatus(BAD_REQUEST)
              )
          )

          result.futureValue shouldBe expectedResponse
        }
      }
    }
  }
}
