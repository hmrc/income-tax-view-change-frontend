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
import models.admin.YourSelfAssessmentCharges
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json


class PayApiConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: PayApiConnector = app.injector.instanceOf[PayApiConnector]

  "PayApiConnector" when {

    ".startPaymentJourney()" when {

      "CREATED - 201" should {

        "return a successful response with valid json when FS is off and user is not an agent" in {

          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val json = Json.toJson(PaymentJourneyModel("id", "redirect-url")).toString()

          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe",
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyModel("id", "redirect-url")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, json)

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = false).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
        "return a successful response with valid json when FS is on and user is not an agent" in {
          enable(YourSelfAssessmentCharges)
          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val json = Json.toJson(PaymentJourneyModel("id", "redirect-url")).toString()
          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/your-self-assessment-charges" ,
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyModel("id", "redirect-url")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, json)

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = false).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
        "return a successful response with valid json when FS is off and user is an agent" in {

          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val json = Json.toJson(PaymentJourneyModel("id", "redirect-url")).toString()

          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl":"http://localhost:9081/report-quarterly/income-and-expenses/view/agents/payments-owed",
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/payments-owed"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyModel("id", "redirect-url")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, json)

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = true).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
        "return a successful response with valid json when FS is on and user is an agent" in {
          enable(YourSelfAssessmentCharges)
          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val json = Json.toJson(PaymentJourneyModel("id", "redirect-url")).toString()

          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges",
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyModel("id", "redirect-url")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, json)

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = true).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
      }

      "CREATED - 201" should {

        "return a successful response with invalid json" in {

          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val responseStatus = 201

          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe",
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyErrorResponse(responseStatus, "Invalid Json")

          WiremockHelper.stubPostWithRequest(url, requestBody, CREATED, "{}")

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = false).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
      }

      "INTERNAL_SERVER_ERROR - 500" should {

        "return PaymentJourneyErrorResponse" in {

          val utr = "saUtr"
          val amountInPence = 10000

          val url = s"/pay-api/mtd-income-tax/sa/journey/start"

          val responseStatus = 500

          val requestBody = Json.parse(
            """
              |{
              | "utr": "saUtr",
              | "amountInPence": 10000,
              | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe",
              | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/what-you-owe"
              |}
              """.stripMargin
          )

          val expectedResponse: PaymentJourneyResponse =
            PaymentJourneyErrorResponse(responseStatus, "Error Message")

          WiremockHelper.stubPostWithRequest(url, requestBody, INTERNAL_SERVER_ERROR, "Error Message")

          val result: PaymentJourneyResponse = connector.startPaymentJourney(utr, amountInPence, isAgent = false).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyPost(
            uri = s"/pay-api/mtd-income-tax/sa/journey/start"
          )
        }
      }
    }
  }
}