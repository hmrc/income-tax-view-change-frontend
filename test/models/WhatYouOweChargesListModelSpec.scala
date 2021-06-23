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

package models

import assets.FinancialDetailsTestConstants.testFinancialDetailsModel
import models.financialDetails.{FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

import java.time.LocalDate

class WhatYouOweChargesListModelSpec extends UnitSpec with Matchers {

  def outstandingChargesModel(dueDate: String): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val financialDetailsDueInMoreThan30Days: FinancialDetailsModel = testFinancialDetailsModel(
    List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("POA1"),
    List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some("clearingDate"),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount =  Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    List(Some(50), Some(75)),
    LocalDate.now().getYear.toString
  )

  val financialDetailsDueIn30Days: FinancialDetailsModel = testFinancialDetailsModel(
    List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("POA1"),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some("clearingDate"),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount =  Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    List(Some(50), Some(75)),
    LocalDate.now().getYear.toString
  )

  val financialDetailsOverdueData: FinancialDetailsModel = testFinancialDetailsModel(
    List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("POA1"),
    List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some("clearingDate"),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount =  Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    List(Some(50), Some(75)),
    LocalDate.now().getYear.toString
  )

  val outstandingCharges: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusMonths(13).toString)

  val whatYouOweAllData: WhatYouOweChargesList = WhatYouOweChargesList(
    dueInThirtyDaysList = financialDetailsDueIn30Days.getAllDocumentDetailsWithDueDates,
    futurePayments = financialDetailsDueInMoreThan30Days.getAllDocumentDetailsWithDueDates,
    overduePaymentList = financialDetailsOverdueData.getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingCharges)
  )

  val whatYouOweFinancialDataWithoutOutstandingCharges: WhatYouOweChargesList = WhatYouOweChargesList(
    dueInThirtyDaysList = financialDetailsDueIn30Days.getAllDocumentDetailsWithDueDates,
    futurePayments = financialDetailsDueInMoreThan30Days.getAllDocumentDetailsWithDueDates,
    overduePaymentList = financialDetailsOverdueData.getAllDocumentDetailsWithDueDates
  )


  "The WhatYouOweChargesList model" when {

    "all values in model exists with tie breaker matching in OutstandingCharges Model" should {
      "bcdChargeTypeDefinedAndGreaterThanZero is true" in {
        whatYouOweAllData.bcdChargeTypeDefinedAndGreaterThanZero shouldBe true
      }
      "isChargesListEmpty is false" in {
        whatYouOweAllData.isChargesListEmpty shouldBe false
      }
      "getEarliestTaxYearAndAmountByDueDate should have correct values" in {
        whatYouOweAllData.getEarliestTaxYearAndAmountByDueDate._1 shouldBe LocalDate.now().minusMonths(13).getYear
        whatYouOweAllData.getEarliestTaxYearAndAmountByDueDate._2 shouldBe 123456.67
      }
    }

    "all values in model exists except outstanding charges" should {
      "bcdChargeTypeDefinedAndGreaterThanZero is false" in {
        whatYouOweFinancialDataWithoutOutstandingCharges.bcdChargeTypeDefinedAndGreaterThanZero shouldBe false
      }
      "isChargesListEmpty is false" in {
        whatYouOweFinancialDataWithoutOutstandingCharges.isChargesListEmpty shouldBe false
      }
      "getEarliestTaxYearAndAmountByDueDate should have correct values" in {
        whatYouOweFinancialDataWithoutOutstandingCharges
          .getEarliestTaxYearAndAmountByDueDate._1 shouldBe LocalDate.now().minusDays(10).getYear
        whatYouOweFinancialDataWithoutOutstandingCharges.getEarliestTaxYearAndAmountByDueDate._2 shouldBe 50.0
      }
    }
  }
}
