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

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttpV2
import models.repaymentHistory.{RepaymentHistoryErrorModel, RepaymentHistoryModel, RepaymentHistoryResponseModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.RepaymentHistoryTestConstants.{repaymentHistoryOneRSI, validMultipleRepaymentHistoryJson, validRepaymentHistoryOneRSIJson}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class RepaymentHistoryConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {

  trait Setup {
    val baseUrl = "http://localhost:9999"
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new RepaymentHistoryConnector(mockHttpClientV2, getAppConfig())
  }

  "getRepaymentHistoryByIdUrl" should {
    "return the correct url" in new Setup {
      connector.getRepaymentHistoryByIdUrl(testNino, repaymentId) shouldBe s"$baseUrl/income-tax-view-change/repayments/$testNino/repaymentId/$repaymentId"
    }
  }

  "getAllRepaymentHistory" should {
    "return the correct url" in new Setup {
      connector.getAllRepaymentHistoryUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/repayments/$testNino"
    }
  }

  ".getRepaymentHistoryByRepaymentId" should {

    "return a valid RepaymentHistoryModel" when {

      val successResponse = HttpResponse(status = OK, json = validRepaymentHistoryOneRSIJson, headers = Map.empty)
      val successResponseMultipleRepayments = HttpResponse(status = OK, json = validMultipleRepaymentHistoryJson, headers = Map.empty)

      "receiving an OK with only one valid data item" in new Setup {
        setupMockHttpV2Get(connector.getRepaymentHistoryByIdUrl(testNino, repaymentId))(successResponse)

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByRepaymentId(testUserNino, repaymentId)
        result.futureValue shouldBe RepaymentHistoryModel(List(repaymentHistoryOneRSI))
      }

      "receiving an OK with multiple valid data items" in new Setup {
        setupMockHttpV2Get(connector.getRepaymentHistoryByIdUrl(testNino, repaymentId))(successResponseMultipleRepayments)

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByRepaymentId(testUserNino, repaymentId)
        result.futureValue shouldBe RepaymentHistoryModel(List(repaymentHistoryOneRSI, repaymentHistoryOneRSI))
      }
    }

    "return a NOT FOUND repayment history error" when {

      "receiving a not found response" in new Setup {
        setupMockHttpV2Get(connector.getRepaymentHistoryByIdUrl(testNino, repaymentId))(HttpResponse(status = Status.NOT_FOUND,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByRepaymentId(testUserNino, repaymentId)
        result.futureValue shouldBe RepaymentHistoryErrorModel(404, """"Error message"""")
      }
    }

    "return an INTERNAL_SERVER_ERROR repayment history error" when {

      "receiving a 500+ response" in new Setup {
        setupMockHttpV2Get(connector.getRepaymentHistoryByIdUrl(testNino, repaymentId))(HttpResponse(status = Status.SERVICE_UNAVAILABLE,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByRepaymentId(testUserNino, repaymentId)
        result.futureValue shouldBe RepaymentHistoryErrorModel(503, """"Error message"""")
      }

      "receiving a 400- response" in new Setup {
        setupMockHttpV2Get(connector.getRepaymentHistoryByIdUrl(testNino, repaymentId))(HttpResponse(status = Status.BAD_REQUEST,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByRepaymentId(testUserNino, repaymentId)
        result.futureValue shouldBe RepaymentHistoryErrorModel(400, """"Error message"""")
      }
    }
  }

  ".getRepaymentHistoryByNino" should {

    "return a valid RepaymentHistoryModel" when {

      val successResponse = HttpResponse(status = OK, json = validRepaymentHistoryOneRSIJson, headers = Map.empty)
      val successResponseMultipleRepayments = HttpResponse(status = OK, json = validMultipleRepaymentHistoryJson, headers = Map.empty)

      "receiving an OK with only one valid data item" in new Setup {
        setupMockHttpV2Get(connector.getAllRepaymentHistoryUrl(testNino))(successResponse)

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByNino(testUserNino)
        result.futureValue shouldBe RepaymentHistoryModel(List(repaymentHistoryOneRSI))
      }

      "receiving an OK with multiple valid data items" in new Setup {
        setupMockHttpV2Get(connector.getAllRepaymentHistoryUrl(testNino))(successResponseMultipleRepayments)

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByNino(testUserNino)
        result.futureValue shouldBe RepaymentHistoryModel(List(repaymentHistoryOneRSI, repaymentHistoryOneRSI))
      }
    }

    "return a NOT FOUND repayment history error" when {

      "receiving a not found response" in new Setup {
        setupMockHttpV2Get(connector.getAllRepaymentHistoryUrl(testNino))(HttpResponse(status = Status.NOT_FOUND,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByNino(testUserNino)
        result.futureValue shouldBe RepaymentHistoryErrorModel(404, """"Error message"""")
      }
    }

    "return an INTERNAL_SERVER_ERROR repayment history error" when {

      "receiving a 500+ response" in new Setup {
        setupMockHttpV2Get(connector.getAllRepaymentHistoryUrl(testNino))(HttpResponse(status = Status.SERVICE_UNAVAILABLE,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByNino(testUserNino)
        result.futureValue shouldBe RepaymentHistoryErrorModel(503, """"Error message"""")
      }

      "receiving a 400- response" in new Setup {
        setupMockHttpV2Get(connector.getAllRepaymentHistoryUrl(testNino))(HttpResponse(status = Status.BAD_REQUEST,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[RepaymentHistoryResponseModel] = connector.getRepaymentHistoryByNino(testUserNino)
        result.futureValue shouldBe RepaymentHistoryErrorModel(400, """"Error message"""")
      }
    }
  }
}
