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
import audit.models._
import config.FrontendAppConfig
import mocks.MockHttpV2
import models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import play.api.Configuration
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.NextUpdatesTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.Future

class ObligationsConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {

  trait Setup {
    val baseUrl = "http://localhost:9999"
    val fromDate: LocalDate = LocalDate.of(2023, 4, 1)
    val toDate: LocalDate = LocalDate.of(2024, 5, 1)

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val incomeTaxObligationsService: String = "http://localhost:9999"
      }

    val connector = new ObligationsConnector(mockHttpClientV2, mockAuditingService, getAppConfig())
  }

  "getOpenObligationsUrl" should {
    "return the correct url" in new Setup {
      connector.getOpenObligationsUrl(testNino) shouldBe s"$baseUrl/income-tax-obligations/$testNino/open-obligations"
    }
  }

  "getAllObligationsUrl" should {
    "return the correct url" in new Setup {
      connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino) shouldBe s"$baseUrl/income-tax-obligations/$testNino/obligations/from/$fromDate/to/$toDate"
    }
  }

  "getOpenObligations" should {

    val successResponse = HttpResponse(status = Status.OK, json = obligationsDataFromJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val emptyResponse = HttpResponse(status = Status.NOT_FOUND, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getNextUpdatesTestUrl = s"http://localhost:9999/income-tax-obligations/$testNino/open-obligations"

    "return a SuccessResponse with JSON in case of success" in new Setup {
      setupMockHttpV2Get(s"$getNextUpdatesTestUrl")(successResponse)

      val result: Future[ObligationsResponseModel] = connector.getOpenObligations()
      result.futureValue shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyExtendedAudit(NextUpdatesResponseAuditModel(individualUser, testSelfEmploymentId, nextUpdatesDataSelfEmploymentSuccessModel.obligations))
    }

    "return ErrorResponse model in case of failure" in new Setup {
      setupMockHttpV2Get(s"$getNextUpdatesTestUrl")(badResponse)

      val result: Future[ObligationsResponseModel] = connector.getOpenObligations()
      result.futureValue shouldBe ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")

    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpV2Get(s"$getNextUpdatesTestUrl")(successResponseBadJson)

      val result: Future[ObligationsResponseModel] = connector.getOpenObligations()
      result.futureValue shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")

    }

    "return NextUpdatesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpV2Get(s"$getNextUpdatesTestUrl")

      val result: Future[ObligationsResponseModel] = connector.getOpenObligations()
      result.futureValue shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")

    }

    s"return a empty SuccessResponse when ${NOT_FOUND} or ${FORBIDDEN}" in new Setup {
      setupMockHttpV2Get(s"$getNextUpdatesTestUrl")(emptyResponse)
      val result: Future[ObligationsResponseModel] = connector.getOpenObligations()
      result.futureValue shouldBe ObligationsModel(Seq.empty)
    }

  }

  "getAllObligations" should {

    val successResponse = HttpResponse(status = Status.OK, json = obligationsDataFromJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")
    val emptyResponse = HttpResponse(status = Status.NOT_FOUND, json = Json.parse("{}"), headers = Map.empty)


    s"return a valid updates model on a successful response with valid json" in new Setup {
      setupMockHttpV2Get(s"${connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino)}")(successResponse)

      val result: Future[ObligationsResponseModel] = connector.getAllObligationsDateRange(fromDate, toDate)
      result.futureValue shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyExtendedAudit(NextUpdatesResponseAuditModel(individualUser, testSelfEmploymentId, nextUpdatesDataSelfEmploymentSuccessModel.obligations))
    }

    "return an error model in case of failure" in new Setup {
      setupMockHttpV2Get(s"${connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino)}")(badResponse)

      val result: Future[ObligationsResponseModel] = connector.getAllObligationsDateRange(fromDate, toDate)
      result.futureValue shouldBe ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")

    }

    "return model when bad JSON is received" in new Setup {
      setupMockHttpV2Get(s"${connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino)}")(successResponseBadJson)

      val result: Future[ObligationsResponseModel] = connector.getAllObligationsDateRange(fromDate, toDate)
      result.futureValue shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")

    }

    "return NextUpdatesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpV2Get(s"${connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino)}")

      val result: Future[ObligationsResponseModel] = connector.getAllObligationsDateRange(fromDate, toDate)
      result.futureValue shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")

    }

    s"return a empty SuccessResponse when ${NOT_FOUND} or ${FORBIDDEN}" in new Setup {
      setupMockHttpV2Get(s"${connector.getAllObligationsDateRangeUrl(fromDate, toDate, testNino)}")(emptyResponse)
      val result: Future[ObligationsResponseModel] = connector.getAllObligationsDateRange(fromDate, toDate)
      result.futureValue shouldBe ObligationsModel(Seq.empty)
    }
  }
}
