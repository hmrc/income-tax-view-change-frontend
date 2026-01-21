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
import models.calculationList.{CalculationListErrorModel, CalculationListResponseModel}
import models.core.Nino
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import testConstants.BaseTestConstants._
import testConstants.CalculationListTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class CalculationListConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {

  trait Setup {
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new CalculationListConnector(mockHttpClientV2, getAppConfig())
  }

  "getLegacyCalculationListUrl" should {
    "return the correct url" in new Setup {
      connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString) shouldBe s"http://localhost:9999/income-tax-view-change/list-of-calculation-results/$testNino/$testTaxYear"
    }
  }

  "getCalculationListUrl" should {
    "return the correct url" in new Setup {
      connector.getCalculationListUrl(testNino, testTaxYear.toString) shouldBe s"http://localhost:9999/income-tax-view-change/calculation-list/$testNino/$testTaxYear"
    }
  }

  ".getLegacyCalculationList (API 1404)" should {
    "return a valid CalculationListResponseModel" in new Setup {
      val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpV2Get(itvc1404Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(testNino, testTaxYear.toString)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return an error" when {
      "json is invalid" in new Setup {
        val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
        val successResponse: HttpResponse = HttpResponse(status = OK, json = Json.obj(), headers = Map.empty)
        setupMockHttpV2Get(itvc1404Url)(successResponse)

        val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(testNino, testTaxYear.toString)
        result.futureValue shouldBe CalculationListErrorModel(500, "Json validation error parsing legacy calculation list response")
      }
    }
    "return an error" when {
      "receiving a 400-499 response" in new Setup {
        val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
        val errorResponse: HttpResponse = HttpResponse(status = IM_A_TEAPOT, """I'm a teapot""", headers = Map.empty)
        setupMockHttpV2Get(itvc1404Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(testNino, testTaxYear.toString)
        result.futureValue shouldBe CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
      }
      "receiving a 500-599 response" in new Setup {
        val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
        val errorResponse: HttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, """Dependent systems are currently not responding.""", headers = Map.empty)
        setupMockHttpV2Get(itvc1404Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(testNino, testTaxYear.toString)
        result.futureValue shouldBe CalculationListErrorModel(SERVICE_UNAVAILABLE, "Dependent systems are currently not responding.")
      }
    }
  }

  ".getCalculationList (API 1896)" should {
    "return a valid CalculationListResponseModel (including optional field `crystallised`)" in new Setup {
      val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpV2Get(itvc1896Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return a valid CalculationListResponseModel (excluding optional field `crystallised`)" in new Setup {
      val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpV2Get(itvc1896Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return an error" when {
      "json is invalid" in new Setup {
        val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
        val successResponse: HttpResponse = HttpResponse(status = OK, json = Json.obj(), headers = Map.empty)
        setupMockHttpV2Get(itvc1896Url)(successResponse)

        val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
        result.futureValue shouldBe CalculationListErrorModel(500, "Json validation error parsing calculation list response")
      }
    }
    "return an error" when {
      "receiving a 400-499 response" in new Setup {
        val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
        val errorResponse: HttpResponse = HttpResponse(status = IM_A_TEAPOT, """I'm a teapot""", headers = Map.empty)
        setupMockHttpV2Get(itvc1896Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
        result.futureValue shouldBe CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
      }
      "receiving a 500-599 response" in new Setup {
        val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
        val errorResponse: HttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, """Dependent systems are currently not responding.""", headers = Map.empty)
        setupMockHttpV2Get(itvc1896Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
        result.futureValue shouldBe CalculationListErrorModel(SERVICE_UNAVAILABLE, "Dependent systems are currently not responding.")
      }
    }
  }
}
