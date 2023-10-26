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

import audit.AuditingService
import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttp
import models.calculationList.{CalculationListErrorModel, CalculationListResponseModel}
import models.core.Nino
import org.mockito.Mockito.{mock, when}
import play.api.http.Status._
import testConstants.BaseTestConstants._
import testConstants.CalculationListTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class CalculationListConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup {
    val connector = new CalculationListConnector(mockHttpGet, appConfig)
    val baseUrl = "http://localhost:9999"
    when(appConfig.itvcProtectedService) thenReturn baseUrl
  }

  ".getLegacyCalculationList (API 1404)" should {
    "return a valid CalculationListResponseModel" in new Setup {
      val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpGet(itvc1404Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(Nino(testNino), testTaxYear.toString)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return an error" when {
      "receiving a 400-499 response" in new Setup {
        val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
        val errorResponse: HttpResponse = HttpResponse(status = IM_A_TEAPOT, """I'm a teapot""", headers = Map.empty)
        setupMockHttpGet(itvc1404Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(Nino(testNino), testTaxYear.toString)
        result.futureValue shouldBe CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
      }
      "receiving a 500-599 response" in new Setup {
        val itvc1404Url: String = connector.getLegacyCalculationListUrl(testNino, testTaxYear.toString)
        val errorResponse: HttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, """Dependent systems are currently not responding.""", headers = Map.empty)
        setupMockHttpGet(itvc1404Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getLegacyCalculationList(Nino(testNino), testTaxYear.toString)
        result.futureValue shouldBe CalculationListErrorModel(SERVICE_UNAVAILABLE, "Dependent systems are currently not responding.")
      }
    }
  }

  ".getCalculationList (API 1896)" should {
    "return a valid CalculationListResponseModel (including optional field `crystallised`)" in new Setup {
      val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpGet(itvc1896Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return a valid CalculationListResponseModel (excluding optional field `crystallised`)" in new Setup {
      val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
      val successResponse: HttpResponse = HttpResponse(status = OK, json = CalculationListTestConstants.jsonResponseFull, headers = Map.empty)
      setupMockHttpGet(itvc1896Url)(successResponse)

      val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
      result.futureValue shouldBe CalculationListTestConstants.calculationListFull
    }
    "return an error" when {
      "receiving a 400-499 response" in new Setup {
        val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
        val errorResponse: HttpResponse = HttpResponse(status = IM_A_TEAPOT, """I'm a teapot""", headers = Map.empty)
        setupMockHttpGet(itvc1896Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
        result.futureValue shouldBe CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
      }
      "receiving a 500-599 response" in new Setup {
        val itvc1896Url: String = connector.getCalculationListUrl(testNino, testTaxYearRange)
        val errorResponse: HttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, """Dependent systems are currently not responding.""", headers = Map.empty)
        setupMockHttpGet(itvc1896Url)(errorResponse)

        val result: Future[CalculationListResponseModel] = connector.getCalculationList(Nino(testNino), testTaxYearRange)
        result.futureValue shouldBe CalculationListErrorModel(SERVICE_UNAVAILABLE, "Dependent systems are currently not responding.")
      }
    }
  }
}
