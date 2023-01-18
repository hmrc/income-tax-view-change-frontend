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

import mocks.MockHttp
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import models.core.{Nino, RepaymentRefund}
import org.junit.Ignore
import play.api.http.Status.{ACCEPTED, UNAUTHORIZED}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

@Ignore
class RepaymentConnectorSpec extends TestSupport with MockHttp {

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

  object TestPayApiConnector extends RepaymentConnector(mockHttpGet, appConfig)

  "Calling .startRepayment" should {
    val testStartUrl = s"$host:$port/self-assessment-refund-backend/itsa-viewer/journey/start-refund"

    val testBody = Json.toJson[RepaymentRefund](RepaymentRefund(nino, fullAmount))

    "return a RepaymentResponse" when {

      "a 202 response is received with valid json" in {
        setupMockHttpPost(testStartUrl, testBody)(successResponse)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyModel(expectedNextUrl(fullAmount))
      }
    }

    "return a RepaymentJourneyErrorResponse" when {

      "a non 202 response is received" in {
        setupMockHttpPost(testStartUrl, testBody)(unauthorizedResponse)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(UNAUTHORIZED, "Unauthorized Error Message")
      }

      "a 202 response with invalid json is received" in {
        setupMockHttpPost(testStartUrl, testBody)(successResponseBadJson)
        val result = TestPayApiConnector.start(nino, fullAmount).futureValue
        result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
      }
    }
  }

  "calling .view" should {
    val testViewUrl = s"$host:$port/self-assessment-refund-backend/itsa-viewer/journey/view-history"
    val body = Json.toJson[Nino](Nino(nino))
    "return a RepaymentResponse" when {

      "a 202 response is received with valid json" in {
        setupMockHttpPost(testViewUrl, body)(successResponse)
        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyModel(expectedNextUrl(fullAmount))
      }
    }

    "return a RepaymentJourneyErrorResponse" when {

      "a non 202 response is received" in {
        setupMockHttpPost(testViewUrl, body)(unauthorizedResponse)
        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyErrorResponse(UNAUTHORIZED, "Unauthorized Error Message")
      }

      "a 202 response with invalid json is received" in {
        setupMockHttpPost(testViewUrl, body)(successResponseBadJson)
        val result = TestPayApiConnector.view(nino).futureValue
        result shouldBe RepaymentJourneyErrorResponse(ACCEPTED, "Invalid Json")
      }
    }
  }

}
