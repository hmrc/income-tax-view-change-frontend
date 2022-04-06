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

package services

import testConstants.BaseTestConstants._
import testConstants.PaymentAllocationsTestConstants._
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.services.MockFinancialDetailsService
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, PaymentAllocationError, PaymentAllocationViewModel}
import models.paymentAllocations.PaymentAllocationsError
import testUtils.TestSupport

class PaymentAllocationsServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector with MockFinancialDetailsService {

  object TestPaymentAllocationsService extends PaymentAllocationsService(mockIncomeTaxViewChangeConnector, mockFinancialDetailsService, appConfig)

  "get paymentAllocations" should {
    "return successful payment allocation details" when {
      "all fields are present" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation(testNino, "paymentLot", "paymentLotItem")(testValidPaymentAllocationsModel)
        setupGetPaymentAllocationCharges(testNino, "1040000872")(paymentAllocationChargesModel)
        setupGetPaymentAllocationCharges(testNino, "1040000873")(paymentAllocationChargesModel)

        val result = TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue

        result shouldBe Right(paymentAllocationViewModel)
      }
        "paymentLot and LotItem is missing" in {
          setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModelNoPayment)
          setupGetPaymentAllocationCharges(testNino, "1040000872")(paymentAllocationChargesModelNoPayment)
          setupGetPaymentAllocationCharges(testNino, "1040000873")(paymentAllocationChargesModelNoPayment)

          val result = TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue

          result shouldBe Right(paymentAllocationViewModelNoPayment)
        }
    }


    "return successful payment allocation details for LPI" when {
      "all fields are present" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(lpiPaymentAllocationParentChargesModel)
        setupGetPaymentAllocation(testNino, "paymentLot", "paymentLotItem")(testValidLpiPaymentAllocationsModel)
        setupGetPaymentAllocationCharges(testNino, "1040000872")(lpiPaymentAllocationParentChargesModel)
        setupGetPaymentAllocationCharges(testNino, "1040000873")(lpiPaymentAllocationParentChargesModel)
        mockGetAllFinancialDetails(List((2020, lpiFinancialDetailsModel)))

        val result = TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue

        result shouldBe Right(paymentAllocationViewModelLpi)
      }
    }

    "return an error" when {

      "all calls succeed except the payment charges which returns a internal server error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(FinancialDetailsWithDocumentDetailsErrorModel(500, "INTERNAL_SERVER"))

        TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue shouldBe Left(PaymentAllocationError())
      }

      "all calls succeed except the payment charges which returns a Not found error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(FinancialDetailsWithDocumentDetailsErrorModel(404, "NOT FOUND"))

        TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue shouldBe Left(PaymentAllocationError(Some(404)))
      }

      "all calls succeed except the call to payment Allocation which returns an error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation(testNino, "paymentLot", "paymentLotItem")(PaymentAllocationsError(404, "NOT FOUND"))

        TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue shouldBe Left(PaymentAllocationError())
      }

      "all calls succeed except the call to financial details with a payments SAP document number which returns an error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation(testNino, "paymentLot", "paymentLotItem")(testValidPaymentAllocationsModel)
        setupGetPaymentAllocationCharges(testNino, "1040000872")(FinancialDetailsWithDocumentDetailsErrorModel(404, "NOT FOUND"))
        setupGetPaymentAllocationCharges(testNino, "1040000873")(paymentAllocationChargesModel)


        TestPaymentAllocationsService.getPaymentAllocation(testUserNino, docNumber).futureValue shouldBe Left(PaymentAllocationError())
      }
    }
  }
}
