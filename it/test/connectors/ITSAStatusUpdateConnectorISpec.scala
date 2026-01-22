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
import connectors.constants.ITSAStatusUpdateConnectorConstants._
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import connectors.itsastatus.{ITSAStatusUpdateConnector, ITSAStatusUpdateRequest}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json

class ITSAStatusUpdateConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  "ITSAStatusUpdateConnector" when {

    ".updateITSAStatus()" when {

      "happy path" should {

        "return a successful response" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptOutRequestBody, "{}")

          val requestBody = ITSAStatusUpdateRequest(taxYear = taxYear2024.shortenTaxYearEnd, updateReason = optOutUpdateReason)

          val result = connector.updateITSAStatus(taxableEntityId, requestBody).futureValue

          result.status shouldBe NO_CONTENT
          result.body shouldBe ""

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }
      }

      "unhappy path" when {

        "BAD_REQUEST" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", BAD_REQUEST, correctOptOutRequestBody, invalidPayLoadFailureResponseBody)

          val requestBody = ITSAStatusUpdateRequest(taxYear = taxYear2024.shortenTaxYearEnd, updateReason = optOutUpdateReason)

          val result = connector.updateITSAStatus(taxableEntityId, requestBody).futureValue

          result.status shouldBe BAD_REQUEST
          result.body shouldBe invalidPayLoadFailureResponseBody

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }
      }
    }

    ".makeITSAStatusUpdate()" when {

      "request body" should {

        "be built correctly for OptOutReason" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptOutRequestBody, "{}")

          val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }

        "be built correctly for OptInReason" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptIntRequestBody, "{}")

          val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optInUpdateReason).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptIntRequestBody)))
          )
        }

        "be built correctly using the supplied tax year (2023)" in {

          val requestBody2023 =
            """{
              | "taxYear":"2022-23",
              | "updateReason":"11"
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, requestBody2023, "{}")

          val result = connector.makeITSAStatusUpdate(taxYear2023, taxableEntityId, optInUpdateReason).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(requestBody2023)))
          )
        }
      }

      "happy path" should {

        "return a successful response" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptOutRequestBody, "{}")

          val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }

        "handle unexpected HTTP status code" in {

          val unexpectedTeapotStatus = 418

          val failureResponseBody =
            """{
              |  "failures": [
              |    {
              |      "code": "UNEXPECTED_ERROR",
              |      "reason": "Unexpected response code"
              |    }
              |  ]
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", unexpectedTeapotStatus, correctOptOutRequestBody, failureResponseBody)

          val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe
            ITSAStatusUpdateResponseFailure(List(ErrorItem("UNEXPECTED_ERROR", "Unexpected response code")))

          // checks the request being made is performed with the correct body
          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }
      }

      "handle errors" when {

        "not NO_CONTENT" should {

          "return a error response with status INTERNAL_SERVER_ERROR" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", OK, correctOptOutRequestBody, "{}")

            val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optOutUpdateReason).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to json response: List((/failures,List(JsonValidationError(List(error.path.missing),ArraySeq()))))")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
            )
          }
        }

        "status is BAD_REQUEST" when {

          "with INVALID_PAYLOAD" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", BAD_REQUEST, correctOptOutRequestBody, invalidPayLoadFailureResponseBody)

            val result = connector.makeITSAStatusUpdate(taxYear2024, taxableEntityId, optOutUpdateReason).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
            )
          }
        }
      }
    }

    ".optIn()" when {

      "request body" should {

        "be built correctly for OptInReason" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptIntRequestBody, "{}")

          val result = connector.optIn(taxYear2024, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptIntRequestBody)))
          )
        }

        "be built correctly using the supplied tax year (2023)" in {

          val requestBody2023 =
            """{
              | "taxYear":"2022-23",
              | "updateReason":"11"
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, requestBody2023, "{}")

          val result = connector.optIn(taxYear2023, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(requestBody2023)))
          )
        }
      }

      "happy path" should {

        "return a successful response" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptIntRequestBody, "{}")

          val result = connector.optIn(taxYear2024, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptIntRequestBody)))
          )
        }
      }

      "handle errors" when {

        "not NO_CONTENT" should {

          "return a error response with status INTERNAL_SERVER_ERROR" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", OK, correctOptIntRequestBody, "{}")

            val result = connector.optIn(taxYear2024, taxableEntityId).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to json response: List((/failures,List(JsonValidationError(List(error.path.missing),ArraySeq()))))")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptIntRequestBody)))
            )
          }
        }

        "status is BAD_REQUEST" when {

          "with INVALID_PAYLOAD" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", BAD_REQUEST, correctOptIntRequestBody, invalidPayLoadFailureResponseBody)

            val result = connector.optIn(taxYear2024, taxableEntityId).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptIntRequestBody)))
            )
          }
        }
      }
    }

    ".optOut()" when {

      "request body" should {

        "be built correctly for OptOutReason" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptOutRequestBody, "{}")

          val result = connector.optOut(taxYear2024, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }

        "be built correctly using the supplied tax year (2023)" in {

          val requestBody2023 =
            """{
              | "taxYear":"2022-23",
              | "updateReason":"10"
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, requestBody2023, "{}")

          val result = connector.optOut(taxYear2023, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(requestBody2023)))
          )
        }
      }

      "happy path" should {

        "return a successful response" in {

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, correctOptOutRequestBody, "{}")

          val result = connector.optOut(taxYear2024, taxableEntityId).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
          )
        }
      }

      "handle errors" when {

        "not NO_CONTENT" should {

          "return a error response with status INTERNAL_SERVER_ERROR" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", OK, correctOptOutRequestBody, "{}")

            val result = connector.optOut(taxYear2024, taxableEntityId).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to json response: List((/failures,List(JsonValidationError(List(error.path.missing),ArraySeq()))))")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
            )
          }
        }

        "status is BAD_REQUEST" when {

          "with INVALID_PAYLOAD" in {

            WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", BAD_REQUEST, correctOptOutRequestBody, invalidPayLoadFailureResponseBody)

            val result = connector.optOut(taxYear2024, taxableEntityId).futureValue

            result shouldBe
              ITSAStatusUpdateResponseFailure(List(ErrorItem("INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload.")))

            WiremockHelper.verifyPut(
              uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
              optRequestBody = Some(Json.stringify(Json.parse(correctOptOutRequestBody)))
            )
          }
        }
      }
    }
  }
}