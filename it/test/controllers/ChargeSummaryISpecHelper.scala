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

package controllers

import enums.ChargeType.{ITSA_ENGLAND_AND_NI, ITSA_NI, NIC4_SCOTLAND}
import models.chargeHistory.ChargeHistoryModel
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails.{ChargeItem, FinancialDetail, MfaDebitCharge}
import models.incomeSourceDetails.TaxYear
import play.api.libs.json.Json
import services.DateService
import testConstants.BaseIntegrationTestConstants.testTaxYear
import testConstants.FinancialDetailsIntegrationTestConstants.financialDetailModelPartial

import java.time.{LocalDate, LocalDateTime, LocalTime}

trait ChargeSummaryISpecHelper extends ControllerISpecHelper {

  override implicit val dateService: DateService = app.injector.instanceOf[DateService]

  val paymentAllocation: List[PaymentHistoryAllocations] = List(
    paymentsWithCharge("SA Balancing Charge", ITSA_NI, dateService.getCurrentDate.plusDays(20).toString, -10000.0),
    paymentsWithCharge("SA Payment on Account 1", NIC4_SCOTLAND, dateService.getCurrentDate.plusDays(20).toString, -9000.0),
    paymentsWithCharge("SA Payment on Account 2", NIC4_SCOTLAND, dateService.getCurrentDate.plusDays(20).toString, -8000.0)
  )
  val chargeHistories: List[ChargeHistoryModel] = List(ChargeHistoryModel("2019", "1040000124", LocalDate.of(2018, 2, 14), "ITSA- POA 1", 2500,
    LocalDateTime.of(LocalDate.of(2019, 2, 14), LocalTime.of(9, 30, 45)), "Customer Request", Some("001")))
  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetailModelPartial(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, mainType = "SA Balancing Charge", dunningLock = Some("Dunning Lock"), interestLock = Some("Interest Lock")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, dunningLock = Some("Stand over order"), interestLock = Some("Breathing Space Moratorium Act")),
    financialDetailModelPartial(originalAmount = 123.45, chargeType = NIC4_SCOTLAND, mainType = "SA Payment on Account 2", dunningLock = Some("Dunning Lock"), interestLock = Some("Manual RPI Signal")))
  val important: String = s"${messagesAPI("chargeSummary.dunning.locks.banner.title")}"
  val paymentHistory: String = messagesAPI("chargeSummary.chargeHistory.heading")
  val lpiHistory: String = messagesAPI("chargeSummary.chargeHistory.lateInterestPayment")

  def paymentsWithCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(PaymentHistoryAllocation(
        amount = Some(amount),
        dueDate = Some(LocalDate.parse(date)),
        clearingSAPDocument = Some("012345678901"), clearingId = Some("012345678901"),
        taxYear = None)),
      chargeMainType = Some(mainType), chargeType = Some(chargeType))

  val financialDetailsUnpaidMFA = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> testTaxYear,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 1200.00,
        "originalAmount" -> 1200.00,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> "2018-03-30",
        "documentDueDate" -> "2018-03-30"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> s"$testTaxYear",
        "mainType" -> "ITSA Manual Penalty Pre CY-4",
        "mainTransaction" -> "4002",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> 1200.00,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("subItem" -> "001",
            "amount" -> 10000,
            "dueDate" -> "2018-03-30"))
      )
    )
  )

  val financialDetailsPaidMFA = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> testTaxYear,
        "transactionId" -> "1",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 0,
        "originalAmount" -> 1200.00,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> "2018-03-30",
        "documentDueDate" -> "2018-03-30"
      ),
      Json.obj(
        "taxYear" -> testTaxYear,
        "transactionId" -> "2",
        "documentDate" -> "2022-04-06",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 0,
        "originalAmount" -> 1200.00,
        "documentText" -> "documentText",
        "documentDueDate" -> "2021-04-15",
        "formBundleNumber" -> "88888888",
        "statisticalFlag" -> false,
        "paymentLot" -> "MA999991A",
        "paymentLotItem" -> "5",
        "effectiveDateOfPayment" -> "2018-03-30"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> s"$testTaxYear",
        "mainType" -> "ITSA Manual Penalty Pre CY-4",
        "mainTransaction" -> "4002",
        "transactionId" -> "1",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> 1200.00,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("subItem" -> "001",
            "amount" -> 1200,
            "dueDate" -> "2018-03-30"),
          Json.obj(
            "subItem" -> "002",
            "dueDate" -> "2022-07-28",
            "clearingDate" -> "2022-07-28",
            "amount" -> 1200,
            "paymentReference" -> "GF235687",
            "paymentAmount" -> 1200,
            "paymentMethod" -> "Payment",
            "clearingSAPDocument" -> "012345678912"
          )
        )
      ),
      Json.obj(
        "taxYear" -> s"$testTaxYear",
        "mainType" -> "Payment on Account",
        "transactionId" -> "2",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> 1200.00,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("subItem" -> "001",
            "amount" -> 1200,
            "dueDate" -> "2018-03-30"),
          Json.obj(
            "subItem" -> "002",
            "dueDate" -> "2022-07-28",
            "clearingDate" -> "2022-07-28",
            "amount" -> 1200,
            "paymentReference" -> "GF235687",
            "paymentAmount" -> 1200,
            "paymentMethod" -> "Payment",
            "paymentLot" -> "MA999991A",
            "paymentLotItem" -> "5",
            "clearingSAPDocument" -> "012345678912"
          )
        )
      )
    )
  )

  val chargeItemUnpaid: ChargeItem = ChargeItem(
    transactionId = "1040000124",
    taxYear = TaxYear.forYearEnd(2018),
    transactionType = MfaDebitCharge,
    codedOutStatus = None,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(LocalDate.parse("2018-03-30")),
    originalAmount = 1200,
    outstandingAmount = 1200,
    interestOutstandingAmount = None,
    accruingInterestAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    interestRate = None,
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  val chargeItemPaid = chargeItemUnpaid.copy(outstandingAmount = 0)


}
