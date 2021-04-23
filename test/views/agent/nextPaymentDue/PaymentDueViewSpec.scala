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

package views.agent.nextPaymentDue

import java.time.LocalDate

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.FinancialDetailsTestConstants.{testFinancialDetailsModel, testFinancialDetailsModelWithChargesOfSameType}
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.MessagesLookUp.{AgentPaymentDue, WhatYouOwe => whatYouOwe}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.WhatYouOweChargesList
import models.outstandingCharges.{OutstandingChargesModel, _}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.agent.nextPaymentDue.paymentDue

class PaymentDueViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  class Setup(charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear,
              paymentEnabled: Boolean = true) {

    val agentPaymentDue: paymentDue = app.injector.instanceOf[paymentDue]

    val html: HtmlFormat.Appendable = agentPaymentDue(charges, currentTaxYear,
      paymentEnabled, mockImplicitDateFormatter, "testBackURL", Some("1234567890"))(FakeRequest(),implicitly, mockAppConfig)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("sa-note-migrated").selectFirst("a")
      anchor.text shouldBe AgentPaymentDue.saLink
      anchor.attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      anchor.attr("target") shouldBe "_blank"
    }
  }

  def outstandingChargesModel(dueDate: String, aciAmount: BigDecimal = 12.67) = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, aciAmount, 1234)))

  val financialDetailsDueInMoreThan30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesDueInMoreThan30Days = outstandingChargesModel(LocalDate.now().plusDays(35).toString)
  val whatYouOweDataWithDataDueInMoreThan30Days = WhatYouOweChargesList(futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days))

  val financialDetailsDueIn30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesDueIn30Days = outstandingChargesModel(LocalDate.now().plusDays(30).toString)
  val whatYouOweDataWithDataDueIn30Days = WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    outstandingChargesModel = Some(outstandingChargesDueIn30Days))

  val financialDetailsOverdueData = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesOverdueData = outstandingChargesModel(LocalDate.now().minusDays(30).toString)
  val whatYouOweDataWithOverdueData = WhatYouOweChargesList(overduePaymentList = financialDetailsOverdueData.financialDetails,
    outstandingChargesModel = Some(outstandingChargesOverdueData))

  val financialDetailsWithMixedData = testFinancialDetailsModelWithChargesOfSameType(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(25), Some(50), Some(75)), LocalDate.now().getYear.toString)
  val whatYouOweDataWithMixedData = WhatYouOweChargesList(overduePaymentList = List(financialDetailsWithMixedData.financialDetails(2)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData.financialDetails(1)), futurePayments = List(financialDetailsWithMixedData.financialDetails(0)))

  val outstandingChargesWithAciValueZeroAndOverdue = outstandingChargesModel(LocalDate.now().minusDays(15).toString, 0.00)
  val whatYouOweDataWithWithAciValueZeroAndOverdue = WhatYouOweChargesList(overduePaymentList = List(financialDetailsWithMixedData.financialDetails(2)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData.financialDetails(1)), futurePayments = List(financialDetailsWithMixedData.financialDetails(0)),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val noChargesModel = WhatYouOweChargesList()

  "The What you owe view with financial details model" when {
    "the user has charges and access viewer before 30 days of due date" should {
      s"have the title '${AgentPaymentDue.title}' and page header and notes" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {
        verifySelfAssessmentLink()
      }

      s"have the remaining balance title, table header " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe AgentPaymentDue.preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue
      }
      s"remaining balance row data exists and should not contain hyperlink and overdue tag " in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe AgentPaymentDue.remainingBalance
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }
      s"payment type drop down and content exists" in new Setup(whatYouOweDataWithDataDueInMoreThan30Days) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1

      }

      s"table header and data for future payments" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days) {
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
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null

        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        futurePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("future-payments-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("future-payments-type-1-overdue") shouldBe null

        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe AgentPaymentDue.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.agent.routes.PaymentController.paymentHandoff(12345667).url

        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer within 30 days of due date" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }
      s"have the remaining balance header and table data" in new Setup(whatYouOweDataWithDataDueIn30Days) {
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
      s"have payment type drop down details" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1

        pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
      }

      s"have table header and data for due within 30 days" in new Setup(whatYouOweDataWithDataDueIn30Days) {
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
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have data with POA2 with hyperlink and no overdue" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("due-in-thirty-days-type-1-overdue") shouldBe null
      }

      s"have payment details and should not contain future payments and overdue payment headers" in new Setup(whatYouOweDataWithDataDueIn30Days) {
        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe AgentPaymentDue.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.agent.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer after due date" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
      }
      s"have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
        whatYouOweDataWithOverdueData) {
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
      s"have payment type dropdown details" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe AgentPaymentDue.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe AgentPaymentDue.remainingBalance + " " + AgentPaymentDue.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe AgentPaymentDue.poaHeading + " " + AgentPaymentDue.poaLine1
      }

      s"have overdue payments header and data with POA1 charge type" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe AgentPaymentDue.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(3)
        overdueTableHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        overdueTableHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " "+
          AgentPaymentDue.poa1Text + " " + AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(whatYouOweDataWithOverdueData) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(5)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe AgentPaymentDue.overdueTag + " " + AgentPaymentDue.poa2Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-1-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("over-due-type-1-overdue").text shouldBe AgentPaymentDue.overdueTag
      }
      s"have payments data with button" in new Setup(whatYouOweDataWithOverdueData) {
        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe AgentPaymentDue.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.agent.routes.PaymentController.paymentHandoff(12345667).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer with mixed dates" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.title() shouldBe AgentPaymentDue.title
      }
      s"not have MTD payments heading" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithMixedData) {
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
      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(whatYouOweDataWithMixedData) {
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
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithMixedData) {
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
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithMixedData) {
        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe AgentPaymentDue.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.agent.routes.PaymentController.paymentHandoff(7500).url

        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
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
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
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
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("future-payments-heading").text shouldBe AgentPaymentDue.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(6)
        futurePaymentsHeader.select("th").first().text() shouldBe AgentPaymentDue.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe AgentPaymentDue.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe AgentPaymentDue.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(7)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe AgentPaymentDue.poa1Text + " " +
          AgentPaymentDue.taxYearForChargesText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(whatYouOweDataWithWithAciValueZeroAndOverdue) {
        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe AgentPaymentDue.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.agent.routes.PaymentController.paymentHandoff(12345667).url

      }
      s"does not have payment data payments is disabled" in new Setup(charges = whatYouOweDataWithWithAciValueZeroAndOverdue,
        paymentEnabled = false) {
        pageDocument.getElementById("payment-days-note") shouldBe null
        pageDocument.getElementById("credit-on-account") shouldBe null
        pageDocument.getElementById("payment-button") shouldBe null

      }
    }

    "the user has no charges" should {
      s"have the title '${AgentPaymentDue.title}' and notes" in new Setup(noChargesModel) {
        pageDocument.title() shouldBe AgentPaymentDue.title
        pageDocument.getElementById("no-payments-due").text shouldBe AgentPaymentDue.noPaymentsDue
        pageDocument.getElementById("sa-note-migrated").text shouldBe AgentPaymentDue.saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe AgentPaymentDue.osChargesNote
        pageDocument.getElementById("payment-days-note").text shouldBe AgentPaymentDue.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe AgentPaymentDue.creditOnAccount
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
