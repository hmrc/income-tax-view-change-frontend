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
import models.updateIncomeSource.UpdateIncomeSourceResponse
import org.mockito.Mockito.{mock, when}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class UpdateIncomeSourceConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup extends UpdateIncomeSourceConnector {

    val http: HttpClient = mockHttpGet
    val auditingService: AuditingService = mockAuditingService
    val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
    val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val baseUrl = "http://localhost:9999"

    when(appConfig.itvcProtectedService) thenReturn baseUrl

  }

  "updateCessationDate" should {

    s"return a valid UpdateIncomeSourceResponseModel" in new Setup {
      setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
        UpdateIncomeSourceTestConstants.request,
        UpdateIncomeSourceTestConstants.successHttpResponse)
      val result: Future[UpdateIncomeSourceResponse] = updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
      result.futureValue shouldBe successResponse
    }

    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
      "invalid json response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.request,
          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
        val result: Future[UpdateIncomeSourceResponse] = updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
        result.futureValue shouldBe badJsonResponse
      }
      "receiving a 500+ response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.request, HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
            json = Json.toJson("Error message"), headers = Map.empty))
        val result: Future[UpdateIncomeSourceResponse] = updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
        result.futureValue shouldBe failureResponse
      }
    }

  }

  "updateTaxYearSpecific" should {

    s"return a valid UpdateIncomeSourceResponseModel" in new Setup {
      setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
        UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
        UpdateIncomeSourceTestConstants.successHttpResponse)
      val result: Future[UpdateIncomeSourceResponse] = updateIncomeSourceTaxYearSpecific(
        testNino, incomeSourceId, taxYearSpecific)
      result.futureValue shouldBe successResponse
    }

    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
      "invalid json response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
        val result: Future[UpdateIncomeSourceResponse] = updateIncomeSourceTaxYearSpecific(
          testNino, incomeSourceId, taxYearSpecific)
        result.futureValue shouldBe badJsonResponse
      }
      "receiving a 500+ response" in new Setup {
        setupMockHttpPutWithHeaderCarrier(getUpdateIncomeSourceUrl)(
          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
          HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
            json = Json.toJson("Error message"), headers = Map.empty))
        val result: Future[UpdateIncomeSourceResponse] = updateIncomeSourceTaxYearSpecific(
          testNino, incomeSourceId, taxYearSpecific)
        result.futureValue shouldBe failureResponse
      }
    }

  }

}
