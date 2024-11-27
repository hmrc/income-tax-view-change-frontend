/*
 * Copyright 2024 HM Revenue & Customs
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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import models.core.ResponseModel.ResponseModel
import models.core.{ErrorModel, Nino}
import models.creditsandrefunds.CreditsModel
import models.financialDetails.{Payment, Payments, PaymentsError}
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesErrorModel, OutstandingChargesModel, OutstandingChargesResponseModel}
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, FinancialDetailsWithDocumentDetailsErrorModel}
import models.paymentAllocations.{PaymentAllocationsError, PaymentAllocationsResponse}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testPaymentLot, testPaymentLotItem}
import testConstants.PaymentAllocationsTestConstants.{paymentAllocationChargesModelMultiplePayments, testValidPaymentAllocationsModel}

import java.time.LocalDate


class FinancialDetailsConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: FinancialDetailsConnector = app.injector.instanceOf[FinancialDetailsConnector]

  "FinancialDetailsConnector" when {

    ".getPaymentAllocations()" when {

      "OK" should {

        "return a successful PaymentAllocationsResponse" in {

          val testUserNino = "AA123456A"
          val response: String = Json.toJson(testValidPaymentAllocationsModel).toString()

          val url = s"/income-tax-view-change/$testUserNino/payment-allocations/$testPaymentLot/$testPaymentLotItem"

          WiremockHelper.stubGet(url, OK, response)

          val result: PaymentAllocationsResponse =
            connector.getPaymentAllocations(
              Nino(testUserNino),
              testPaymentLot,
              testPaymentLotItem
            ).futureValue

          result shouldBe testValidPaymentAllocationsModel
          WiremockHelper.verifyGet(uri = url)
        }

        "request returns json validation errors" should {

          "return a PaymentAllocationsError a message notifying us of json validation errors" in {

            val testUserNino = "AB123456C"
            val url = s"/income-tax-view-change/$testUserNino/payment-allocations/$testPaymentLot/$testPaymentLotItem"

            WiremockHelper.stubGet(url, OK, """{"bad_key":"bad_value"}""")

            val result: PaymentAllocationsResponse =
              connector.getPaymentAllocations(
                Nino(testUserNino),
                testPaymentLot,
                testPaymentLotItem
              ).futureValue

            result shouldBe PaymentAllocationsError(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Payment Allocations Data Response")
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a PaymentAllocationsError containing the error status and error response" in {

            val testUserNino = "AB123456C"
            val url = s"/income-tax-view-change/$testUserNino/payment-allocations/$testPaymentLot/$testPaymentLotItem"
            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result: PaymentAllocationsResponse =
              connector.getPaymentAllocations(
                Nino(testUserNino),
                testPaymentLot,
                testPaymentLotItem
              ).futureValue

            result shouldBe PaymentAllocationsError(INTERNAL_SERVER_ERROR, response)
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }

    ".getCreditsAndRefund()" when {

      "OK" should {

        "return a successful Right(CreditsModel)" in {

          val testUserNino = "AB123456C"
          val testCreditModel = CreditsModel(0.0, 0.0, Nil)
          val response: String = Json.toJson(testCreditModel).toString()

          val url = s"/income-tax-view-change/$testUserNino/financial-details/credits/from/2023-04-06/to/2024-04-05"

          WiremockHelper.stubGet(url, OK, response)

          val result: ResponseModel[CreditsModel] =
            connector.getCreditsAndRefund(
              taxYear = TaxYear(2023, 2024),
              nino = testUserNino,
            ).futureValue

          result shouldBe Right(testCreditModel)
          WiremockHelper.verifyGet(uri = url)
        }

        "request returns json validation errors" should {

          "return a Left(ErrorModel(500, \"Invalid JSON\"))" in {

            val testUserNino = "AB123456C"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/credits/from/2023-04-06/to/2024-04-05"

            WiremockHelper.stubGet(url, OK, """{"bad_key":"bad_value"}""")

            val result: ResponseModel[CreditsModel] =
              connector.getCreditsAndRefund(
                taxYear = TaxYear(2023, 2024),
                nino = testUserNino,
              ).futureValue

            result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON"))
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a Left(ErrorModel(500, \"Invalid JSON\"))" in {

            val testUserNino = "AB123456C"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/credits/from/2023-04-06/to/2024-04-05"
            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result: ResponseModel[CreditsModel] =
              connector.getCreditsAndRefund(
                taxYear = TaxYear(2023, 2024),
                nino = testUserNino,
              ).futureValue

            result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON"))
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }

    ".getOutstandingCharges()" when {

      "OK" should {

        "return a successful OutstandingChargesModel" in {

          val idType = "fakeId"
          val idNumber = "1337"
          val taxYear: String = "2023"
          val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

          val response: OutstandingChargesModel =
            OutstandingChargesModel(List(
              OutstandingChargeModel("BCD", Some(LocalDate.of(2025, 1, 1)), 123456789012345.67, 1234),
              OutstandingChargeModel("ACI", None, 12.67, 1234)
            ))

          WiremockHelper.stubGet(url, OK, Json.toJson(response).toString())

          val result: OutstandingChargesResponseModel =
            connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

          result shouldBe response
          WiremockHelper.verifyGet(uri = url)
        }

        "request returns json validation errors" should {

          "return a OutstandingChargesErrorModel" in {

            val idType = "fakeId"
            val idNumber = "1337"
            val taxYear: String = "2023"
            val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

            WiremockHelper.stubGet(url, OK, """{"bad_key":"bad_value"}""")

            val result: OutstandingChargesResponseModel =
              connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

            result shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing OutstandingCharges Data Response")
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a OutstandingChargesErrorModel with response body message" in {

            val idType = "fakeId"
            val idNumber = "1337"
            val taxYear: String = "2023"
            val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result: OutstandingChargesResponseModel =
              connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

            result shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, response)
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }

    ".getPaymentsUrl()" when {

      "OK" should {

        "return a successful Payments" in {

          val testUserNino = "AA123456A"
          val url = s"/income-tax-view-change/$testUserNino/financial-details/payments/from/2023-04-06/to/2024-04-05"

          val paymentFull: List[Payment] =
            List(
              Payment(
                reference = Some("reference"),
                amount = Some(100.00),
                outstandingAmount = Some(1.00),
                method = Some("method"),
                documentDescription = None,
                lot = Some("lot"),
                lotItem = Some("lotItem"),
                dueDate = Some(LocalDate.of(2024, 1, 1)),
                documentDate = LocalDate.of(2024, 1, 1), Some("DOCID01")
              )
            )

          val response: List[Payment] = paymentFull

          WiremockHelper.stubGet(url, OK, Json.toJson(response).toString())

          val result = connector.getPayments(2024).futureValue

          result shouldBe Payments(response)
          WiremockHelper.verifyGet(uri = url)
        }

        "request returns json validation errors" should {

          "return a PaymentsError" in {

            val testUserNino = "AA123456A"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/payments/from/2023-04-06/to/2024-04-05"

            val response = """{"bad_key":"bad_value"}"""

            WiremockHelper.stubGet(url, OK, response)

            val result = connector.getPayments(2024).futureValue

            result shouldBe PaymentsError(OK, "Json validation error")
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a OutstandingChargesErrorModel with response body message" in {

            val testUserNino = "AA123456A"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/payments/from/2023-04-06/to/2024-04-05"

            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result = connector.getPayments(2024).futureValue

            result shouldBe PaymentsError(INTERNAL_SERVER_ERROR, response)
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }

    ".getFinancialDetailsByDocumentId()" when {

      "OK" should {

        "return a FinancialDetailsWithDocumentDetailsModel" in {

          val testUserNino = "AA123456A"
          val documentNumber = "12345"
          val url = s"/income-tax-view-change/$testUserNino/financial-details/charges/documentId/$documentNumber"

          val response: FinancialDetailsWithDocumentDetailsModel = paymentAllocationChargesModelMultiplePayments

          WiremockHelper.stubGet(url, OK, Json.toJson(response).toString())

          val result = connector.getFinancialDetailsByDocumentId(
            nino = Nino(testUserNino),
            documentNumber = documentNumber
          ).futureValue

          result shouldBe response
          WiremockHelper.verifyGet(uri = url)
        }

        "request request body is a bad incorrect" should {

          "return an empty FinancialDetailsWithDocumentDetailsModel" in {

            val testUserNino = "AA123456A"
            val documentNumber = "12345"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/charges/documentId/$documentNumber"

            val response = """{"bad_key": 1}"""

            WiremockHelper.stubGet(url, OK, response)

            val result = connector.getFinancialDetailsByDocumentId(
              nino = Nino(testUserNino),
              documentNumber = documentNumber
            ).futureValue

            result shouldBe FinancialDetailsWithDocumentDetailsModel(List(), List())
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a FinancialDetailsWithDocumentDetailsErrorModel response body message" in {

            val testUserNino = "AA123456A"
            val documentNumber = "12345"
            val url = s"/income-tax-view-change/$testUserNino/financial-details/charges/documentId/$documentNumber"

            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result = connector.getFinancialDetailsByDocumentId(
              nino = Nino(testUserNino),
              documentNumber = documentNumber
            ).futureValue

            result shouldBe FinancialDetailsWithDocumentDetailsErrorModel(INTERNAL_SERVER_ERROR, response)
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }
  }
}