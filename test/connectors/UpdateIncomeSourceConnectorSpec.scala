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
import mocks.MockHttp
import models.updateIncomeSource.UpdateIncomeSourceResponse
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.Future

class UpdateIncomeSourceConnectorSpec extends TestSupport with MockHttp with MockAuditingService {
  val http: HttpClient = mockHttpGet

  trait Setup {
    val baseUrl = "http://localhost:9999"
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new UpdateIncomeSourceConnector(mockHttpGet, getAppConfig())
  }

  "updateCessationDate" should {

    s"return a valid UpdateIncomeSourceResponseModel" in new Setup {
      setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
        UpdateIncomeSourceTestConstants.request,
        UpdateIncomeSourceTestConstants.successHttpResponse)
      val result: Future[UpdateIncomeSourceResponse] = connector.updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
      result.futureValue shouldBe successResponse
    }

    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
      "invalid json response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.request,
          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
        val result: Future[UpdateIncomeSourceResponse] = connector.updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
        result.futureValue shouldBe badJsonResponse
      }
      "receiving a 500+ response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.request, HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
            json = Json.toJson("Error message"), headers = Map.empty))
        val result: Future[UpdateIncomeSourceResponse] = connector.updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
        result.futureValue shouldBe failureResponse
      }
    }

  }

  "updateTaxYearSpecific" should {

    s"return a valid UpdateIncomeSourceResponseModel" in new Setup {
      setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
        UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
        UpdateIncomeSourceTestConstants.successHttpResponse)
      val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
        testNino, incomeSourceId, taxYearSpecific)
      result.futureValue shouldBe successResponse
    }

    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
      "invalid json response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
        val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
          testNino, incomeSourceId, taxYearSpecific)
        result.futureValue shouldBe badJsonResponse
      }
      "receiving a 500+ response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
          HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
            json = Json.toJson("Error message"), headers = Map.empty))
        val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
          testNino, incomeSourceId, taxYearSpecific)
        result.futureValue shouldBe failureResponse
      }
    }
  }
}
