/*
 * Copyright 2021 HM Revenue & Customs
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
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import play.api.http.Status.{ACCEPTED, UNAUTHORIZED}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

class RepaymentConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  val nino = "AA010101Q"
  val fullAmount = BigDecimal("303.00")
  val port = 9171
  val host = "http://localhost"
  val expectedNextUrl: BigDecimal => String = (amount: BigDecimal) => s"$host:$port/self-assessment-repayment-frontend/$amount/select-amount"

  val successResponse = HttpResponse(
    status = Status.ACCEPTED,
    json = Json.toJson(RepaymentJourneyModel(expectedNextUrl(fullAmount))),
    headers = Map.empty
  )
  val successResponseBadJson = HttpResponse(
    status = Status.ACCEPTED,
    json = Json.parse("{}"),
    headers = Map.empty
  )
  val unauthorizedResponse = HttpResponse(
    status = Status.UNAUTHORIZED,
    body = "Unauthorized Error Message"
  )

  object TestPayApiConnector extends RepaymentConnector(mockHttpGet, mockAuditingService, appConfig)

  "Calling .startRepayment" should {
    val testUrl = s"$host:$port/self-assessment-repayment-backend/start"

    val testBody = Json.parse(
      s"""
         |{
         | "nino": "$nino",
         | "fullAmount": $fullAmount
         |}
      """.stripMargin
    )

    "return a RepaymentResponse" when {

      "a 202 response is received with valid json" in {
        setupMockHttpPost(testUrl, testBody)(successResponse)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyModel(expectedNextUrl(fullAmount))
      }
    }

    "return a RepaymentJourneyErrorResponse" when {

      "a non 202 response is received" in {
        setupMockHttpPost(testUrl, testBody)(unauthorizedResponse)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(UNAUTHORIZED, "Unauthorized Error Message")
      }

      "a 202 response with invalid json is received" in {
        setupMockHttpPost(testUrl, testBody)(successResponseBadJson)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
      }
    }
  }

}
