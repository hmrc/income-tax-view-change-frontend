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
import mocks.MockHttp
import models.core.{NinoResponse, NinoResponseError}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import play.api.Configuration
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.NinoLookupTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{singleBusinessAndPropertyMigrat2019, singleBusinessIncome}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class BusinessDetailsConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup {
    val baseUrl = "http://localhost:9999"

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
        override def incomeSourceOverrides(): Option[Seq[String]] = Some(incomeSourceOverride)
      }

    val connector = new BusinessDetailsConnector(mockHttpGet, mockAuditingService, getAppConfig())

  }

  "getBusinessDetailsUrl" should {
    "return the correct url" in new Setup {
      connector.getBusinessDetailsUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/get-business-details/nino/$testNino"
    }
  }

  "getIncomeSourcesUrl" should {
    "return the correct url" in new Setup {
      connector.getIncomeSourcesUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/income-sources/$testMtditid"
    }
  }

  "getNinoLookupUrl" should {
    "return the correct url" in new Setup {
      connector.getNinoLookupUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/nino-lookup/$testMtditid"
    }
  }


  "getBusinessDetails" should {

    val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessIncome), headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getBusinessDetailsTestUrl = s"http://localhost:9999/income-tax-view-change/get-business-details/nino/$testNino"

    "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(successResponse)

      val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
      result.futureValue shouldBe singleBusinessIncome
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(successResponseBadJson)

      val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
    }

    "return IncomeSourceDetailsError model in case of failure" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")
    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getBusinessDetailsTestUrl)

      val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")
    }
  }

  val incomeSourceOverride = Seq(
    "uk-property-reporting-method", // UK Property Select reporting method
    "foreign-property-reporting-method" // Foreign Property Select reporting method
  )

  "getIncomeSources" should {

    val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessAndPropertyMigrat2019), headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getIncomeSourcesTestUrl = s"http://localhost:9999/income-tax-view-change/income-sources/$testMtditid"


    "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponse)

      val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
      result.futureValue shouldBe singleBusinessAndPropertyMigrat2019

      verifyExtendedAudit(IncomeSourceDetailsResponseAuditModel(testMtdUserOptionNino, List(testSelfEmploymentId), List(testPropertyIncomeId), Some(testMigrationYear2019)))
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponseBadJson)

      val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")

    }

    "return IncomeSourceDetailsError model in case of failure" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
      result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")

    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getIncomeSourcesTestUrl)

      val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")

    }
  }

  "getNino" should {

    val successResponse = HttpResponse(status = Status.OK, json = testNinoModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(Status.BAD_REQUEST, body = "Error Message")

    val getNinoTestUrl = s"http://localhost:9999/income-tax-view-change/nino-lookup/$testMtditid"

    "return a Nino model when successful JSON is received" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(successResponse)

      val result: Future[NinoResponse] = connector.getNino(testMtditid)
      result.futureValue shouldBe testNinoModel
    }

    "return NinoResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(successResponseBadJson)

      val result: Future[NinoResponse] = connector.getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
    }

    "return NinoResponseError model in case of failure" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(badResponse)

      val result: Future[NinoResponse] = connector.getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.BAD_REQUEST, "Error Message")
    }

    "return NinoResponseError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getNinoTestUrl)

      val result: Future[NinoResponse] = connector.getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")
    }
  }
}
