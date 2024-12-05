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
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json


class RepaymentConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: RepaymentConnector = app.injector.instanceOf[RepaymentConnector]

  "RepaymentConnector" when {

    ".start()" when {

      "ACCEPTED" should {

        "return a successful RepaymentJourneyModel" in {

          val testUserNino = "AA123456A"
          val fullAmount = 1337.99
          val response: RepaymentJourneyModel = RepaymentJourneyModel(nextUrl = "/some-fake-next-url")

          val url = "/self-assessment-refund-backend/itsa-viewer/journey/start-refund"

          WiremockHelper.stubPost(url, ACCEPTED, Json.toJson(response).toString())

          val result =
            connector
              .start(testUserNino, fullAmount)
              .futureValue

          result shouldBe response
        }

        "request returns json validation errors" should {

          "return a RepaymentJourneyErrorResponse a message notifying us of json validation errors" in {

            val testUserNino = "AA123456A"
            val fullAmount = 1337.99

            val url = "/self-assessment-refund-backend/itsa-viewer/journey/start-refund"
            val response = """{"bad_key":"bad_value"}"""

            WiremockHelper.stubPost(url, ACCEPTED, response)

            val result =
              connector
                .start(testUserNino, fullAmount)
                .futureValue

            result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a RepaymentJourneyErrorResponse containing the error status and error response" in {

            val testUserNino = "AA123456A"
            val fullAmount = 1337.99

            val url = s"/self-assessment-refund-backend/itsa-viewer/journey/start-refund"
            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubPost(url, INTERNAL_SERVER_ERROR, response)

            val result =
              connector
                .start(testUserNino, fullAmount)
                .futureValue

            result shouldBe RepaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, response)
          }
        }
      }
    }

    ".view()" when {

      "ACCEPTED" should {

        "return a successful RepaymentJourneyModel" in {

          val testUserNino = "AA123456A"

          val url = "/self-assessment-refund-backend/itsa-viewer/journey/view-history"
          val response:RepaymentJourneyModel = RepaymentJourneyModel(nextUrl = "/some-fake-next-url")

          WiremockHelper.stubPost(url, ACCEPTED, Json.toJson(response).toString())

          val result =
            connector
              .view(testUserNino)
              .futureValue

          result shouldBe response
        }

        "request returns json validation errors" should {

          "return a RepaymentJourneyErrorResponse with message for Invalid Json" in {

            val testUserNino = "AA123456A"

            val url = "/self-assessment-refund-backend/itsa-viewer/journey/view-history"
            val response = """{"bad_key":"bad_value"}"""

            WiremockHelper.stubPost(url, ACCEPTED, response)

            val result =
              connector
                .view(testUserNino)
                .futureValue

            result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return RepaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, response)" in {

            val testUserNino = "AA123456A"

            val url = "/self-assessment-refund-backend/itsa-viewer/journey/view-history"
            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubPost(url, INTERNAL_SERVER_ERROR, response)

            val result =
              connector
                .view(testUserNino)
                .futureValue

            result shouldBe RepaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, response)
          }
        }
      }
    }
  }
}