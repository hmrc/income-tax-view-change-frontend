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
import audit.models.{AuditModel, ExtendedAuditModel}
import config.FrontendAppConfig
import models.financialDetails._
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsResponse}
import models.paymentAllocations.{PaymentAllocationsError, PaymentAllocationsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import org.mockito.{AdditionalMatchers, ArgumentMatchers}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Request
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.FinancialDetailsTestConstants._
import testConstants.PaymentAllocationsTestConstants._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


class FinancialDetailsConnectorSpec extends BaseConnectorSpec {

  trait Setup {

    val baseUrl = "http://localhost:9999"

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new FinancialDetailsConnector(mockHttpClientV2, getAppConfig())
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditingService)
  }

  lazy val mockAuditingService: AuditingService = mock(classOf[AuditingService])

  def verifyAudit(model: AuditModel, path: Option[String] = None): Unit = {
    verify(mockAuditingService).audit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )
  }

  def verifyExtendedAudit(model: ExtendedAuditModel, path: Option[String] = None): Unit =
    verify(mockAuditingService).extendedAudit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )

  def verifyExtendedAuditSent(model: ExtendedAuditModel): Unit =
    verify(mockAuditingService).extendedAudit(
      ArgumentMatchers.eq(model),
      any()
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )

  "FinancialDetailsConnector" when {

    ".getPaymentAllocations()" should {

      val successResponse =
        HttpResponse(
          status = Status.OK,
          json = testValidPaymentAllocationsModelJson,
          headers = Map.empty
        )
      val successResponseBadJson =
        HttpResponse(
          status = Status.OK,
          json = testInvalidPaymentAllocationsModelJson,
          headers = Map.empty
        )
      val badResponse =
        HttpResponse(
          status = Status.BAD_REQUEST,
          body = "Error Message"
        )

      "return a PaymentAllocations model when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[PaymentAllocationsResponse] =
          connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)

        result.futureValue shouldBe testValidPaymentAllocationsModel
      }

      "return PaymentAllocationsErrorResponse model in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
        result.futureValue shouldBe testPaymentAllocationsErrorModelParsing
      }

      "return PaymentAllocationsErrorResponse model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
        result.futureValue shouldBe PaymentAllocationsError(Status.BAD_REQUEST, "Error Message")
      }

      "return PaymentAllocationsErrorModel model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[PaymentAllocationsResponse] = connector.getPaymentAllocations(testUserNino, testPaymentLot, testPaymentLotItem)
        result.futureValue shouldBe PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
      }
    }

    ".getFinancialDetails() for a single tax year" should {

      val successResponse = HttpResponse(status = Status.OK, json = testValidFinancialDetailsModelJsonReads, headers = Map.empty)
      val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidFinancialDetailsJson, headers = Map.empty)
      val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

      "return a FinancialDetails model when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe testValidFinancialDetailsModel
      }

      "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe testFinancialDetailsErrorModelParsing
      }

      "return FinancialDetailsErrorResponse model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
      }

    }

    ".getFinancialDetails() for a range of tax years" should {

      val successResponse = HttpResponse(status = Status.OK, json = testValidFinancialDetailsModelJsonReads, headers = Map.empty)
      val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidFinancialDetailsJson, headers = Map.empty)
      val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

      "return a FinancialDetails model when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetailsByTaxYearRange(testTaxYearRange2017, testNino)
        result.futureValue shouldBe testValidFinancialDetailsModel
      }

      "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetailsByTaxYearRange(testTaxYearRange2017, testNino)
        result.futureValue shouldBe testFinancialDetailsErrorModelParsing
      }

      "return FinancialDetailsErrorResponse model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetailsByTaxYearRange(testTaxYearRange2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetailsByTaxYearRange(testTaxYearRange2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
      }

    }

    ".getPayments() for a single tax year" should {

      val payments: Seq[Payment] =
        Seq(
          Payment(reference = Some("reference"), amount = Some(100.00), outstandingAmount = None,
            method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
            dueDate = Some(fixedDate), documentDate = fixedDate, Some("DOCID01"))
        )

      val successResponse: HttpResponse =
        HttpResponse(
          status = OK,
          json = Json.toJson(payments),
          headers = Map.empty
        )

      val successResponseInvalidJson: HttpResponse =
        HttpResponse(
          status = OK,
          json = Json.toJson("test"),
          headers = Map.empty
        )

      val notFoundResponse: HttpResponse =
        HttpResponse(
          status = NOT_FOUND,
          body = "Not Found"
        )

      val internalServerErrorResponse: HttpResponse =
        HttpResponse(
          status = INTERNAL_SERVER_ERROR,
          body = "Internal Server Error"
        )

      "return Payments" when {
        "a successful response is received with valid json" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2016)

          result.futureValue shouldBe Payments(payments)
        }
      }

      "return a PaymentsError" when {
        "a successful response is received with invalid json" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponseInvalidJson))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2016)

          result.futureValue shouldBe PaymentsError(OK, "Json validation error")
        }

        "a 4xx response is returned" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(notFoundResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2016)

          result.futureValue shouldBe PaymentsError(NOT_FOUND, "Not Found")
        }

        "a 5xx response is returned" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(internalServerErrorResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2016)

          result.futureValue shouldBe PaymentsError(INTERNAL_SERVER_ERROR, "Internal Server Error")
        }
      }
    }

    ".getPayments() for a range of tax years" should {

      val payments: Seq[Payment] =
        Seq(
          Payment(reference = Some("reference"), amount = Some(100.00), outstandingAmount = None,
            method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
            dueDate = Some(fixedDate), documentDate = fixedDate, Some("DOCID01"))
        )

      val successResponse: HttpResponse =
        HttpResponse(
          status = OK,
          json = Json.toJson(payments),
          headers = Map.empty
        )

      val successResponseInvalidJson: HttpResponse =
        HttpResponse(
          status = OK,
          json = Json.toJson("test"),
          headers = Map.empty
        )

      val notFoundResponse: HttpResponse =
        HttpResponse(
          status = NOT_FOUND,
          body = "Not Found"
        )

      val internalServerErrorResponse: HttpResponse =
        HttpResponse(
          status = INTERNAL_SERVER_ERROR,
          body = "Internal Server Error"
        )

      "return Payments" when {
        "a successful response is received with valid json" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2017, testTaxYear2017.nextYear)

          result.futureValue shouldBe Payments(payments)
        }
      }

      "return a PaymentsError" when {
        "a successful response is received with invalid json" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponseInvalidJson))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2017, testTaxYear2017.nextYear)

          result.futureValue shouldBe PaymentsError(OK, "Json validation error")
        }

        "a 4xx response is returned" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(notFoundResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2017, testTaxYear2017.nextYear)

          result.futureValue shouldBe PaymentsError(NOT_FOUND, "Not Found")
        }

        "a 5xx response is returned" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(internalServerErrorResponse))

          val result: Future[PaymentsResponse] = connector.getPayments(testTaxYear2017, testTaxYear2017.nextYear)

          result.futureValue shouldBe PaymentsError(INTERNAL_SERVER_ERROR, "Internal Server Error")
        }
      }
    }

    ".getPaymentAllocation()" should {

      "a payment allocation" when {

        val successResponse = HttpResponse(status = OK, json = validPaymentAllocationChargesJson, headers = Map.empty)
        val successResponseMultiplePayments = HttpResponse(status = OK, json = validMultiplePaymentAllocationChargesJson, headers = Map.empty)

        "receiving an OK with only one valid data item" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.setHeader(any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponse))

          val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
          result.futureValue shouldBe paymentAllocationChargesModel
        }

        "receiving an OK with multiple valid data items" in new Setup {

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.setHeader(any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(successResponseMultiplePayments))

          val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
          result.futureValue shouldBe paymentAllocationChargesModelMultiplePayments
        }
      }

      "return a NOT FOUND payment allocation error" when {

        "receiving a not found response" in new Setup {

          val response = HttpResponse(status = Status.NOT_FOUND, json = Json.toJson("Error message"), headers = Map.empty)

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.setHeader(any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(response))

          val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
          result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(404, """"Error message"""")
        }
      }

      "return an INTERNAL_SERVER_ERROR payment allocation error" when {

        "receiving a 500+ response" in new Setup {

          val response = HttpResponse(status = Status.SERVICE_UNAVAILABLE, json = Json.toJson("Error message"), headers = Map.empty)

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.setHeader(any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(response))

          val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
          result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(503, """"Error message"""")
        }

        "receiving a 400- response" in new Setup {

          val response = HttpResponse(status = Status.BAD_REQUEST, json = Json.toJson("Error message"), headers = Map.empty)

          when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.setHeader(any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future(response))

          val result: Future[FinancialDetailsWithDocumentDetailsResponse] = connector.getFinancialDetailsByDocumentId(testUserNino, docNumber)
          result.futureValue shouldBe FinancialDetailsWithDocumentDetailsErrorModel(400, """"Error message"""")
        }
      }
    }
  }
}
