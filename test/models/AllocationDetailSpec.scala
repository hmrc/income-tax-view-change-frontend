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

import enums.ChargeType.{CGT, SL}
import models.paymentAllocations.AllocationDetail
import org.scalatest.Matchers
import testUtils.TestSupport

import java.time.LocalDate

class AllocationDetailSpec extends TestSupport with Matchers {

  private val localToDateOpt = Some(LocalDate.parse("2019-01-04"))

  def allocationDetails(mainType: String, chargeType: String, to: Option[LocalDate]): AllocationDetail = {
    AllocationDetail(Some("id"),
      localToDateOpt,
      to, Option(chargeType), Option(mainType), Some(10000.0), Some(5000.0), Some("chargeReference1"))
  }

  "AllocationDetail" when {

    "calling .getPaymentAllocationKeyInPaymentAllocations" should {

      "return a valid message" when {

        "provided with all subcharge types for POA1" in {
          allocationDetails("SA Payment on Account 1", "NIC4", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.nic4"
          allocationDetails("SA Payment on Account 1", "ITSA", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.incomeTax"
        }

        "provided with all subcharge types for POA2" in {
          allocationDetails("SA Payment on Account 2", "NIC4", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.nic4"
          allocationDetails("SA Payment on Account 2", "ITSA", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.incomeTax"
        }

        "provided with all subcharge types for a balancing charge" in {
          allocationDetails("SA Balancing Charge", "ITSA", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.incomeTax"
          allocationDetails("SA Balancing Charge", "NIC4", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic4"
          allocationDetails("SA Balancing Charge", "Voluntary NIC2", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.vcnic2"
          allocationDetails("SA Balancing Charge", "NIC2", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic2"
          allocationDetails("SA Balancing Charge", SL, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.sl"
          allocationDetails("SA Balancing Charge", CGT, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.cgt"

        }

      }

      "return an empty message" when {
        "mainType and/or chargeType is None" in {
          allocationDetails(null, "NIC4", localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
          allocationDetails("SA Payment on Account 1", null, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
          allocationDetails(null, null, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
        }
      }


    }

    "calling .getTaxYear" should {

      "determine the allocation tax year by the period end date in the model" in {
        def allocationDetailWithDateTo(taxPeriodEndDate: String): AllocationDetail = {
          AllocationDetail(Some("id"), Some(LocalDate.parse("2018-08-04")), to = Some(LocalDate.parse(taxPeriodEndDate)),
            Some("ITSA"), Some("SA Balancing Charge"), Some(10000.0), Some(5000.0), Some("chargeReference1"))
        }

        allocationDetailWithDateTo("2018-03-06").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-05").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-06").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-04-07").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-06-01").getTaxYear shouldBe 2019
      }

    }

    "calling .determineTaxYearFromPeriodEnd" should {
      "throw Exception" when {
        "periodEndDate is None" in {
          intercept[Exception] {
            allocationDetails("SA Payment on Account 1", "NIC4", None).getTaxYear
          }
        }
      }
    }

  }
}
