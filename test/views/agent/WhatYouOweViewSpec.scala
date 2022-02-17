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

package views.agent

import testConstants.BaseTestConstants.{testArn, testCredId, testUserTypeAgent}
import testConstants.FinancialDetailsTestConstants._
import testConstants.MessagesLookUp.{AgentPaymentDue, WhatYouOwe => whatYouOwe}
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetailsModel, WhatYouOweChargesList}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.outstandingCharges.OutstandingChargesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.WhatYouOwe
import java.time.LocalDate

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class WhatYouOweViewSpec extends ViewSpec with FeatureSwitching with ImplicitDateFormatter {
  import Selectors.id

  class Setup(charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear,
              migrationYear: Int = LocalDate.now().getYear - 1,
              codingOutEnabled: Boolean = true,
              displayTotals: Boolean = true,
              dunningLock: Boolean = false,
              hasLpiWithDunningBlock: Boolean = false) {

    val agentUser: MtdItUser[_] = MtdItUser(
      mtditid = "XAIT00000000015",
      nino = "AA111111A",
      userName = Some(Name(Some("Test"), Some("User"))),
      incomeSources = IncomeSourceDetailsModel("testMtdItId", Some(migrationYear.toString), List(), None),
      btaNavPartial =  None,
      saUtr = Some("1234567890"),
      credId = Some(testCredId),
      userType = Some(testUserTypeAgent),
      arn = Some(testArn)
    )(FakeRequest())

    val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

    val html: HtmlFormat.Appendable = whatYouOweView(
      chargesList = charges,
      hasLpiWithDunningBlock = hasLpiWithDunningBlock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      codingOutEnabled = codingOutEnabled,
      displayTotals = displayTotals,
      isAgent = true)(FakeRequest(), agentUser, implicitly)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("sa-note-migrated").selectFirst("a")
      anchor.text shouldBe AgentPaymentDue.saLink
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

  def financialDetailsOverdueWithLpi(latePaymentInterest: List[Option[BigDecimal]],
                                     dunningLock: List[Option[String]]): FinancialDetailsModel = testFinancialDetailsModelWithLPI(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    dunningLock = dunningLock,
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
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

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List()
  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(
    LocalDate.now().minusDays(15).toString, 0.00
  )
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

  val codingOutAmount = 444.23
  val whatYouOweDataWithCodingOut: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(testFinancialDetailsModelWithCodingOut().getAllDocumentDetailsWithDueDates.head),
    dueInThirtyDaysList = List(),
    futurePayments = List(),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(DocumentDetail(taxYear = "2021", transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
      documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(12.34),
      originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
      interestOutstandingAmount = None, interestRate = None,
      latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
      interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
      amountCodedOut = Some(codingOutAmount)))
  )

  val whatYouOweDataWithCodingOutFuture: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(),
    dueInThirtyDaysList = List(),
    futurePayments = List(testFinancialDetailsModelWithCodingOut().getAllDocumentDetailsWithDueDates.head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(DocumentDetail(taxYear = "2021", transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
      documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(12.34),
      originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
      interestOutstandingAmount = None, interestRate = None,
      latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
      interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
      amountCodedOut = Some(codingOutAmount)))
  )

  val whatYouOweDataWithCancelledPayeSa: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(testFinancialDetailsModelWithCancelledPayeSa().getAllDocumentDetailsWithDueDates.head),
    dueInThirtyDaysList = List(),
    futurePayments = List(),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00))

  "The What you owe view with financial details model" when {
    "the user has charges and access viewer before 30 days of due date" should {

      s"have the title '${AgentPaymentDue.title}'" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.title() shouldBe AgentPaymentDue.title

      }

      s"have the Balancing Payment title, table header " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue
      }
      s"Balancing Payment row data exists and should not contain hyperlink and overdue tag " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe AgentPaymentDue.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.doesNotHave(id("balancing-charge-type-overdue"))
      }
      s"payment type drop down and content exists" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe AgentPaymentDue.lpiHeading + " " + AgentPaymentDue.lpiLine1

      }
      s"payment processing bullet list exists" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }


      "when showing the Dunning Lock content" should {
        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(oneDunningLock), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe AgentPaymentDue.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(noDunningLocks), dunningLock = false) {
          pageDocument.doesNotHave(Selectors.id("payment-under-review-info"))
        }

      }

    }

    "the user has charges and access viewer within 30 days of due date" should {

      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.title() shouldBe AgentPaymentDue.title
      }
      s"have the Balancing Payment header and table data" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe AgentPaymentDue.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

      }
      s"have payment type drop down details" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe AgentPaymentDue.lpiHeading + " " + AgentPaymentDue.lpiLine1

        pageDocument.doesNotHave(id("balancing-charge-type-overdue"))
      }

      s"have table header and data for due within 30 days" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("payments-due").text shouldBe AgentPaymentDue.paymentsDue

        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        dueWithInThirtyDaysHeader.select("th").get(2).text() shouldBe AgentPaymentDue.taxYearSummary
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + s" $currentYear"
        dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))
        pageDocument.getElementById("taxYearSummary-30days-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
          LocalDate.now().getYear).url
      }
      s"have data with POA2 with hyperlink and no overdue" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2Text + s" $currentYear"
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-1-overdue"))
      }

      "when showing the Dunning Lock content" should {

        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithDataDueIn30Days(oneDunningLock), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithDataDueIn30Days(noDunningLocks), dunningLock = false) {
          pageDocument.doesNotHave(id("payment-under-review-info"))
        }

        s"payment processing bullet list exists when overdue and due within 30 days is there" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days(oneDunningLock), dunningLock = true) {
          pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
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
    }

    "the user has charges and access viewer after due date" should {

      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.title() shouldBe AgentPaymentDue.title

      }
      s"have the mtd payments header, table header and data with Balancing Payment data with no hyperlink but have overdue tag" in new Setup(
        whatYouOweDataWithOverdueData()) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        val interestTable: Element = pageDocument.select("tr").get(2)
        interestTable.select("td").first().text() shouldBe ""
        interestTable.select("td").get(1).text() shouldBe AgentPaymentDue.interestOnRemainingBalance + " " + whatYouOwe
          .interestOnRemainingBalanceYear(LocalDate.now().minusDays(30).toLongDateShort, LocalDate.now().toLongDateShort)
        interestTable.select("td").last().text() shouldBe "£12.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have payment type dropdown details" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1
        pageDocument.getElementById("payment-details-content-2").text shouldBe AgentPaymentDue.lpiHeading + " " + AgentPaymentDue.lpiLine1

      }

      s"payment processing bullet list exists when overdue charge is there" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }

      "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2" in
        new Setup(whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {
          pageDocument.getElementById("payments-due").text shouldBe AgentPaymentDue.paymentsDue

          val overdueTableHeader: Element = pageDocument.select("tr").get(3)
          overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
          overdueTableHeader.select("th").get(2).text() shouldBe AgentPaymentDue.taxYearSummary
          overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
            AgentPaymentDue.latePoa1Text + s" $currentYear"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

          pageDocument.getElementById("over-due-type-0-late-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
            LocalDate.now().getYear, "1040000124", latePaymentCharge = true).url
          pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
            LocalDate.now().getYear).url
        }

      s"payment processing bullet list exists when overdue lpi is there" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }


      "have overdue payments header and data with POA1 charge type and No Late payment interest" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("payments-due").text shouldBe AgentPaymentDue.paymentsDue

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").get(2).text() shouldBe AgentPaymentDue.taxYearSummary
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
          AgentPaymentDue.poa1Text + s" $currentYear"
        overduePaymentsTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag

        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
          LocalDate.now().getYear).url
      }

      "have overdue payments header and data with POA1 charge type" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("payments-due").text shouldBe AgentPaymentDue.paymentsDue

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").get(2).text() shouldBe AgentPaymentDue.taxYearSummary
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        /*
                overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        */
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
          AgentPaymentDue.poa1Text + s" $currentYear"
        overduePaymentsTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
          LocalDate.now().getYear).url
      }
      s"have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + s" $currentYear"
        overduePaymentsTableRow2.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-1-late-link2").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-1-overdue").text shouldBe AgentPaymentDue.overdueTag
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
          LocalDate.now().getYear).url

      }
      s"have payments data with button" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.doesNotHave(id("due-in-thirty-days-payments-heading"))
      }

      "when showing the Dunning Lock content" should {
        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithOverdueData(oneDunningLock), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithOverdueData(noDunningLocks), dunningLock = false) {
          pageDocument.doesNotHave(id("payment-under-review-info"))
        }

        s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against a single charge" in new Setup(
          whatYouOweDataWithOverdueLPI(List(None, None), oneDunningLock), dunningLock = true) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearOverdue
        }

          s"payment processing bullet list exists when single lpi charge is there" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None), oneDunningLock), dunningLock = true) {
            pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
            val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
            paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
            paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
            pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          }
        }

        s"display ${whatYouOwe.paymentUnderReview} when there is a dunningLock against multiple charges" in new Setup(
          whatYouOweDataWithOverdueLPI(List(None, None), twoDunningLocks), dunningLock = true) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2WithTaxYearOverdueAndUnderReview
        }
        s"payment processing bullet list exists when multiple charges are there" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None), twoDunningLocks), dunningLock = true) {
          pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }
      }
    }

    "the user has charges and access viewer with mixed dates" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.title() shouldBe AgentPaymentDue.title
      }
      s"not have MTD payments heading" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.doesNotHave(id("pre-mtd-payments-heading"))
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueMixedData2(List(None, None, None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").first()
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + s" $currentYear"
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + s" $currentYear"
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.doesNotHave(id("pre-mtd-payments-heading"))
      }
      s"payment processing bullet list exists when multiple data is present" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {

      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.title() shouldBe AgentPaymentDue.title
      }
      s"have the mtd payments header, table header and data with Balancing Payment data with no hyperlink but have overdue tag and payment processing is visible" in new Setup(
        whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(15).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe AgentPaymentDue.overdueTag

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }
      s"have overdue table header and data with hyperlink and overdue tag and payment processing" in new Setup(whatYouOweDataTestActiveWithMixedData2(List(None, None, None, None))) {
        val overdueTableHeader: Element = pageDocument.select("#payments-due-table thead tr").get(0)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").get(2).text() shouldBe AgentPaymentDue.taxYearSummary
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("#payments-due-table tbody tr").get(0)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + s" $currentYear"
        overduePaymentsTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("#payments-due-table tbody tr").get(1)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + s" $currentYear"
        dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe AgentPaymentDue.taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-late-link2").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))

        pageDocument.getElementById("taxYearSummary-30days-link-0").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(
          LocalDate.now().getYear).url

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
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

        Option(overduePaymentsInterestTableRow("0")) shouldBe None

        overduePaymentsInterestTableRow("1").select("td").get(1).text() shouldBe whatYouOwe.interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
        overduePaymentsInterestTableRow("1").select("td").last().text() shouldBe "£24.05"
      }

      s"payment processing bullet list exists when interest and no LPI is shown" in new Setup(whatYouOweDataWithOverdueInterestData(List(Some(34.56), None))) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }
      s"payment processing bullet list exists when when interest is displayed" in new Setup(whatYouOweDataWithOverdueInterestData(List(None, None))) {
        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }


    }

    "the user has no charges" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(noChargesModel) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("no-payments-due").text shouldBe AgentPaymentDue.noPaymentsDue
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote


      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(noChargesModel) {
        verifySelfAssessmentLink()
      }


      "not have button Pay now" in new Setup(noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }

      "should have payment processing bullets" in new Setup(noChargesModel) {

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }

    }

    "codingOut is enabled" should {
      "have coding out message displayed at the bottom of the page" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
      "have a class 2 Nics overdue entry" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("over-due-type-0") should not be null
        pageDocument.getElementById("over-due-type-0").text().contains("Class 2 National Insurance") shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1

      }

      "have a link to the SA summary coding out page" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/agents/tax-years/2021/charge?id=1040000125"
      }

      "should have payment processing bullets when coding out is taking place" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = true) {

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }


      "have a cancelled paye self assessment entry" in new Setup(whatYouOweDataWithCancelledPayeSa, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-notice") shouldBe null
        pageDocument.getElementById("over-due-type-0") should not be null
        pageDocument.getElementById("over-due-type-0").text().contains("Cancelled Self Assessment payment (through your PAYE tax code)") shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        pageDocument.getElementById("coding-out-summary-link") shouldBe null
      }
    }

    "codingOut is disabled" should {
      "have no coding out message displayed" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = false) {
        pageDocument.getElementById("coding-out-notice") shouldBe null
      }
      "have a balancing charge overdue entry" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = false) {
        pageDocument.getElementById("over-due-type-0") should not be null
        pageDocument.select("#over-due-type-0 a").get(0).text() shouldBe "Balancing payment 2021"
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
      }
      "should have payment processing bullets when coding out is disabled" in new Setup(whatYouOweDataWithCodingOut, codingOutEnabled = true) {

        pageDocument.getElementById("payments-made").text shouldBe AgentPaymentDue.paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe AgentPaymentDue.paymentprocessingbullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe AgentPaymentDue.paymentprocessingbullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }
    }

}
