/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants.{testMtditid, testNino, testReferrerUrl, testSelfEmploymentId, testTaxYear}
import assets.CalcBreakdownTestConstants._
import assets.IncomeSourceDetailsTestConstants.singleBusinessIncome
import assets.NinoLookupTestConstants.{testNinoModelJson, _}
import assets.ReportDeadlinesTestConstants.{obligationsDataFromJson, obligationsDataSuccessModel}
import audit.AuditingService
import audit.mocks.MockAuditingService
import audit.models.{IncomeSourceDetailsRequestAuditModel, IncomeSourceDetailsResponseAuditModel, ReportDeadlinesRequestAuditModel, ReportDeadlinesResponseAuditModel}
import config.FrontendAppConfig
import mocks.MockHttp
import models.calculation._
import models.core.{NinoResponse, NinoResponseError}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesResponseModel}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future

class IncomeTaxViewChangeConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup extends IncomeTaxViewChangeConnector {

    val http: HttpClient = mockHttpGet
    val auditingService: AuditingService = mockAuditingService
    val config: FrontendAppConfig = mock[FrontendAppConfig]

    val baseUrl = "http://localhost:9999"

    when(config.itvcProtectedService) thenReturn baseUrl

  }

  "getLatestCalculationUrl" should {
    "return the correct url" in new Setup {
      getLatestCalculationUrl(testNino, testTaxYear.toString) shouldBe s"$baseUrl/income-tax-view-change/previous-tax-calculation/$testNino/$testTaxYear"
    }
  }

  "getIncomeSourcesUrl" should {
    "return the correct url" in new Setup {
      getIncomeSourcesUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/income-sources/$testMtditid"
    }
  }

  "getNinoLookupUrl" should {
    "return the correct url" in new Setup {
      getNinoLookupUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/nino-lookup/$testMtditid"
    }
  }

  "getReportDeadlinesUrl" should {
    "return the correct url" in new Setup {
      getReportDeadlinesUrl(testSelfEmploymentId, testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/income-source/$testSelfEmploymentId/report-deadlines"
    }
  }

  "getPreviousObligationsUrl" should {
    "return the correct url" in new Setup {
      getPreviousObligationsUrl(testSelfEmploymentId, testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/income-source/$testSelfEmploymentId/fulfilled-report-deadlines"
    }
  }
  
  "getLatestCalculation" should {

    val successResponse = HttpResponse(Status.OK, Some(testCalculationInputJson))
    val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{\"incomeTaxYTD\":\"somethingBad\"}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getLatestCalculationTestUrl = s"http://localhost:9999/income-tax-view-change/previous-tax-calculation/$testNino/$testTaxYear"

    "return a CalculationModel with JSON in case of success" in new Setup {
      setupMockHttpGet(getLatestCalculationTestUrl)(successResponse)

      val result: Future[CalculationResponseModel] = getLatestCalculation(testNino, testTaxYear)
      await(result) shouldBe testCalcModelCrystallised
    }

    "return a CalculationErrorModel in case of failure" in new Setup {
      setupMockHttpGet(getLatestCalculationTestUrl)(badResponse)

      val result: Future[CalculationResponseModel] = getLatestCalculation(testNino, testTaxYear)
      await(result) shouldBe CalculationErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return a CalculationErrorModel when bad JSON is received" in new Setup {
      setupMockHttpGet(getLatestCalculationTestUrl)(successResponseBadJson)

      val result: Future[CalculationResponseModel] = getLatestCalculation(testNino, testTaxYear)
      await(result) shouldBe CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing calculation model response")
    }

    "return a CalculationErrorModel in case of failed GET request" in new Setup {
      setupMockFailedHttpGet(getLatestCalculationTestUrl)(badResponse)

      val result: Future[CalculationResponseModel] = getLatestCalculation(testNino, testTaxYear)
      await(result) shouldBe CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")
    }
  }

  "getIncomeSources" should {

    val successResponse = HttpResponse(Status.OK, Some(Json.toJson(singleBusinessIncome)))
    val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getIncomeSourcesTestUrl = s"http://localhost:9999/income-tax-view-change/income-sources/$testMtditid"

    "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponse)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources(testMtditid, testNino)
      await(result) shouldBe singleBusinessIncome

      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino), Some(testReferrerUrl))
      verifyExtendedAudit(IncomeSourceDetailsResponseAuditModel(testMtditid, testNino, List(testSelfEmploymentId), None))
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponseBadJson)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources(testMtditid, testNino)
      await(result) shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")

      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino))
    }

    "return IncomeSourceDetailsError model in case of failure" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources(testMtditid, testNino)
      await(result) shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")

      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino))
    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getIncomeSourcesTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources(testMtditid, testNino)
      await(result) shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")

      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino))
    }
  }

  "getNino" should {

    val successResponse = HttpResponse(Status.OK, Some(testNinoModelJson))
    val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getNinoTestUrl = s"http://localhost:9999/income-tax-view-change/nino-lookup/$testMtditid"

    "return a Nino model when successful JSON is received" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(successResponse)

      val result: Future[NinoResponse] = getNino(testMtditid)
      await(result) shouldBe testNinoModel
    }

    "return NinoResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(successResponseBadJson)

      val result: Future[NinoResponse] = getNino(testMtditid)
      await(result) shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
    }

    "return NinoResponseError model in case of failure" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(badResponse)

      val result: Future[NinoResponse] = getNino(testMtditid)
      await(result) shouldBe NinoResponseError(Status.BAD_REQUEST, "Error Message")
    }

    "return NinoResponseError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getNinoTestUrl)(badResponse)

      val result: Future[NinoResponse] = getNino(testMtditid)
      await(result) shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")
    }

  }

  "getReportDeadlines" should {

    val successResponse = HttpResponse(Status.OK, responseJson = Some(obligationsDataFromJson))
    val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getReportDeadlinesTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/income-source/$testSelfEmploymentId/report-deadlines"

    "return a SuccessResponse with JSON in case of success" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(successResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines(testSelfEmploymentId)
      await(result) shouldBe obligationsDataSuccessModel

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId), Some(testReferrerUrl))
      verifyExtendedAudit(ReportDeadlinesResponseAuditModel(testMtditid, testNino, testSelfEmploymentId, obligationsDataSuccessModel.obligations))
    }

    "return ErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(successResponseBadJson)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getReportDeadlinesTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

  }

  "getPreviousObligations" should {

    val successResponse = HttpResponse(Status.OK, responseJson = Some(obligationsDataFromJson))
    val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getPreviousObligationsTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/income-source/$testSelfEmploymentId/fulfilled-report-deadlines"

    s"return a report deadlines model on a successful response with valid json" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations(testSelfEmploymentId)
      await(result) shouldBe obligationsDataSuccessModel

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId), Some(testReferrerUrl))
      verifyExtendedAudit(ReportDeadlinesResponseAuditModel(testMtditid, testNino, testSelfEmploymentId, obligationsDataSuccessModel.obligations))
    }

    "return an error model in case of failure" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponseBadJson)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPreviousObligationsTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations(testSelfEmploymentId)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

  }

}
