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

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttp
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel}
import play.api.Configuration
import play.mvc.Http.Status
import testConstants.BaseTestConstants.{chargeReference, testMtditid}
import testConstants.ChargeHistoryTestConstants.{testChargeHistoryErrorModelParsing, testInvalidChargeHistoryDetailsModelJson, testValidChargeHistoryDetailsModelJson, testValidChargeHistoryModel}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class ChargeHistoryConnectorSpec extends TestSupport with MockHttp with MockAuditingService {


  trait Setup {
    val baseUrl = "http://localhost:9999"
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new ChargeHistoryConnector(httpClientMock, getAppConfig())
  }


  "getChargeHistoryUrl" should {
    "return the correct url" in new Setup {
      connector.getChargeHistoryUrl(testMtditid, chargeReference) shouldBe s"$baseUrl/income-tax-view-change/charge-history/$testMtditid/chargeRef/$chargeReference"
    }
  }

  "getChargeHistory" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidChargeHistoryDetailsModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidChargeHistoryDetailsModelJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getChargeHistoryUrlTestUrl =
      s"http://localhost:9999/income-tax-view-change/charge-history/$testMtditid/chargeRef/$chargeReference"

    "return a ChargeHistory model when successful JSON is received" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(successResponse)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, chargeReference)
      result.futureValue shouldBe testValidChargeHistoryModel

    }

    "return a ChargeHistory model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargeHistoryUrlTestUrl)
      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, chargeReference)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return ChargeHistoryErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(badResponse)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, chargeReference)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ChargeHistoryErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(successResponseBadJson)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, chargeReference)
      result.futureValue shouldBe testChargeHistoryErrorModelParsing
    }

  }

}
