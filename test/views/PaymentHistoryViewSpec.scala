/*
 * Copyright 2023 HM Revenue & Customs
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

import config.FrontendAppConfig
import implicits.ImplicitCurrencyFormatter._
import implicits.ImplicitDateFormatter
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.paymentCreditAndRefundHistory.PaymentCreditAndRefundHistoryViewModel
import models.repaymentHistory.PaymentHistoryEntry
import org.jsoup.nodes.Element
import play.api.test.FakeRequest
import services.DateServiceInterface
import testUtils.ViewSpec
import views.html.PaymentHistory

import java.time.LocalDate
import java.time.Month.APRIL


class PaymentHistoryViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  lazy val paymentHistoryView: PaymentHistory = app.injector.instanceOf[PaymentHistory]

  implicit val dateServiceInterface: DateServiceInterface = new DateServiceInterface {

    override def getCurrentDate: LocalDate = fixedDate

    override protected def now(): LocalDate = fixedDate

    override def getCurrentTaxYear: TaxYear = TaxYear.forYearEnd(fixedDate.getYear)

    override def getCurrentTaxYearEnd: Int = fixedDate.getYear + 1

    override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2023, 4, 6)

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate =  {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate
      } else {
        accountingPeriodEndDate.plusYears(1)
      }
    }

    override def isWithin30Days(date: LocalDate): Boolean = false
  }

  object PaymentHistoryMessages {
    val heading: String = messages("paymentHistory.heading")
    val paymentHistoryRefundHeading = "Payment and refund history"
    val title: String = messages("htmlTitle", heading)
    val agentTitle: String = messages("htmlTitle.agent", heading)
    val titlePaymentRefundEnabled: String = messages("htmlTitle", messages("paymentHistory.paymentAndRefundHistory.heading"))

    val info: String = s"${messages("PaymentHistory.classicSA")} ${messages("taxYears.oldSa.content.link")} ${messages("pagehelp.opensInNewTabText")}."

    def button(year: Int): String = s"$year payments"

    val paymentToHmrc: String = messages("paymentHistory.paymentToHmrc")

    val CardRef: String = messages("paymentsHistory.CardRef")
    val earlierPaymentToHMRC: String = messages("paymentAllocation.earlyTaxYear.heading")

    val paymentHeadingDate: String = messages("paymentHistory.table.header.date")
    val paymentHeadingDescription: String = messages("paymentHistory.table.header.description")
    val paymentHeadingTaxYear: String = messages("paymentHistory.tableHead.taxYear")
    val paymentHeadingAmount: String = messages("paymentHistory.table.header.amount")
    val partialH2Heading = "payments"
    val saLink: String = s"${messages("selfAssessmentCharges.sa-link")} ${messages("pagehelp.opensInNewTabText")}"
  }



  val paymentEntriesMFA = List(
    (2020, List(
      PaymentHistoryEntry(date = "2020-12-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface),
      PaymentHistoryEntry(date = "2020-04-13", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface))),
    (2021, List(
      PaymentHistoryEntry(date = "2019-04-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface),
      PaymentHistoryEntry(date = "2018-04-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface))),
    (2022, List(
      PaymentHistoryEntry(date = "2019-12-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface),
      PaymentHistoryEntry(date = "2019-09-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface)))
  )

  val repaymentRequestNumber = "000000003135"

  val groupedRepayments = List(
    (2021, List(PaymentHistoryEntry("2021-08-22", Repayment, None, None, s"refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber)(dateServiceInterface),
      PaymentHistoryEntry("2021-08-21", Repayment, Some(300.0), None, s"refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber)(dateServiceInterface),
      PaymentHistoryEntry("2021-08-20", Repayment, Some(301.0), None, s"refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber)(dateServiceInterface)))
  )

  val expectedDatesOrder = List("25 December 2020", "13 April 2020", "25 December 2019", "25 September 2019", "25 April 2019", "25 April 2018")

  val emptyPayments = List(
    (2021, List(PaymentHistoryEntry(date = "2019-09-25", creditType = PaymentType, amount = None, transactionId = Some("TRANS123"),
      linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface)))
  )

  val viewModel = PaymentCreditAndRefundHistoryViewModel(paymentHistoryAndRefundsEnabled = false, creditsRefundsRepayEnabled = false)

  class PaymentHistorySetup(testPayments: List[(Int, List[PaymentHistoryEntry])] = groupedRepayments, paymentCreditAndRefundHistoryViewModel: PaymentCreditAndRefundHistoryViewModel = viewModel, saUtr: Option[String] = Some("1234567890"), isAgent: Boolean = false) extends Setup(
    paymentHistoryView(testPayments, paymentCreditAndRefundHistoryViewModel, paymentHistoryAndRefundsEnabled = true, "testBackURL", saUtr, isAgent = isAgent)(FakeRequest(), implicitly)
  )

  class PaymentHistorySetup1(paymentsnotFull: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(paymentsnotFull, PaymentCreditAndRefundHistoryViewModel(paymentHistoryAndRefundsEnabled = false, creditsRefundsRepayEnabled = false), paymentHistoryAndRefundsEnabled = false, "testBackURL", saUtr, isAgent = false)(FakeRequest(), implicitly)
  )

  class PaymentHistorySetupMFA(testPayments: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(testPayments, PaymentCreditAndRefundHistoryViewModel(paymentHistoryAndRefundsEnabled = false, creditsRefundsRepayEnabled = false), paymentHistoryAndRefundsEnabled = false, "testBackURL", saUtr, isAgent = false)(FakeRequest(), implicitly)
  )

  val paymentHistoryMessageInfo = s"${messages("paymentHistory.info")} ${messages("taxYears.oldSa.agent.content.2")} ${messages("pagehelp.opensInNewTabText")}. ${messages("paymentHistory.info.2")}"

  val entry = PaymentHistoryEntry(date = "2020-12-25", creditType = MfaCreditType, amount = Some(-10000.00), transactionId = Some("TRANS123"),
    linkUrl = "link1", visuallyHiddenText = "hidden-text1")(dateServiceInterface)

  def getContent(row: Int)(implicit layoutContent: Element): String = {
    val sectionContent = layoutContent.selectHead(s"#accordion-default-content-1")
    val tbody = sectionContent.selectHead("table > tbody")
    val rowHtml = tbody.selectNth("tr", row + 1)
    rowHtml.selectNth("td", 2).select("a.govuk-link").first().ownText()
  }

  "The payments history view" when {

    "logged in as a user" when {

      "the user has payment history for a single Year" should {

          s"display correct content" in new PaymentHistorySetup(List(
            (2020, List(
              entry,
              entry.copy(date = "2020-12-24", creditType = CutOverCreditType)(dateServiceInterface),
              entry.copy(date = "2020-12-23", creditType = BalancingChargeCreditType)(dateServiceInterface),
              entry.copy(date = "2020-12-21", creditType = RepaymentInterest)(dateServiceInterface),
              entry.copy(date = "2020-12-20", creditType = PaymentType)(dateServiceInterface),
              entry.copy(date = "2020-12-19", creditType = Repayment)(dateServiceInterface))))) {

            document.getElementById("payment-0").child(0).ownText() shouldBe "Credit from HMRC adjustment"
            document.getElementById("payment-1").child(0).ownText() shouldBe "Credit from an earlier tax year"
            document.getElementById("payment-2").child(0).ownText() shouldBe "Credit from overpaid tax"
            document.getElementById("payment-3").child(0).ownText() shouldBe "Credit from repayment interest"
            document.getElementById("payment-4").child(0).ownText() shouldBe "Payment you made to HMRC"
            document.getElementById("payment-5").child(0).ownText() shouldBe "Refund issued"
        }
        s"display Review and Reconcile Credits in a Table" in new PaymentHistorySetup(List(
          (2020, List(
            entry.copy(date = "2020-12-23", creditType = PoaTwoReconciliationCredit)(dateServiceInterface),
            entry.copy(date = "2020-12-23", creditType = PoaOneReconciliationCredit)(dateServiceInterface))))) {

          document.getElementById("payment-0").child(0).ownText() shouldBe "First payment on account: credit from your tax return"
          document.getElementById("payment-1").child(0).ownText() shouldBe "Second payment on account: credit from your tax return"
        }

        "has payment and refund history title when CreditsRefundsRepay OFF / PaymentHistoryRefunds ON" in
          new PaymentHistorySetup(paymentCreditAndRefundHistoryViewModel = PaymentCreditAndRefundHistoryViewModel(false, true)) {
            document.title() shouldBe messages("htmlTitle", messages("paymentHistory.paymentAndRefundHistory.heading"))
            layoutContent.selectHead("h1").text shouldBe messages("paymentHistory.paymentAndRefundHistory.heading")
          }
        "has payment and credit history title when CreditsRefundsRepay ON / PaymentHistoryRefunds OFF" in
          new PaymentHistorySetup(paymentCreditAndRefundHistoryViewModel = PaymentCreditAndRefundHistoryViewModel(true, false)) {
            document.title() shouldBe messages("htmlTitle", messages("paymentHistory.paymentAndCreditHistory"))
            layoutContent.selectHead("h1").text shouldBe messages("paymentHistory.paymentAndCreditHistory")
          }
        "has payment, credit and refund history title when CreditsRefundsRepay ON / PaymentHistoryRefunds ON" in
          new PaymentHistorySetup(paymentCreditAndRefundHistoryViewModel = PaymentCreditAndRefundHistoryViewModel(true, true)) {
            document.title() shouldBe messages("htmlTitle", messages("paymentHistory.paymentCreditAndRefundHistory.heading"))
            layoutContent.selectHead("h1").text shouldBe messages("paymentHistory.paymentCreditAndRefundHistory.heading")
          }
        "has payment history title when CreditsRefundsRepay OFF / PaymentHistoryRefunds OFF" in
          new PaymentHistorySetup(paymentCreditAndRefundHistoryViewModel = PaymentCreditAndRefundHistoryViewModel(false, false)) {
            document.title() shouldBe messages("htmlTitle", messages("paymentHistory.heading"))
            layoutContent.selectHead("h1").text shouldBe messages("paymentHistory.heading")
          }
      }

      s"the user has has a payment history for multiple years" should {
        s"have the table caption" in new PaymentHistorySetup(paymentEntriesMFA) {
          layoutContent.selectHead("div").selectNth("div", 2).selectHead("table")
            .selectHead("caption").text.contains(PaymentHistoryMessages.partialH2Heading)
        }
        s"have table headings for each table column" in new PaymentHistorySetup(paymentEntriesMFA) {
          val row: Element = layoutContent.selectHead("div").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
          row.selectNth("th", 1).text shouldBe PaymentHistoryMessages.paymentHeadingDate
          row.selectNth("th", 2).text shouldBe PaymentHistoryMessages.paymentHeadingDescription
          row.selectNth("th", 3).text shouldBe PaymentHistoryMessages.paymentHeadingTaxYear
          row.selectNth("th", 4).text shouldBe PaymentHistoryMessages.paymentHeadingAmount
        }
        s"have table headings for amount column right aligned" in new PaymentHistorySetup(paymentEntriesMFA) {
          val row: Element = layoutContent.selectHead("div").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
          row.selectNth("th", 3).hasClass("govuk-table__header--numeric")
        }

        s"have the information  ${PaymentHistoryMessages.info}" in new PaymentHistorySetup(paymentEntriesMFA) {
          layoutContent.select(Selectors.p).text shouldBe PaymentHistoryMessages.info
          layoutContent.selectFirst(Selectors.p).hasCorrectLink(PaymentHistoryMessages.saLink, "http://localhost:8930/self-assessment/ind/1234567890/account")
        }

        s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(paymentEntriesMFA, saUtr = None) {
          layoutContent.select("#payment-history-info").text should not be PaymentHistoryMessages.info
        }

        "display payment history by year" in new PaymentHistorySetup(paymentEntriesMFA) {
          for (((year, payments), index) <- paymentEntriesMFA.zipWithIndex) {
            layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe s"${year.toString} activity"
            layoutContent.selectHead(s"#accordion-default-content-1 > table > caption").text shouldBe "2020 payments, credits and refunds"
            val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
            val tbody = sectionContent.selectHead("table > tbody")
            payments.zipWithIndex.foreach {
              case (payment, index) =>
                val row = tbody.selectNth("tr", index + 1)
                row.selectNth("td", 1).text shouldBe payment.date.toLongDateShort
                row.selectNth("td", 2).text shouldBe s"Credit from HMRC adjustment hidden-text1 Item " + (index+1)
                row.selectNth("td", 2).select("a").attr("href") shouldBe s"link1"
                row.selectNth("td", 3).text shouldBe s"${payment.getTaxYear.startYear} to ${payment.getTaxYear.endYear}"
                row.selectNth("td", 4).text shouldBe payment.amount.get.abs.toCurrencyString
            }
          }
        }
      }

      s"should have a refund block with correct relative link" in new PaymentHistorySetup(groupedRepayments) {
        val tbody = layoutContent.selectHead("table > tbody")

        tbody.selectNth("tr", 1).selectNth("td", 1).text() shouldBe "22 Aug 2021"
        tbody.selectNth("tr", 1).selectNth("td", 2).text() shouldBe "Refund issued 000000003135 Item 1"
        tbody.selectNth("tr", 1).select("a").attr("href") shouldBe "refund-to-taxpayer/000000003135"
        tbody.selectNth("tr", 1).selectNth("td", 3).text() shouldBe "2021 to 2022"
        tbody.selectNth("tr", 1).selectNth("td", 4).text() shouldBe "Unknown"

        tbody.selectNth("tr", 2).selectNth("td", 1).text() shouldBe "21 Aug 2021"
        tbody.selectNth("tr", 2).selectNth("td", 2).text() shouldBe "Refund issued 000000003135 Item 2"
        tbody.selectNth("tr", 2).select("a").attr("href") shouldBe "refund-to-taxpayer/000000003135"
        tbody.selectNth("tr", 2).selectNth("td", 3).text() shouldBe "2021 to 2022"
        tbody.selectNth("tr", 2).selectNth("td", 4).text() shouldBe "£300.00"

        tbody.selectNth("tr", 3).selectNth("td", 1).text() shouldBe "20 Aug 2021"
        tbody.selectNth("tr", 3).selectNth("td", 2).text() shouldBe "Refund issued 000000003135 Item 3"
        tbody.selectNth("tr", 3).select("a").attr("href") shouldBe "refund-to-taxpayer/000000003135"
        tbody.selectNth("tr", 3).selectNth("td", 3).text() shouldBe "2021 to 2022"
        tbody.selectNth("tr", 3).selectNth("td", 4).text() shouldBe "£301.00"
      }

      s"should have a amount column right aligned" in new PaymentHistorySetup(groupedRepayments) {
        val tbody = layoutContent.selectHead("table > tbody")

        tbody.selectNth("tr", 1).selectNth("td", 3).hasClass("govuk-table__cell--numeric")
        tbody.selectNth("tr", 2).selectNth("td", 3).hasClass("govuk-table__cell--numeric")
        tbody.selectNth("tr", 3).selectNth("td", 3).hasClass("govuk-table__cell--numeric")
      }
    }

    "logged as an Agent" should {

      s"display Review and Reconcile Credits" in new PaymentHistorySetup(isAgent = true, testPayments = List(
        (2020, List(
          entry.copy(date = "2020-12-23", creditType = PoaTwoReconciliationCredit)(dateServiceInterface),
          entry.copy(date = "2020-12-23", creditType = PoaOneReconciliationCredit)(dateServiceInterface))))) {

          document.getElementById("payment-0").child(0).ownText() shouldBe "First payment on account: credit from your tax return"
          document.getElementById("payment-1").child(0).ownText() shouldBe "Second payment on account: credit from your tax return"
      }

      s"have the title '${PaymentHistoryMessages.agentTitle}'" in new PaymentHistorySetupWhenAgentView(paymentEntriesMFA) {
        document.title() shouldBe PaymentHistoryMessages.agentTitle
      }

      s"have the information ${messages("paymentHistory.info")}" in new PaymentHistorySetup(paymentEntriesMFA, isAgent = true) {
        layoutContent.select(Selectors.p).text shouldBe paymentHistoryMessageInfo
        layoutContent.selectFirst(Selectors.p).hasCorrectLink(s"${messages("taxYears.oldSa.agent.content.2")} ${messages("pagehelp.opensInNewTabText")}", "https://www.gov.uk/guidance/self-assessment-for-agents-online-service")
      }

      s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(paymentEntriesMFA, saUtr = None, isAgent = true) {
        layoutContent.select("#payment-history-info").text should not be paymentHistoryMessageInfo
      }

      s"should have a refund block with correct relative link" in new PaymentHistorySetup(groupedRepayments, saUtr = None, isAgent = true) {
        val tbody = layoutContent.selectHead("table > tbody")

        tbody.selectNth("tr", 1).selectNth("td", 1).text() shouldBe "22 Aug 2021"
        tbody.selectNth("tr", 1).selectNth("td", 2).text() shouldBe "Refund issued 000000003135 Item 1"
        tbody.selectNth("tr", 1).selectNth("td", 3).text() shouldBe "2021 to 2022"
        tbody.selectNth("tr", 1).select("a").attr("href") shouldBe "refund-to-taxpayer/000000003135"

        tbody.selectNth("tr", 2).selectNth("td", 1).text() shouldBe "21 Aug 2021"
        tbody.selectNth("tr", 2).selectNth("td", 2).text() shouldBe "Refund issued 000000003135 Item 2"
        tbody.selectNth("tr", 2).select("a").attr("href") shouldBe "refund-to-taxpayer/000000003135"
        tbody.selectNth("tr", 2).selectNth("td", 3).text() shouldBe "2021 to 2022"
        tbody.selectNth("tr", 2).selectNth("td", 4).text() shouldBe "£300.00"
      }
    }
  }

  class PaymentHistorySetupWhenAgentView(testPayments: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(paymentEntriesMFA, PaymentCreditAndRefundHistoryViewModel(paymentHistoryAndRefundsEnabled = false, creditsRefundsRepayEnabled = false), paymentHistoryAndRefundsEnabled = false, "testBackURL", saUtr, isAgent = true)(FakeRequest(), implicitly)
  )

}
