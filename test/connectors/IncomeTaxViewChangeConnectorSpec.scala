/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.ChargeHistoryTestConstants._
import testConstants.FinancialDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.{singleBusinessAndPropertyMigrat2019, singleBusinessIncome}
import testConstants.NinoLookupTestConstants.{testNinoModelJson, _}
import testConstants.OutstandingChargesTestConstants._
import testConstants.PaymentAllocationsTestConstants._
import testConstants.NextUpdatesTestConstants._
import audit.AuditingService
import audit.mocks.MockAuditingService
import audit.models._
import config.FrontendAppConfig
import config.featureswitch.TxmEventsApproved
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import mocks.MockHttp
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel}
import models.core.{NinoResponse, NinoResponseError}
import models.financialDetails._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesResponseModel}
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsResponse}
import models.paymentAllocations.{PaymentAllocationsError, PaymentAllocationsResponse}
import models.nextUpdates.{NextUpdatesErrorModel, NextUpdatesResponseModel}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxViewChangeConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup extends IncomeTaxViewChangeConnector {

    val http: HttpClient = mockHttpGet
    val auditingService: AuditingService = mockAuditingService
    val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val baseUrl = "http://localhost:9999"

    when(appConfig.itvcProtectedService) thenReturn baseUrl

  }

  "getBusinessDetailsUrl" should {
    "return the correct url" in new Setup {
      getBusinessDetailsUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/get-business-details/nino/$testNino"
    }
  }

  "getOutstandingChargesUrl" should {
    "return the correct url" in new Setup {
      getOutstandingChargesUrl(testSaUtr, testSaUtrId, testTo) shouldBe s"$baseUrl/income-tax-view-change/out-standing-charges/$testSaUtr/$testSaUtrId/$testTo"
    }
  }

  "getChargeHistoryUrl" should {
    "return the correct url" in new Setup {
      getChargeHistoryUrl(testMtditid, docNumber) shouldBe s"$baseUrl/income-tax-view-change/charge-history/$testMtditid/docId/$docNumber"
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

  "getNextUpdatesUrl" should {
    "return the correct url" in new Setup {
      getReportDeadlinesUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/report-deadlines"
    }
  }

  "getPreviousObligationsUrl" should {
    "return the correct url" in new Setup {
      getPreviousObligationsUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/$testNino/fulfilled-report-deadlines"
    }
  }

  "getBusinessDetails" should {

    val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessIncome), headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getBusinessDetailsTestUrl = s"http://localhost:9999/income-tax-view-change/get-business-details/nino/$testNino"

    "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(successResponse)

      val result: Future[IncomeSourceDetailsResponse] = getBusinessDetails(testNino)
      result.futureValue shouldBe singleBusinessIncome
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(successResponseBadJson)

      val result: Future[IncomeSourceDetailsResponse] = getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
    }

    "return IncomeSourceDetailsError model in case of failure" in new Setup {
      setupMockHttpGet(getBusinessDetailsTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")
    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getBusinessDetailsTestUrl)

      val result: Future[IncomeSourceDetailsResponse] = getBusinessDetails(testNino)
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")
    }
  }


  "getIncomeSources" should {

    val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessAndPropertyMigrat2019), headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getIncomeSourcesTestUrl = s"http://localhost:9999/income-tax-view-change/income-sources/$testMtditid"

    "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {
      enable(TxmEventsApproved)
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponse)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources()
      result.futureValue shouldBe singleBusinessAndPropertyMigrat2019

      verifyExtendedAudit(IncomeSourceDetailsResponseAuditModel(testMtdUserNino, List(testSelfEmploymentId), Some(testPropertyIncomeId), Some(testMigrationYear2019), true))
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(successResponseBadJson)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources()
      result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")

    }

    "return IncomeSourceDetailsError model in case of failure" in new Setup {
      setupMockHttpGet(getIncomeSourcesTestUrl)(badResponse)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources()
      result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")

    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getIncomeSourcesTestUrl)

      val result: Future[IncomeSourceDetailsResponse] = getIncomeSources()
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

      val result: Future[NinoResponse] = getNino(testMtditid)
      result.futureValue shouldBe testNinoModel
    }

    "return NinoResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(successResponseBadJson)

      val result: Future[NinoResponse] = getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
    }

    "return NinoResponseError model in case of failure" in new Setup {
      setupMockHttpGet(getNinoTestUrl)(badResponse)

      val result: Future[NinoResponse] = getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.BAD_REQUEST, "Error Message")
    }

    "return NinoResponseError model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getNinoTestUrl)

      val result: Future[NinoResponse] = getNino(testMtditid)
      result.futureValue shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")
    }

  }

  "getNextUpdates" should {

    val successResponse = HttpResponse(status = Status.OK, json = obligationsDataFromJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getNextUpdatesTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/report-deadlines"

    "return a SuccessResponse with JSON in case of success" in new Setup {
      setupMockHttpGet(getNextUpdatesTestUrl)(successResponse)

      val result: Future[NextUpdatesResponseModel] = getNextUpdates()
      result.futureValue shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyExtendedAudit(NextUpdatesResponseAuditModel(individualUser, testSelfEmploymentId, nextUpdatesDataSelfEmploymentSuccessModel.obligations))
    }

    "return ErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getNextUpdatesTestUrl)(badResponse)

      val result: Future[NextUpdatesResponseModel] = getNextUpdates()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.BAD_REQUEST, "Error Message")

    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getNextUpdatesTestUrl)(successResponseBadJson)

      val result: Future[NextUpdatesResponseModel] = getNextUpdates()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")

    }

    "return NextUpdatesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getNextUpdatesTestUrl)

      val result: Future[NextUpdatesResponseModel] = getNextUpdates()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")

    }

  }

  "getPreviousObligations" should {

    val successResponse = HttpResponse(status = Status.OK, json = obligationsDataFromJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getPreviousObligationsTestUrl = s"http://localhost:9999/income-tax-view-change/$testNino/fulfilled-report-deadlines"

    s"return a next updates model on a successful response with valid json" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponse)

      val result: Future[NextUpdatesResponseModel] = getPreviousObligations()
      result.futureValue shouldBe obligationsDataSelfEmploymentOnlySuccessModel

      verifyExtendedAudit(NextUpdatesResponseAuditModel(individualUser, testSelfEmploymentId, nextUpdatesDataSelfEmploymentSuccessModel.obligations))
    }

    "return an error model in case of failure" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(badResponse)

      val result: Future[NextUpdatesResponseModel] = getPreviousObligations()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.BAD_REQUEST, "Error Message")

    }

    "return BusinessListError model when bad JSON is received" in new Setup {
      setupMockHttpGet(getPreviousObligationsTestUrl)(successResponseBadJson)

      val result: Future[NextUpdatesResponseModel] = getPreviousObligations()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")

    }

    "return NextUpdatesErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPreviousObligationsTestUrl)

      val result: Future[NextUpdatesResponseModel] = getPreviousObligations()
      result.futureValue shouldBe NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")

    }

  }

  "getPaymentAllocations" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidPaymentAllocationsModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidPaymentAllocationsModelJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getPaymentAllocationTestUrl =
      s"http://localhost:9999/income-tax-view-change/$testNino/payment-allocations/$testPaymentLot/$testPaymentLotItem"

    "return a PaymentAllocations model when successful JSON is received" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(successResponse)

      val result: Future[PaymentAllocationsResponse] = getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe testValidPaymentAllocationsModel
    }

    "return PaymentAllocationsErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(successResponseBadJson)

      val result: Future[PaymentAllocationsResponse] = getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe testPaymentAllocationsErrorModelParsing
    }

    "return PaymentAllocationsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(badResponse)

      val result: Future[PaymentAllocationsResponse] = getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe PaymentAllocationsError(Status.BAD_REQUEST, "Error Message")
    }

    "return PaymentAllocationsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPaymentAllocationTestUrl)

      val result: Future[PaymentAllocationsResponse] = getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }

  }

  "getFinancialDetails" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidFinancialDetailsModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidFinancialDetailsJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getChargesTestUrl =
      s"http://localhost:9999/income-tax-view-change/$testNino/financial-details/charges/from/$testFrom/to/$testTo"

    "return a FinancialDetails model when successful JSON is received" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponse)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe testValidFinancialDetailsModel
    }

    "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponseBadJson)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe testFinancialDetailsErrorModelParsing
    }

    "return FinancialDetailsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(badResponse)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargesTestUrl)

      val result: Future[FinancialDetailsResponseModel] = getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }

  }

  "getOutstandingCharges" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidOutStandingChargeModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidOutstandingChargesJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getOutstandingChargesTestUrl =
      s"http://localhost:9999/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear"

    "return a OutstandingCharges model when successful JSON is received" in new Setup {
      setupMockHttpGet(getOutstandingChargesTestUrl)(successResponse)

      val result: Future[OutstandingChargesResponseModel] = getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe testValidOutstandingChargesModel

    }

    "return a OutstandingCharges model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getOutstandingChargesTestUrl)

      val result: Future[OutstandingChargesResponseModel] = getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return OutstandingChargesErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getOutstandingChargesTestUrl)(badResponse)

      val result: Future[OutstandingChargesResponseModel] = getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe OutstandingChargesErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return OutstandingChargesErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getOutstandingChargesTestUrl)(successResponseBadJson)

      val result: Future[OutstandingChargesResponseModel] = getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe testOutstandingChargesErrorModelParsing
    }

  }

  "getChargeHistory" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidChargeHistoryDetailsModelJson, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidChargeHistoryDetailsModelJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getChargeHistoryUrlTestUrl =
      s"http://localhost:9999/income-tax-view-change/charge-history/$testMtditid/docId/$docNumber"

    "return a ChargeHistory model when successful JSON is received" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(successResponse)

      val result: Future[ChargeHistoryResponseModel] = getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe testValidChargeHistoryModel

    }

    "return a ChargeHistory model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargeHistoryUrlTestUrl)
      val result: Future[ChargeHistoryResponseModel] = getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return ChargeHistoryErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(badResponse)

      val result: Future[ChargeHistoryResponseModel] = getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ChargeHistoryErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(successResponseBadJson)

      val result: Future[ChargeHistoryResponseModel] = getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe testChargeHistoryErrorModelParsing
    }

  }

  "getPayments" should {

    val getPaymentsTestUrl: String = {
      s"http://localhost:9999/income-tax-view-change/$testNino/financial-details/payments/from/$testFrom/to/$testTo"
    }

    val payments: Seq[Payment] = Seq(Payment(reference = Some("reference"), amount = Some(100.00), method = Some("method"), lot = Some("lot"), lotItem = Some("lotItem"), date = Some("date"), Some("DOCID01")))

    val successResponse: HttpResponse = HttpResponse(
      status = OK,
      json = Json.toJson(payments),
      headers = Map.empty
    )

    val successResponseInvalidJson: HttpResponse = HttpResponse(
      status = OK,
      json = Json.toJson("test"),
      headers = Map.empty
    )

    val notFoundResponse: HttpResponse = HttpResponse(
      status = NOT_FOUND,
      body = "Not Found"
    )

    val internalServerErrorResponse: HttpResponse = HttpResponse(
      status = INTERNAL_SERVER_ERROR,
      body = "Internal Server Error"
    )

    "return Payments" when {
      "a successful response is received with valid json" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(successResponse)

        val result: Future[PaymentsResponse] = getPayments(testYear2017)

        result.futureValue shouldBe Payments(payments)
      }
    }

    "return a PaymentsError" when {
      "a successful response is received with invalid json" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(successResponseInvalidJson)

        val result: Future[PaymentsResponse] = getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(OK, "Json validation error")
      }
      "a 4xx response is returned" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(notFoundResponse)

        val result: Future[PaymentsResponse] = getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(NOT_FOUND, "Not Found")
      }
      "a 5xx response is returned" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(internalServerErrorResponse)

        val result: Future[PaymentsResponse] = getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(INTERNAL_SERVER_ERROR, "Internal Server Error")
      }
    }
  }

  ".getPaymentAllocation" should {

    "a payment allocation" when {

      val successResponse = HttpResponse(status = OK, json = validPaymentAllocationChargesJson, headers = Map.empty)
      val successResponseMultiplePayments = HttpResponse(status = OK, json = validMultiplePaymentAllocationChargesJson, headers = Map.empty)

      "receiving an OK with only one valid data item" in new Setup {
        setupMockHttpGet(getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(successResponse)

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe paymentAllocationChargesModel
      }

      "receiving an OK with multiple valid data items" in new Setup {
        setupMockHttpGet(getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(successResponseMultiplePayments)

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe paymentAllocationChargesModelMultiplePayments
      }
    }

    "return a NOT FOUND payment allocation error" when {

      "receiving a not found response" in new Setup {
        setupMockHttpGet(getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.NOT_FOUND,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(404, """"Error message"""")
      }
    }

    "return an INTERNAL_SERVER_ERROR payment allocation error" when {

      "receiving a 500+ response" in new Setup {
        setupMockHttpGet(getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.SERVICE_UNAVAILABLE,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(503, """"Error message"""")
      }

      "receiving a 400- response" in new Setup {
        setupMockHttpGet(getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.BAD_REQUEST,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(400, """"Error message"""")
      }
    }
  }
}
