/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.PaymentAllocationChargesTestConstants.paymentAllocationChargesModel
import assets.PaymentAllocationsTestConstants.testValidPaymentAllocationsModel
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.paymentAllocationCharges.{PaymentAllocationChargesErrorModel, PaymentAllocationViewModel}
import models.paymentAllocations.PaymentAllocationsError
import services.PaymentAllocationsService.PaymentAllocationError
import testUtils.TestSupport

class PaymentAllocationsServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector  {

  object TestPaymentAllocationsService extends PaymentAllocationsService(mockIncomeTaxViewChangeConnector, appConfig)

  "get paymentAllocations" should {
    "return successful payment allocation details" when {
      "all fields are present" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation("paymentLot", "paymentLotItem")(testValidPaymentAllocationsModel)
        setupGetPaymentAllocationCharges(testNino, "1040000872")(paymentAllocationChargesModel)
        setupGetPaymentAllocationCharges(testNino, "1040000873")(paymentAllocationChargesModel)

        val result = await(TestPaymentAllocationsService.getPaymentAllocation(testNino, docNumber))

        result shouldBe Right(
            PaymentAllocationViewModel(paymentAllocationChargesModel,
              Seq(
                (testValidPaymentAllocationsModel, Some("2021-01-31")),
                (testValidPaymentAllocationsModel, Some("2021-01-31"))
              )
            )
          )
      }
    }

    "return an error" when {

      "all calls succeed except the payment charges which returns an error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(PaymentAllocationChargesErrorModel(404, "NOT FOUND"))

        await(TestPaymentAllocationsService.getPaymentAllocation(testNino, docNumber)) shouldBe Left(PaymentAllocationError)
      }

      "all calls succeed except the call to payment Allocation which returns an error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation("paymentLot", "paymentLotItem")(PaymentAllocationsError(404, "NOT FOUND"))

        await(TestPaymentAllocationsService.getPaymentAllocation(testNino, docNumber)) shouldBe Left(PaymentAllocationError)
      }

      "all calls succeed except the call to financial details with a payments SAP document number which returns an error" in {
        setupGetPaymentAllocationCharges(testNino, docNumber)(paymentAllocationChargesModel)
        setupGetPaymentAllocation("paymentLot", "paymentLotItem")(testValidPaymentAllocationsModel)
        setupGetPaymentAllocationCharges(testNino, "1040000872")(PaymentAllocationChargesErrorModel(404, "NOT FOUND"))
        setupGetPaymentAllocationCharges(testNino, "1040000873")(paymentAllocationChargesModel)


        await(TestPaymentAllocationsService.getPaymentAllocation(testNino, docNumber)) shouldBe Left(PaymentAllocationError)
      }
    }
  }
}
