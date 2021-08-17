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

package views.agent

import assets.FinancialDetailsTestConstants._
import assets.MessagesLookUp.{AgentPaymentDue, WhatYouOwe => whatYouOwe}
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.{FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.OutstandingChargesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.agent.WhatYouOwe

import java.time.LocalDate

class WhatYouOweViewSpec extends ViewSpec with FeatureSwitching with ImplicitDateFormatter {
  import Selectors.id

  class Setup(charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear) {

    val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

    val html: HtmlFormat.Appendable = whatYouOweView(charges, currentTaxYear,
      "testBackURL", Some("1234567890"))(messages)
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
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
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
    overduePaymentList = financialDetailsOverdueInterestData(latePaymentInterest).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPI(latePaymentInterest: List[Option[BigDecimal]],
                                   dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueWithLpi(latePaymentInterest, dunningLock).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),

  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(
    LocalDate.now().minusDays(15).toString, 0.00
  )
  val whatYouOweDataWithWithAciValueZeroAndOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithWithAciValueZeroAndFuturePayments: WhatYouOweChargesList = WhatYouOweChargesList(
    overduePaymentList = List(),
    dueInThirtyDaysList = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates(1)),
    futurePayments = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList()

  "The What you owe view with financial details model" when {
    "the user has charges and access viewer before 30 days of due date" should {
      s"have the title '${AgentPaymentDue.title}' and page header and notes" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {
        verifySelfAssessmentLink()
      }

      s"have the remaining balance title, table header " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue
      }
      s"remaining balance row data exists and should not contain hyperlink and overdue tag " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days()) {

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

      }

      s"table header and data for future payments" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("future-payments-heading").text shouldBe AgentPaymentDue.futurePayments
        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(45).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.doesNotHave(id("future-payments-type-0-overdue"))

        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        futurePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("future-payments-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.doesNotHave(id("future-payments-type-1-overdue"))

        pageDocument.doesNotHave(id("due-in-thirty-days-payments-heading"))
        pageDocument.doesNotHave(id("over-due-payments-heading"))
      }

      "when showing the Dunning Lock content" should {
        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(oneDunningLock)) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe AgentPaymentDue.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(noDunningLocks)) {
          pageDocument.doesNotHave(Selectors.id("payment-under-review-info"))
        }

        s"display ${AgentPaymentDue.paymentUnderReview} when there is a dunningLock against a single charge" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(oneDunningLock)) {
          val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1WithTaxYearAndUnderReview
          futurePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2WithTaxYear
        }

        s"display ${AgentPaymentDue.paymentUnderReview} when there is a dunningLock against multiple charges" in new Setup(
          whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks)) {
          val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1WithTaxYearAndUnderReview
          futurePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2WithTaxYearAndUnderReview
        }
      }

    }

    "the user has charges and access viewer within 30 days of due date" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }
      s"have the remaining balance header and table data" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
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

        pageDocument.doesNotHave(id("balancing-charge-type-overdue"))
      }

      s"have table header and data for due within 30 days" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.getElementById("due-in-thirty-days-payments-heading").text shouldBe AgentPaymentDue.dueInThirtyDays

        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))
      }
      s"have data with POA2 with hyperlink and no overdue" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-1-overdue"))
      }

      s"have payment details and should not contain future payments and overdue payment headers" in new Setup(whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.doesNotHave(id("future-payments-heading"))
        pageDocument.doesNotHave(id("over-due-payments-heading"))
      }

      "when showing the Dunning Lock content" should {
        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithDataDueIn30Days(oneDunningLock)) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithDataDueIn30Days(noDunningLocks)) {
          pageDocument.doesNotHave(id("payment-under-review-info"))
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
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }
      s"have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
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
      }

      "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2" in
        new Setup(whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {
          pageDocument.getElementById("over-due-payments-heading").text shouldBe AgentPaymentDue.overduePayments

          val overdueTableHeader: Element = pageDocument.select("tr").get(3)
          overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
          overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
            AgentPaymentDue.latePoa1Text + " " + AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

          pageDocument.getElementById("over-due-type-0-late-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
            LocalDate.now().getYear, "1040000124",latePaymentCharge = true).url
          pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
        }


      "have overdue payments header and data with POA1 charge type and No Late payment interest" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe AgentPaymentDue.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
          AgentPaymentDue.poa1Text + " " + AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
      }

      "have overdue payments header and data with POA1 charge type" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe AgentPaymentDue.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        /*
                overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        */
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " +
          AgentPaymentDue.poa1Text + " " + AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueLPI(List(None, None))) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-1-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have payments data with button" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.doesNotHave(id("future-payments-heading"))
        pageDocument.doesNotHave(id("due-in-thirty-days-payments-heading"))
      }

      "when showing the Dunning Lock content" should {
        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          whatYouOweDataWithOverdueData(oneDunningLock)) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe whatYouOwe.paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          whatYouOweDataWithOverdueData(noDunningLocks)) {
          pageDocument.doesNotHave(id("payment-under-review-info"))
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
    }

    "the user has charges and access viewer with mixed dates" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.title() shouldBe AgentPaymentDue.title
      }
      s"not have MTD payments heading" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.doesNotHave(id("pre-mtd-payments-heading"))
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueMixedData2(List(None,None,None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").first()
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(whatYouOweDataWithMixedData2) {
        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.getElementById("future-payments-heading").text shouldBe AgentPaymentDue.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("future-payments-type-0-overdue"))
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithMixedData1) {
        pageDocument.doesNotHave(id("pre-mtd-payments-heading"))
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }
      s"have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
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
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataTestActiveWithMixedData2(List(None,None,None,None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").get(2)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
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

      "have a paragraph explaining interest rates" in new Setup(whatYouOweDataWithOverdueInterestData(List(None, None))) {
        pageDocument.getElementsByClass("interest-rate").get(0).text() shouldBe whatYouOwe.interestRatesPara

        val expectedUrl = "https://www.gov.uk/government/publications/rates-and-allowances-hmrc-interest-rates-for-late-and-early-payments/rates-and-allowances-hmrc-interest-rates"
        pageDocument.getElementById("interest-rate-link").attr("href") shouldBe expectedUrl
      }

      "not have a paragraph explaining interest rates when there is no accruing interest" in new Setup(whatYouOweDataWithOverdueData()) {
        pageDocument.getOptionalSelector(".interest-rate") shouldBe None
      }


      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(5)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("due-in-thirty-days-type-0-overdue"))
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndFuturePayments) {
        pageDocument.getElementById("future-payments-heading").text shouldBe AgentPaymentDue.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(4)
        futurePaymentsHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(5)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.doesNotHave(id("future-payments-type-0-overdue"))
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

      "have note credit-on-account as a panel" in new Setup(noChargesModel) {
        pageDocument.getElementById("credit-on-account").classNames should contain allOf("panel", "panel-indent", "panel-border-wide")
      }

      "not have button Pay now" in new Setup(noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
    }
  }
}
