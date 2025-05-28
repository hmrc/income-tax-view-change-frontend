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
import models.core.{SelfServeTimeToPayJourneyErrorResponse, SelfServeTimeToPayJourneyResponse, SelfServeTimeToPayJourneyResponseModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json


class SelfServeTimeToPayConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: SelfServeTimeToPayConnector = app.injector.instanceOf[SelfServeTimeToPayConnector]

  val url = s"/essttp-backend/sa/itsa/journey/start"

  val requestBody = Json.parse(
    s"""
     {
      "returnUrl": "/report-quarterly/income-and-expenses/view",
      "backUrl": "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
     }
    """.stripMargin
  )

  "SelfServeTimeToPayConnector" when {
    ".startSelfServeTimeToPayJourney()" when {
      "CREATED - 201" should {
        "return a successful response with valid json" in {

          val json = Json.toJson(SelfServeTimeToPayJourneyResponseModel("journeyId", "nextUrl"))

          val expectedResponse: SelfServeTimeToPayJourneyResponse =
            SelfServeTimeToPayJourneyResponseModel("journeyId", "nextUrl")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, json.toString())

          val result: SelfServeTimeToPayJourneyResponse = connector.startSelfServeTimeToPayJourney().futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPostContainingJson(
            uri = s"/essttp-backend/sa/itsa/journey/start",
            bodyPart = Some(Json.toJson(requestBody))
          )
        }
      }

      "CREATED - 201" should {
        "return a successful response with invalid json" in {

          val expectedResponse: SelfServeTimeToPayJourneyResponse =
            SelfServeTimeToPayJourneyErrorResponse(CREATED, "Invalid Json")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, "{}")

          val result: SelfServeTimeToPayJourneyResponse = connector.startSelfServeTimeToPayJourney().futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPostContainingJson(
            uri = s"/essttp-backend/sa/itsa/journey/start",
            bodyPart = Some(Json.toJson(requestBody))

          )
        }
      }

      "INTERNAL_SERVER_ERROR - 500" should {
        "return PaymentJourneyErrorResponse" in {

          val expectedResponse: SelfServeTimeToPayJourneyResponse =
            SelfServeTimeToPayJourneyErrorResponse(INTERNAL_SERVER_ERROR, "Error Message")

          WiremockHelper.stubPostWithRequest(url, requestBody, INTERNAL_SERVER_ERROR, "Error Message")

          val result: SelfServeTimeToPayJourneyResponse = connector.startSelfServeTimeToPayJourney().futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPostContainingJson(
            uri = s"/essttp-backend/sa/itsa/journey/start",
            bodyPart = Some(Json.toJson(requestBody))

          )
        }
      }
    }
  }
}
