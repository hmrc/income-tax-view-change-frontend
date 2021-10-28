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

import testConstants.BaseTestConstants.{testCredId, testMtditid, testNino, testRetrievedUserName, testSaUtr, testUserTypeIndividual}
import testConstants.FinancialDetailsTestConstants._
import testConstants.MessagesLookUp.{WhatYouOwe => whatYouOwe}
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.outstandingCharges._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.WhatYouOwe
import java.time.LocalDate

import play.api.test.FakeRequest
import testConstants.MessagesLookUp.WhatYouOwe.paymentUnderReview

class WhatYouOweViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter {

  val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

  class Setup(charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear,
              hasLpiWithDunningBlock: Boolean = false,
              dunningLock: Boolean = false,
              migrationYear: Int = LocalDate.now().getYear - 1
              ) {
    val individualUser: MtdItUser[_] = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(testRetrievedUserName),
      incomeSources = IncomeSourceDetailsModel("testMtdItId", Some(migrationYear.toString), List(), None),
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = Some(testUserTypeIndividual),
      arn = None
    )(FakeRequest())
    val html: HtmlFormat.Appendable = whatYouOweView(charges,hasLpiWithDunningBlock, currentTaxYear, "testBackURL", Some("1234567890"), dunningLock)(FakeRequest(),individualUser, implicitly)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("payments-due-note").selectFirst("a")
      anchor.text shouldBe whatYouOwe.saLink
      anchor.attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      anchor.attr("target") shouldBe "_blank"
    }
  }

  def financialDetailsOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): FinancialDetailsModel = testFinancialDetailsModelWithInterest(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestOutstandingAmount = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueWithLpi(latePaymentInterest: List[Option[BigDecimal]], dunningLock: List[Option[String]]): FinancialDetailsModel = testFinancialDetailsModelWithLPI(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    dunningLock = dunningLock,
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueWithLpiDunningBlock(latePaymentInterest: Option[BigDecimal],lpiWithDunningBlock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLPIDunningLock(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest,
      lpiWithDunningBlock = lpiWithDunningBlock
  )

  def financialDetailsOverdueWithLpiDunningBlockZero(latePaymentInterest: Option[BigDecimal],lpiWithDunningBlock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLpiDunningLockZero(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest,
    lpiWithDunningBlock = lpiWithDunningBlock
  )

  def whatYouOweDataWithOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = financialDetailsOverdueInterestData(latePaymentInterest).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPI(latePaymentInterest: List[Option[BigDecimal]],
                                   dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = financialDetailsOverdueWithLpi(latePaymentInterest, dunningLock).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPIDunningBlock(latePaymentInterest: Option[BigDecimal],
                                               lpiWithDunningBlock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = financialDetailsOverdueWithLpiDunningBlock(latePaymentInterest,lpiWithDunningBlock).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPIDunningBlockZero(latePaymentInterest: Option[BigDecimal],
                                               lpiWithDunningBlock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = financialDetailsOverdueWithLpiDunningBlockZero(latePaymentInterest,lpiWithDunningBlock).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),

  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusDays(15).toString, 0.00)

  val whatYouOweDataWithWithAciValueZeroAndOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithWithAciValueZeroAndFuturePayments: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(),
    dueInThirtyDaysList = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates(1)),
    futurePayments = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00))

  val noUtrModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00))

  "The What you owe view with financial details model" when {
    "the user has charges and access viewer before 30 days of due date" should {
      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        verifySelfAssessmentLink()
      }
      "have Overdue amount and Total amount displayed " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("overdueAmount").select("p").get(0).text shouldBe whatYouOwe.overduePaymentsDue
        pageDocument.getElementById("overdueAmount").select("p").get(1).text shouldBe "£2.00"
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance").select("p").get(0).text shouldBe whatYouOwe.totalPaymentsDue
        pageDocument.getElementById("totalBalance").select("p").get(1).text shouldBe "£2.00"
      }
      "not display totals at the top if its first year of migration" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days(),
        migrationYear = LocalDate.now().getYear) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
      "have the remaining balance title, table header " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue
      }
      "remaining balance row data exists and should not contain hyperlink and overdue tag " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }
      "payment type drop down and content exists" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe whatYouOwe.lpiHeading + " " + whatYouOwe.lpiLine1

      }

      "table header and data for future payments" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days(noDunningLocks)) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments
        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(45).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYear
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null

        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        futurePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYear
        futurePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("future-payments-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("future-payments-type-1-overdue") shouldBe null


        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
      "display the paragraph about payments under review when there is a dunningLock" in new Setup(
        whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks), dunningLock = true) {
        val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

        pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
        paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
        paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
      }

      "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
        whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks)) {
        pageDocument.getElementById("payment-under-review-info") shouldBe null
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against a single charge" in new Setup(
        whatYouOweDataWithDataDueInMoreThan30Days(oneDunningLock)) {
        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearAndUnderReview
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYear
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against multiple charges" in new Setup(
        whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks)) {
        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearAndUnderReview
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearAndUnderReview
      }
    }

    "the user has charges and access viewer within 30 days of due date" should {
      "have BalanceDueWithin30Days amount and Total amount displayed " in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(0).text shouldBe whatYouOwe.dueInThirtyDays
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(1).text shouldBe "£1.00"
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("totalBalance").select("p").get(0).text shouldBe whatYouOwe.totalPaymentsDue
        pageDocument.getElementById("totalBalance").select("p").get(1).text shouldBe "£1.00"
      }
      "not display totals at the top if its first year of migration" in new Setup(whatYouOweDataWithDataDueIn30Days(),
        migrationYear = LocalDate.now().getYear) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
      s"have the remaining balance header and table data" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
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
      "have payment type drop down details" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe whatYouOwe.lpiHeading + " " + whatYouOwe.lpiLine1


        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }

      "have table header and data for due within 30 days" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("due-in-thirty-days-payments-heading").text shouldBe whatYouOwe.dueInThirtyDays

        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYear
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      "have data with POA2 with hyperlink and no overdue" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYear
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("due-in-thirty-days-type-1-overdue") shouldBe null
      }

      "have payment details and should not contain future payments and overdue payment headers" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }

      "display the paragraph about payments under review when there is a dunningLock" in new Setup(
        whatYouOweDataWithDataDueIn30Days(twoDunningLocks), dunningLock = true) {
        val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

        pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
        paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
        paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
      }

      "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
        whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {
        pageDocument.getElementById("payment-under-review-info") shouldBe null
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against a single charge" in new Setup(
        whatYouOweDataWithDataDueIn30Days(oneDunningLock)) {
        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)

        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearAndUnderReview
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYear
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against multiple charges" in new Setup(
        whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {
        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)

        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearAndUnderReview
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearAndUnderReview
      }
    }

    "the user has charges and access viewer after due date" should {
      "have Overdue amount and Total amount displayed " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("overdueAmount").select("p").get(0).text shouldBe whatYouOwe.overduePaymentsDue
        pageDocument.getElementById("overdueAmount").select("p").get(1).text shouldBe "£2.00"
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance").select("p").get(0).text shouldBe whatYouOwe.totalPaymentsDue
        pageDocument.getElementById("totalBalance").select("p").get(1).text shouldBe "£2.00"
      }
      "not display totals at the top if its first year of migration" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days(),
        migrationYear = LocalDate.now().getYear) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
      "have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
        whatYouOweDataWithOverdueData()) {
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
      "have payment type dropdown details" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe whatYouOwe.lpiHeading + " " + whatYouOwe.lpiLine1

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
          whatYouOwe.latePoa1Text + s" $currentYear " + whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

        pageDocument.getElementById("over-due-type-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124",latePaymentCharge = true).url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }

      "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - LPI Dunning Block" in
        new Setup(whatYouOweDataWithOverdueLPIDunningBlock(Some(34.56),Some(1000))) {
          pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

          val overdueTableHeader: Element = pageDocument.select("tr").get(3)
          overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
          overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " +
            whatYouOwe.latePoa1Text + s" $currentYear " + whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)  + " " + paymentUnderReview
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

          pageDocument.getElementById("over-due-type-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
            LocalDate.now().getYear, "1040000124",latePaymentCharge = true).url
          pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
          pageDocument.getElementById("LpiDunningBlock").text shouldBe "Payment under review"
        }

      "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - No LPI Dunning Block" in
        new Setup(whatYouOweDataWithOverdueLPIDunningBlockZero(Some(34.56),Some(0))) {
          pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

          val overdueTableHeader: Element = pageDocument.select("tr").get(3)
          overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
          overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " +
            whatYouOwe.latePoa1Text + s" $currentYear " + whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
          whatYouOwe.poa1Text + s" $currentYear " + whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
          whatYouOwe.poa1Text + s" $currentYear " + whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      "have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
        pageDocument.getElementById("interest-rate-link").text().contains("Any overdue payment interest")
        val expectedUrl = "https://www.gov.uk/government/publications/rates-and-allowances-hmrc-interest-rates-for-late-and-early-payments/rates-and-allowances-hmrc-interest-rates"
        pageDocument.getElementById("interest-rate-link").attr("href") shouldBe expectedUrl
      }

      "not have a paragraph explaining interest rates when there is no accruing interest" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.select(".interest-rate").first() shouldBe null
      }

      "have payments data with button" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
      }

      "display the paragraph about payments under review when there is a dunningLock" in new Setup(
        whatYouOweDataWithOverdueData(twoDunningLocks), dunningLock = true) {
        val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

        pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
        paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
        paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
      }

      "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
        whatYouOweDataWithOverdueData(twoDunningLocks)) {
        pageDocument.getElementById("payment-under-review-info") shouldBe null
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against a single charge" in new Setup(
        whatYouOweDataWithOverdueLPI(List(None, None), oneDunningLock)) {
        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)

        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearOverdueAndUnderReview
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearOverdue
      }

      s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against multiple charges" in new Setup(
        whatYouOweDataWithOverdueLPI(List(None, None), twoDunningLocks)) {
        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)

        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearOverdueAndUnderReview
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearOverdueAndUnderReview
      }
    }

    "the user has charges and access viewer with mixed dates" should {
      s"have the title '${whatYouOwe.title}' and notes" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.title() shouldBe whatYouOwe.title
      }
      s"not have MTD payments heading" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
      "have Overdue amount, Due within 30 days and Total amount displayed " in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("overdueAmount").select("p").get(0).text shouldBe whatYouOwe.overduePaymentsDue
        pageDocument.getElementById("overdueAmount").select("p").get(1).text shouldBe "£2.00"
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(0).text shouldBe whatYouOwe.dueInThirtyDays
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(1).text shouldBe "£1.00"
        pageDocument.getElementById("totalBalance").select("p").get(0).text shouldBe whatYouOwe.totalPaymentsDue
        pageDocument.getElementById("totalBalance").select("p").get(1).text shouldBe "£3.00"
      }
      "not display totals at the top if its first year of migration" in new Setup(whatYouOweDataWithMixedData1,
        migrationYear = LocalDate.now().getYear) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueMixedData2(List(None,None,None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").first()
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {
      "have Overdue amount and Total amount displayed " in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(0).text shouldBe whatYouOwe.dueInThirtyDays
        pageDocument.getElementById("balanceDueWithin30Days").select("p").get(1).text shouldBe "£1.00"
        pageDocument.getElementById("overdueAmount").select("p").get(0).text shouldBe whatYouOwe.overduePaymentsDue
        pageDocument.getElementById("overdueAmount").select("p").get(1).text shouldBe "£2.00"
        pageDocument.getElementById("totalBalance").select("p").get(0).text shouldBe whatYouOwe.totalPaymentsDue
        pageDocument.getElementById("totalBalance").select("p").get(1).text shouldBe "£3.00"
      }
      "not display totals at the top if its first year of migration" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue,
        migrationYear = LocalDate.now().getYear) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
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
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + s" $currentYear " +
          whatYouOwe.taxYearForChargesText(currentYearMinusOne, currentYear)
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
      "have no totals displayed " in new Setup(noChargesModel) {
        pageDocument.getElementById("overdueAmount") shouldBe null
        pageDocument.getElementById("balanceDueWithin30Days") shouldBe null
        pageDocument.getElementById("totalBalance") shouldBe null
      }
      s"have the title '${whatYouOwe.title}' and page header and notes" in new Setup(noChargesModel) {
        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.selectFirst("h1").text shouldBe whatYouOwe.heading
        pageDocument.getElementById("no-payments-due").text shouldBe whatYouOwe.noPaymentsDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(whatYouOwe.saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe whatYouOwe.osChargesNote
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(noChargesModel) {
        verifySelfAssessmentLink()
      }

      "have note credit-on-account as a panel" in new Setup(noChargesModel) {
        pageDocument.getElementById("credit-on-account").className().contains("govuk-insert-text")
      }

      "not have button Pay now" in new Setup(noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
    }
  }
}
