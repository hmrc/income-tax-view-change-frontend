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
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesResponseModel}
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsResponse}
import models.paymentAllocations.{PaymentAllocationsError, PaymentAllocationsResponse}
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.ChargeHistoryTestConstants._
import testConstants.FinancialDetailsTestConstants._
import testConstants.OutstandingChargesTestConstants._
import testConstants.PaymentAllocationsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.Future

class FinancialDetailsConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup {
    val baseUrl = "http://localhost:9999"
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new FinancialDetailsConnector(mockHttpGet, getAppConfig())
  }

  "getOutstandingChargesUrl" should {
    "return the correct url" in new Setup {
      connector.getOutstandingChargesUrl(testSaUtr, testSaUtrId, testTo) shouldBe s"$baseUrl/income-tax-view-change/out-standing-charges/$testSaUtr/$testSaUtrId/$testTo"
    }
  }

  "getChargeHistoryUrl" should {
    "return the correct url" in new Setup {
      connector.getChargeHistoryUrl(testMtditid, docNumber) shouldBe s"$baseUrl/income-tax-view-change/charge-history/$testMtditid/docId/$docNumber"
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

      val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe testValidPaymentAllocationsModel
    }

    "return PaymentAllocationsErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(successResponseBadJson)

      val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe testPaymentAllocationsErrorModelParsing
    }

    "return PaymentAllocationsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getPaymentAllocationTestUrl)(badResponse)

      val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe PaymentAllocationsError(Status.BAD_REQUEST, "Error Message")
    }

    "return PaymentAllocationsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getPaymentAllocationTestUrl)

      val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
      result.futureValue shouldBe PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }

  }

  "getFinancialDetails" should {

    val successResponse = HttpResponse(status = Status.OK, json = testValidFinancialDetailsModelJsonReads, headers = Map.empty)
    val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidFinancialDetailsJson, headers = Map.empty)
    val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

    val getChargesTestUrl =
      s"http://localhost:9999/income-tax-view-change/$testNino/financial-details/charges/from/$testFrom/to/$testTo"

    "return a FinancialDetails model when successful JSON is received" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponse)

      val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe testValidFinancialDetailsModel
    }

    "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(successResponseBadJson)

      val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe testFinancialDetailsErrorModelParsing
    }

    "return FinancialDetailsErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargesTestUrl)(badResponse)

      val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
      result.futureValue shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargesTestUrl)

      val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
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

      val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe testValidOutstandingChargesModel

    }

    "return a OutstandingCharges model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getOutstandingChargesTestUrl)

      val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return OutstandingChargesErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getOutstandingChargesTestUrl)(badResponse)

      val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
      result.futureValue shouldBe OutstandingChargesErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return OutstandingChargesErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getOutstandingChargesTestUrl)(successResponseBadJson)

      val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
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

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe testValidChargeHistoryModel

    }

    "return a ChargeHistory model in case of future failed scenario" in new Setup {
      setupMockFailedHttpGet(getChargeHistoryUrlTestUrl)
      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return ChargeHistoryErrorResponse model in case of failure" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(badResponse)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ChargeHistoryErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getChargeHistoryUrlTestUrl)(successResponseBadJson)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, docNumber)
      result.futureValue shouldBe testChargeHistoryErrorModelParsing
    }

  }

  "getPayments" should {

    val getPaymentsTestUrl: String = {
      s"http://localhost:9999/income-tax-view-change/$testNino/financial-details/payments/from/$testFrom/to/$testTo"
    }

    val payments: Seq[Payment] = Seq(Payment(reference = Some("reference"), amount = Some(100.00), outstandingAmount = None,
      method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
      dueDate = Some(LocalDate.now), documentDate = LocalDate.now, Some("DOCID01")))

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

        val result: Future[PaymentsResponse] = connector.getPayments(testYear2017)

        result.futureValue shouldBe Payments(payments)
      }
    }

    "return a PaymentsError" when {
      "a successful response is received with invalid json" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(successResponseInvalidJson)

        val result: Future[PaymentsResponse] = connector.getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(OK, "Json validation error")
      }
      "a 4xx response is returned" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(notFoundResponse)

        val result: Future[PaymentsResponse] = connector.getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(NOT_FOUND, "Not Found")
      }
      "a 5xx response is returned" in new Setup {
        setupMockHttpGet(getPaymentsTestUrl)(internalServerErrorResponse)

        val result: Future[PaymentsResponse] = connector.getPayments(testYear2017)

        result.futureValue shouldBe PaymentsError(INTERNAL_SERVER_ERROR, "Internal Server Error")
      }
    }
  }

  ".getPaymentAllocation" should {

    "a payment allocation" when {

      val successResponse = HttpResponse(status = OK, json = validPaymentAllocationChargesJson, headers = Map.empty)
      val successResponseMultiplePayments = HttpResponse(status = OK, json = validMultiplePaymentAllocationChargesJson, headers = Map.empty)

      "receiving an OK with only one valid data item" in new Setup {
        setupMockHttpGet(connector.getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(successResponse)

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe paymentAllocationChargesModel
      }

      "receiving an OK with multiple valid data items" in new Setup {
        setupMockHttpGet(connector.getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(successResponseMultiplePayments)

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe paymentAllocationChargesModelMultiplePayments
      }
    }

    "return a NOT FOUND payment allocation error" when {

      "receiving a not found response" in new Setup {
        setupMockHttpGet(connector.getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.NOT_FOUND,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(404, """"Error message"""")
      }
    }

    "return an INTERNAL_SERVER_ERROR payment allocation error" when {

      "receiving a 500+ response" in new Setup {
        setupMockHttpGet(connector.getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.SERVICE_UNAVAILABLE,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(503, """"Error message"""")
      }

      "receiving a 400- response" in new Setup {
        setupMockHttpGet(connector.getFinancialDetailsByDocumentIdUrl(testNino, docNumber))(HttpResponse(status = Status.BAD_REQUEST,
          json = Json.toJson("Error message"), headers = Map.empty))

        val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
        result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(400, """"Error message"""")
      }
    }
  }
}
