/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.IncomeSourceDetailsTestConstants.singleBusinessIncome
import assets.NinoLookupTestConstants.{testNinoModelJson, _}
import assets.PaymentAllocationsTestConstants._
import assets.FinancialDetailsTestConstants._
import assets.ReportDeadlinesTestConstants._
import audit.AuditingService
import audit.mocks.MockAuditingService
import audit.models._
import config.FrontendAppConfig
import mocks.MockHttp
import models.core.{NinoResponse, NinoResponseError}
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import models.paymentAllocations.{PaymentAllocationsErrorModel, PaymentAllocationsResponseModel}
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesResponseModel}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxViewChangeConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup extends IncomeTaxViewChangeConnector {

    val http: HttpClient = mockHttpGet
    val auditingService: AuditingService = mockAuditingService
    val config: FrontendAppConfig = mock[FrontendAppConfig]
    val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val baseUrl = "http://localhost:9999"

    when(config.itvcProtectedService) thenReturn baseUrl

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
      getReportDeadlinesUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/report-deadlines"
    }
  }

  "getPreviousObligationsUrl" should {
    "return the correct url" in new Setup {
      getPreviousObligationsUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/fulfilled-report-deadlines"
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

    val getReportDeadlinesTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/report-deadlines"

    "return a SuccessResponse with JSON in case of success" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(successResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines()
      await(result) shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino), Some(testReferrerUrl))
      verifyExtendedAudit(ReportDeadlinesResponseAuditModel(testMtditid, testNino, testSelfEmploymentId, reportDeadlinesDataSelfEmploymentSuccessModel.obligations))
    }

    "return ErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getReportDeadlinesTestUrl)(successResponseBadJson)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getReportDeadlinesTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getReportDeadlines()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

  }

  "getPreviousObligations" should {

    val successResponse = HttpResponse(Status.OK, responseJson = Some(obligationsDataFromJson))
    val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getPreviousObligationsTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/fulfilled-report-deadlines"

    s"return a report deadlines model on a successful response with valid json" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations()
      await(result) shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino), Some(testReferrerUrl))
      verifyExtendedAudit(ReportDeadlinesResponseAuditModel(testMtditid, testNino, testSelfEmploymentId, reportDeadlinesDataSelfEmploymentSuccessModel.obligations))
    }

    "return an error model in case of failure" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponseBadJson)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPreviousObligationsTestUrl)(badResponse)

      val result: Future[ReportDeadlinesResponseModel] = getPreviousObligations()
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")

      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino))
    }

  }

  "getPaymentAllocations" should {

    val successResponse = HttpResponse(Status.OK, Some(testValidPaymentAllocationsModelJson))
    val successResponseBadJson = HttpResponse(Status.OK, Some(testInvalidPaymentAllocationsModelJson))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getPaymentAllocationTestUrl =
      s"http://localhost:9999/income-tax-view-change/$testNino/payment-allocations/$testPaymentLot/$testPaymentLotItem"

    "return a PaymentAllocations model when successful JSON is received" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(successResponse)

      val result: Future[PaymentAllocationsResponseModel] = getPaymentAllocations(testPaymentLot, testPaymentLotItem)
      await(result) shouldBe testValidPaymentAllocationsModel
    }

    "return PaymentAllocationsErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(successResponseBadJson)

      val result: Future[PaymentAllocationsResponseModel] = getPaymentAllocations(testPaymentLot, testPaymentLotItem)
      await(result) shouldBe testPaymentAllocationsErrorModelParsing
    }

    "return PaymentAllocationsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(badResponse)

      val result: Future[PaymentAllocationsResponseModel] = getPaymentAllocations(testPaymentLot, testPaymentLotItem)
      await(result) shouldBe PaymentAllocationsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return PaymentAllocationsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPaymentAllocationTestUrl)(badResponse)

      val result: Future[PaymentAllocationsResponseModel] = getPaymentAllocations(testPaymentLot, testPaymentLotItem)
      await(result) shouldBe PaymentAllocationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }

  }

  "getFinancialDetails" should {

    val successResponse = HttpResponse(Status.OK, Some(testValidFinancialDetailsModelJson))
    val successResponseBadJson = HttpResponse(Status.OK, Some(testInvalidFinancialDetailsJson))
    val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

    val getChargesTestUrl =
      s"http://localhost:9999/income-tax-view-change/$testNino/financial-details/charges/from/$testFrom/to/$testTo"

    "return a FinancialDetails model when successful JSON is received" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponse)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testFrom, testTo)
      await(result) shouldBe testValidFinancialDetailsModel
    }

    "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponseBadJson)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testFrom, testTo)
      await(result) shouldBe testFinancialDetailsErrorModelParsing
    }

    "return FinancialDetailsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(badResponse)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testFrom, testTo)
      await(result) shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargesTestUrl)(badResponse)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testFrom, testTo)
      await(result) shouldBe FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }

  }
}
