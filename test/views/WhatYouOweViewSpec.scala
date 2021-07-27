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

package views

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.FinancialDetailsTestConstants._
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.MessagesLookUp.{WhatYouOwe => whatYouOwe}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.{FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargesModel, _}
import models.outstandingCharges._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.WhatYouOwe

import java.time.LocalDate

class WhatYouOweViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  class Setup(charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear
              ) {
    val html: HtmlFormat.Appendable = whatYouOweView(charges, currentTaxYear, "testBackURL", Some("1234567890"))(implicitly)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("sa-note-migrated").selectFirst("a")
      anchor.text shouldBe whatYouOwe.saLink
      anchor.attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      anchor.attr("target") shouldBe "_blank"
    }
  }

  def outstandingChargesModel(dueDate: String, aciAmount: BigDecimal = 12.67): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, aciAmount, 1234)))

  val financialDetailsDueInMoreThan30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
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
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )
  val outstandingChargesDueInMoreThan30Days: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().plusDays(35).toString)

  val whatYouOweDataWithDataDueInMoreThan30Days: WhatYouOweChargesList = WhatYouOweChargesList(
    futurePayments = financialDetailsDueInMoreThan30Days.getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  val financialDetailsDueIn30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
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
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )
  val outstandingChargesDueIn30Days: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().plusDays(30).toString)

  val whatYouOweDataWithDataDueIn30Days: WhatYouOweChargesList = WhatYouOweChargesList(
    dueInThirtyDaysList = financialDetailsDueIn30Days.getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  val financialDetailsOverdueData: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
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
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )

  def financialDetailsOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): FinancialDetailsModel = testFinancialDetailsModelWithInterest(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestOutstandingAmount = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueWithLpi(latePaymentInterest: List[Option[BigDecimal]]): FinancialDetailsModel = testFinancialDetailsModelWithLPI(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueNoLpi: FinancialDetailsModel = testFinancialDetailsModelWithNoLpi(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
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
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusDays(30).toString)

  val whatYouOweDataWithOverdueData: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueData.getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueInterestData(latePaymentInterest).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPI(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueWithLpi(latePaymentInterest).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  val whatYouOweDataWithOverdueNoLpi: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueNoLpi.getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  val financialDetailsWithMixedData: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().plusDays(30).toString)),
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
    outstandingAmount = List(Some(25), Some(50)),
    taxYear = LocalDate.now().getYear.toString
  )

  val whatYouOweDataWithMixedData: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(),
    dueInThirtyDaysList = List(financialDetailsWithMixedData.getAllDocumentDetailsWithDueDates(1)),
    futurePayments = List(financialDetailsWithMixedData.getAllDocumentDetailsWithDueDates.head)
  )

  val financialDetailsWithMixedData2: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    transactionId= Some("TransactionId"),
    transactionDate= Some("transactionDate"),
    `type`= Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some("NIC4 Wales"),
    dueDate = List(Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().minusDays(1).toString)),
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
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )

  val whatYouOweDataWithMixedData2: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List()
  )

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),

  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusDays(15).toString, 0.00)

  val whatYouOweDataWithWithAciValueZeroAndOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithWithAciValueZeroAndFuturePayments: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(),
    dueInThirtyDaysList = List(financialDetailsWithMixedData.getAllDocumentDetailsWithDueDates(1)),
    futurePayments = List(financialDetailsWithMixedData.getAllDocumentDetailsWithDueDates.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList()

  "The What you owe view with financial details model" when {
    "the user has charges and access viewer before 30 days of due date" should {
      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {
        verifySelfAssessmentLink()
      }
      s"have the remaining balance title, table header " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue
      }
      s"remaining balance row data exists and should not contain hyperlink and overdue tag " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }
      s"payment type drop down and content exists" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1

      }

      s"table header and data for future payments" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments
        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(45).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null

        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        futurePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("future-payments-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("future-payments-type-1-overdue") shouldBe null

        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer within 30 days of due date" should {
      s"have the remaining balance header and table data" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

      }
      s"have payment type drop down details" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1

        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }

      s"have table header and data for due within 30 days" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("due-in-thirty-days-payments-heading").text shouldBe whatYouOwe.dueInThirtyDays

        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have data with POA2 with hyperlink and no overdue" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("due-in-thirty-days-type-1-overdue") shouldBe null
      }

      s"have payment details and should not contain future payments and overdue payment headers" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer after due date" should {
      "have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
        whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        val interestTable: Element = pageDocument.select("tr").get(2)
        interestTable.select("td").first().text() shouldBe ""
        interestTable.select("td").get(1).text() shouldBe whatYouOwe.interestOnRemainingBalance + " " + whatYouOwe
          .interestOnRemainingBalanceYear(LocalDate.now().minusDays(30).toLongDateShort, LocalDate.now().toLongDateShort)
        interestTable.select("td").last().text() shouldBe "£12.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe whatYouOwe.overdueTag
      }
      "have payment type dropdown details" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1
      }


      "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2" in
        new Setup(whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " +
          whatYouOwe.latePoa1Text + " " + whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

        pageDocument.getElementById("over-due-type-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124",latePaymentCharge = true).url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }


      "have overdue payments header and data with POA1 charge type and No Late payment interest" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " +
          whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }

      "have overdue payments header and data with POA1 charge type" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
/*
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
*/
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " +
          whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      "have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-1-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-1-overdue").text shouldBe whatYouOwe.overdueTag
      }

      "have accruing interest displayed below each overdue POA" in new Setup(whatYouOweDataWithOverdueInterestData(List(None, None))) {
        def overduePaymentsInterestTableRow(index: String): Element = pageDocument.getElementById(s"overdue-charge-interest-$index")
        overduePaymentsInterestTableRow("0").select("td").get(1).text() shouldBe whatYouOwe.interestFromToDate("25 May 2019", "25 Jun 2019", "2.6")
        overduePaymentsInterestTableRow("0").select("td").last().text() shouldBe "£42.50"

        overduePaymentsInterestTableRow("1").select("td").get(1).text() shouldBe whatYouOwe.interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
        overduePaymentsInterestTableRow("1").select("td").last().text() shouldBe "£24.05"
      }

      "only show interest for POA when there is no late Payment Interest" in new Setup(whatYouOweDataWithOverdueInterestData(List(Some(34.56), None))) {
        def overduePaymentsInterestTableRow(index: String): Element = pageDocument.getElementById(s"overdue-charge-interest-$index")
        overduePaymentsInterestTableRow("0") shouldBe null

        overduePaymentsInterestTableRow("1").select("td").get(1).text() shouldBe whatYouOwe.interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
        overduePaymentsInterestTableRow("1").select("td").last().text() shouldBe "£24.05"
      }

      "have a paragraph explaining interest rates" in new Setup(whatYouOweDataWithOverdueInterestData(List(None, None))) {
        pageDocument.getElementsByClass("interest-rate").get(0).text() shouldBe whatYouOwe.interestRatesPara

        val expectedUrl = "https://www.gov.uk/government/publications/rates-and-allowances-hmrc-interest-rates-for-late-and-early-payments/rates-and-allowances-hmrc-interest-rates"
        pageDocument.getElementById("interest-rate-link").attr("href") shouldBe expectedUrl
      }

      "not have a paragraph explaining interest rates when there is no accruing interest" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.select(".interest-rate").first() shouldBe null
      }

      "have payments data with button" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer with mixed dates" should {
      s"have the title '${whatYouOwe.title}' and notes" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.title() shouldBe whatYouOwe.title
      }
      s"not have MTD payments heading" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }

      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueMixedData2(List(None,None,None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").first()
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(whatYouOweDataWithMixedData2) {
        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {
      s"have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
        whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(15).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataTestActiveWithMixedData2(List(None,None,None,None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").get(2)
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(5)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndFuturePayments) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(4)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(5)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " +
          whatYouOwe.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

      }

    }

    "the user has no charges" should {
      s"have the title '${whatYouOwe.title}' and page header and notes" in new Setup(noChargesModel) {
        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.selectFirst("header > h1").text shouldBe whatYouOwe.heading

        pageDocument.getElementById("no-payments-due").text shouldBe whatYouOwe.noPaymentsDue
        pageDocument.getElementById("sa-note-migrated").text shouldBe whatYouOwe.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe whatYouOwe.osChargesNote
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(noChargesModel) {
        verifySelfAssessmentLink()
      }

      "have note credit-on-account as a panel" in new Setup(noChargesModel) {
        pageDocument.getElementById("credit-on-account").classNames should contain allOf("panel", "panel-indent", "panel-border-wide")
      }

      "not have button Pay now" in new Setup(noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
    }

  }
}
