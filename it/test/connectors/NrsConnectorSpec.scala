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
import models.nrs.NrsSubmissionFailure.NrsErrorResponse
import models.nrs.NrsSubmissionResponse.NrsSubmissionResponse
import models.nrs.NrsSuccessResponse
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.MimeTypes
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import testConstants.NrsUtils
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NrsConnectorSpec extends AnyWordSpec with ComponentSpecBase {

  val request: JsValue = Json.toJson(NrsUtils.nrsSubmission)

  val successResponseBody: NrsSuccessResponse = NrsSuccessResponse("submissionId")

  val failureResponseBody: NrsSubmissionResponse = Left(NrsErrorResponse(status = INTERNAL_SERVER_ERROR))

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val connector: NrsConnector = app.injector.instanceOf[NrsConnector]

  "NrsConnectorSpec" when {

    ".submit()" when {

      "OK - 200" should {

        "return a successful response when provided valid headers and body" in {

          val requestBody = request

          val expectedResponse = Right(NrsSuccessResponse("submissionId"))

          stubFor(
            post(urlPathEqualTo("/nrs-orchestrator/submission"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo("dummy-api-key"))
              .withRequestBody(equalToJson(requestBody.toString(), true, false))
              .willReturn(aResponse()
                .withBody(NrsUtils.successResponseJson)
                .withStatus(ACCEPTED)))

          val result = connector.submit(NrsUtils.nrsSubmission)

          result.futureValue shouldBe expectedResponse
        }
      }
    }
  }
}
