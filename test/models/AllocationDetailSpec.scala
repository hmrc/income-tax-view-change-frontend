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

package models

import models.paymentAllocations.AllocationDetail
import org.scalatest.Matchers
import testUtils.TestSupport

class AllocationDetailSpec extends TestSupport with Matchers {

  def allocationDetails(mainType: String, chargeType: String): AllocationDetail = {
    AllocationDetail(Some("id"), Some("2018-08-04"), Some("2019-01-04"), Some(chargeType), Some(mainType), Some(10000.0), Some(5000.0), Some("chargeReference1"))
  }

  "AllocationDetail" when {

    "calling .getPaymentAllocationKeyInPaymentAllocations" should {

      "return a valid message" when {

        "provided with all subcharge types for POA1" in {
          allocationDetails("SA Payment on Account 1", "NIC4").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.nic4"
          allocationDetails("SA Payment on Account 1", "ITSA").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.incomeTax"
        }

        "provided with all subcharge types for POA2" in {
          allocationDetails("SA Payment on Account 2", "NIC4").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.nic4"
          allocationDetails("SA Payment on Account 2", "ITSA").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.incomeTax"
        }

        "provided with all subcharge types for a balancing charge" in {
          allocationDetails("SA Balancing Charge", "ITSA").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.incomeTax"
          allocationDetails("SA Balancing Charge", "NIC4").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic4"
          allocationDetails("SA Balancing Charge", "Voluntary NIC2").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.vcnic2"
          allocationDetails("SA Balancing Charge", "NIC2").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic2"
          allocationDetails("SA Balancing Charge", "SL").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.sl"
          allocationDetails("SA Balancing Charge", "CGT").getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.cgt"

        }
      }
    }

    "calling .getTaxYear" should {

      "determine the allocation tax year by the period end date in the model" in {
        def allocationDetailWithDateTo(taxPeriodEndDate: String): AllocationDetail = {
          AllocationDetail(Some("id"), Some("2018-08-04"), to = Some(taxPeriodEndDate),
            Some("ITSA"), Some("SA Balancing Charge"), Some(10000.0), Some(5000.0), Some("chargeReference1"))
        }

        allocationDetailWithDateTo("2018-03-06").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-05").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-06").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-04-07").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-06-01").getTaxYear shouldBe 2019
      }

    }
  }
}
