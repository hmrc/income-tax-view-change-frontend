/*
 * Copyright 2019 HM Revenue & Customs
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

import audit.mocks.MockAuditingService
import mocks.MockHttp
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

class PayApiConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  val successResponse = HttpResponse(Status.CREATED, Some(Json.toJson(PaymentJourneyModel("journeyId", "http://www.redirect-url.com"))))
  val successResponseBadJson = HttpResponse(Status.CREATED, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestPayApiConnector extends PayApiConnector(mockHttpGet, mockAuditingService, frontendAppConfig)

  "Calling .startPaymentJourney" should {
    val testUrl = "http://localhost:9057/pay-api/mtd-income-tax/sa/journey/start"
    val testBody = Json.parse(
      """
        |{
        | "utr": "saUtr",
        | "amountInPence": 10000,
        | "returnUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view",
        | "backUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view"
        |}
      """.stripMargin
    )

    "return a PaymentJourneyModel" when {

      "a 201 response is received with valid json" in {
        setupMockHttpPost(testUrl, testBody)(successResponse)
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000)
        await(result) shouldBe PaymentJourneyModel("journeyId", "http://www.redirect-url.com")
      }
    }

    "return a PaymentErrorResponse" when {

      "a non 200 response is received" in {
        setupMockHttpPost(testUrl, testBody)(badResponse)
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000)
        await(result) shouldBe PaymentJourneyErrorResponse(400, "Error Message")
      }

      "a 201 response with invalid json is received" in {
        setupMockHttpPost(testUrl, testBody)(successResponseBadJson)
        val result = TestPayApiConnector.startPaymentJourney("saUtr", 10000)
        await(result) shouldBe PaymentJourneyErrorResponse(201, "Invalid Json")
      }
    }
  }
}
