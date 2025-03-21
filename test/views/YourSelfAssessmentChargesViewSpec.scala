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

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.featureswitch.FeatureSwitching
import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CLASS2_NICS}
import implicits.ImplicitDateFormatter
import models.financialDetails.{BalanceDetails, DocumentDetail, WhatYouOweChargesList, YourSelfAssessmentChargesViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.outstandingCharges.OutstandingChargesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testNino, testUserTypeAgent, testUserTypeIndividual}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{dueDateOverdue, noDunningLocks, oneDunningLock, outstandingChargesModel, twoDunningLocks}
import testUtils.{TestSupport, ViewSpec}
import views.html.YourSelfAssessmentCharges

import java.time.LocalDate

class YourSelfAssessmentChargesViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec with ChargeConstants{

  val yourSelfAssessmentChargesView: YourSelfAssessmentCharges = app.injector.instanceOf[YourSelfAssessmentCharges]

  val yourSelfAssessmentChargesTitle: String = messages("htmlTitle", messages("selfAssessmentCharges.heading"))
  val yourSelfAssessmentChargesHeading: String = messages("selfAssessmentCharges.heading")
  val noPaymentsDue: String = messages("selfAssessmentCharges.no-payments-due")
  val noPaymentsAgentDue: String = messages("selfAssessmentCharges.no-payments-due-agent")
  val bannerTitle: String = messages("selfAssessmentCharges.important")
  val bannerText1: String = messages("selfAssessmentCharges.important-p1")
  val bannerText2: String = messages("selfAssessmentCharges.important-p2")
  val bannerLinkText: String = messages("selfAssessmentCharges.important-p2-link-text")
  def bannerDueNow(amount: String): String = messages("selfAssessmentCharges.charges-due-now-with-amount", amount)
  val saLink: String = s"${messages("selfAssessmentCharges.sa-link")} ${messages("pagehelp.opensInNewTabText")}"
  val saNote: String = s"${messages("selfAssessmentCharges.sa-note")} $saLink."
  val saNoteAgent: String = s"${messages("selfAssessmentCharges.sa-note-agent-1")}. ${messages("selfAssessmentCharges.sa-note-agent-2")} ${messages("selfAssessmentCharges.sa-link-agent")} ${messages("pagehelp.opensInNewTabText")}. ${messages("selfAssessmentCharges.sa-note-agent-3")}"
  val osChargesNote: String = messages("selfAssessmentCharges.outstanding-charges-note")
  val paymentUnderReviewPara: String = s"${messages("selfAssessmentCharges.dunningLock.text", s"${messages("selfAssessmentCharges.dunningLock.link")} ${messages("pagehelp.opensInNewTabText")}")}."
  val dueDate: String = messages("selfAssessmentCharges.tableHead.due-date")
  val chargeType: String = messages("selfAssessmentCharges.tableHead.type-of-charge")
  val taxYear: String = messages("selfAssessmentCharges.tableHead.tax-year")
  val amountDue: String = messages("selfAssessmentCharges.tableHead.amount")
  val estimatedInterest: String = messages("selfAssessmentCharges.tableHead.estimated-interest")
  def totalAmount(amount: String): String = messages("selfAssessmentCharges.table.total-amount", amount)
  val paymentProcessingText: String = s"${messages("selfAssessmentCharges.overdue-inset-text-1")} ${messages("selfAssessmentCharges.overdue-inset-text-2")}"
  val paymentPlanText: String = s"${messages("selfAssessmentCharges.payment-plan-1")} ${messages("selfAssessmentCharges.payment-plan-link-text")} (opens in new tab)."
  val poa1Text: String = messages("selfAssessmentCharges.paymentOnAccount1.text")
  val latePoa1Text: String = messages("selfAssessmentCharges.lpi.paymentOnAccount1.text")
  val poa2Text: String = messages("selfAssessmentCharges.paymentOnAccount2.text")
  val poaExtra1Text: String = messages("selfAssessmentCharges.reviewAndReconcilePoa1.text")
  val poaExtra2Text: String = messages("selfAssessmentCharges.reviewAndReconcilePoa2.text")
  val poa1ReconcileInterest: String = messages("selfAssessmentCharges.lpi.reviewAndReconcilePoa1.text")
  val poa2ReconcileInterest: String = messages("selfAssessmentCharges.lpi.reviewAndReconcilePoa2.text")
  val remainingBalance: String = messages("selfAssessmentCharges.balancingCharge.text")
  val preMTDRemainingBalance: String = s"${messages("selfAssessmentCharges.balancingCharge.text")} ${messages("selfAssessmentCharges.pre-mtd-digital")}"
  val remainingBalanceLine1: String = messages("selfAssessmentCharges.remaining-balance.line1")
  val remainingBalanceLine1Agent: String = messages("selfAssessmentCharges.remaining-balance.line1.agent")
  val paymentUnderReview: String = messages("selfAssessmentCharges.paymentUnderReview")
  val poaHeading: String = messages("selfAssessmentCharges.payment-on-account.heading")
  val poaLine1: String = messages("selfAssessmentCharges.payment-on-account.line1")
  val poaLine1Agent: String = messages("selfAssessmentCharges.payment-on-account.line1.agent")
  val lpiHeading: String = messages("selfAssessmentCharges.late-payment-interest.heading")
  val lpiLine1: String = messages("selfAssessmentCharges.late-payment-interest.line1")
  val lpiLine1Agent: String = messages("selfAssessmentCharges.late-payment-interest.line1.agent")
  val poa1WithTaxYearAndUnderReview: String = s"$poa1Text 1 $paymentUnderReview"
  val poa1WithTaxYearOverdueAndUnderReview: String = s"$poa1Text 1 $paymentUnderReview"
  val payNow: String = messages("selfAssessmentCharges.payNow")
  val saPaymentOnAccount1: String = "SA Payment on Account 1"
  val saPaymentOnAccount2: String = "SA Payment on Account 2"
  val hmrcAdjustment: String = messages("selfAssessmentCharges.hmrcAdjustment.text")
  val hmrcAdjustmentHeading: String = messages("selfAssessmentCharges.hmrcAdjustment.heading")
  val hmrcAdjustmentLine1: String = messages("selfAssessmentCharges.hmrcAdjustment.line1")
  val itsaPOA1: String = "ITSA- POA 1"
  val itsaPOA2: String = "ITSA - POA 2"
  val cancelledPayeSelfAssessment: String = messages("selfAssessmentCharges.cancelledPayeSelfAssessment.text")

  val interestEndDateFuture: LocalDate = LocalDate.of(2100, 1, 1)

  def interestFromToDate(from: String, to: String, rate: String) =
    s"${messages("selfAssessmentCharges.over-due.interest.line1")} ${messages("selfAssessmentCharges.over-due.interest.line2", from, to, rate)}"

  def taxYearSummaryText(from: String, to: String): String = s"${messages("selfAssessmentCharges.tax-year-summary.taxYear", from, to)} ${messages("selfAssessmentCharges.taxYear")}"

  def preMtdPayments(from: String, to: String): String = s"${messages("selfAssessmentCharges.pre-mtd-year", from, to)}"


  def ctaViewModel(isFSEnabled: Boolean): WYOClaimToAdjustViewModel = {
    if (isFSEnabled) {
      WYOClaimToAdjustViewModel(
        adjustPaymentsOnAccountFSEnabled = true,
        poaTaxYear = Some(TaxYear(
          startYear = 2024,
          endYear = 2025)
        )
      )
    } else {
      WYOClaimToAdjustViewModel(
        adjustPaymentsOnAccountFSEnabled = false,
        poaTaxYear = None)
    }
  }


  class TestSetup(charges: WhatYouOweChargesList,
                  currentTaxYear: Int = fixedDate.getYear,
                  hasLpiWithDunningLock: Boolean = false,
                  dunningLock: Boolean = false,
                  migrationYear: Int = fixedDate.getYear - 1,
                  reviewAndReconcileEnabled: Boolean = false,
                  adjustPaymentsOnAccountFSEnabled: Boolean = false,
                  claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None
                 ) {
    val individualUser: MtdItUser[_] = defaultMTDITUser(
      Some(testUserTypeIndividual),
      IncomeSourceDetailsModel(testNino, "testMtditid", Some(migrationYear.toString), List(), List())
    )

    val defaultClaimToAdjustViewModel: WYOClaimToAdjustViewModel = ctaViewModel(adjustPaymentsOnAccountFSEnabled)

    val viewModel = YourSelfAssessmentChargesViewModel(
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      backUrl = "testBackURL",
      dunningLock = dunningLock,
      reviewAndReconcileEnabled = reviewAndReconcileEnabled,
      creditAndRefundEnabled = true,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel)
    )

    val html: HtmlFormat.Appendable = yourSelfAssessmentChargesView(
      viewModel
      )(FakeRequest(), individualUser, implicitly, dateService)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def findElementById(id: String): Option[Element] = {
      Option(pageDocument.getElementById(id))
    }

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("payments-due-note").selectFirst("a")
      anchor.text shouldBe saLink
      anchor.attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      anchor.attr("target") shouldBe "_blank"
    }
  }

  class AgentTestSetup(charges: WhatYouOweChargesList,
                       currentTaxYear: Int = fixedDate.getYear,
                       migrationYear: Int = fixedDate.getYear - 1,
                       reviewAndReconcileEnabled: Boolean = false,
                       dunningLock: Boolean = false,
                       hasLpiWithDunningLock: Boolean = false,
                       adjustPaymentsOnAccountFSEnabled: Boolean = false,
                       claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None) {

    val defaultClaimToAdjustViewModel: WYOClaimToAdjustViewModel = ctaViewModel(adjustPaymentsOnAccountFSEnabled)

    val agentUser: MtdItUser[_] =
      defaultMTDITUser(Some(testUserTypeAgent), IncomeSourceDetailsModel("AA111111A", "testMtditid", Some(migrationYear.toString), List(), List()))

    def findAgentElementById(id: String): Option[Element] = {
      Option(pageDocument.getElementById(id))
    }

    val viewModel = YourSelfAssessmentChargesViewModel(
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      backUrl = "testBackURL",
      dunningLock = dunningLock,
      reviewAndReconcileEnabled = reviewAndReconcileEnabled,
      creditAndRefundEnabled = true,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel))
    val html: HtmlFormat.Appendable = yourSelfAssessmentChargesView(
      viewModel
    )(FakeRequest(), agentUser, implicitly, dateService)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  def whatYouOweDataWithOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueInterestDataCi(latePaymentInterest),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueLPI(latePaymentInterest: List[Option[BigDecimal]],
                                   dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpi(latePaymentInterest, dunningLock ),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueLPIDunningLock(latePaymentInterest: Option[BigDecimal],
                                              lpiWithDunningLock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpi(
      List(latePaymentInterest, latePaymentInterest),
      List(None, None),
      List(lpiWithDunningLock, lpiWithDunningLock)),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueLPIDunningLockZero(latePaymentInterest: Option[BigDecimal],
                                                  lpiWithDunningLock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpiDunningLockZeroCi(TaxYear.forYearEnd(fixedDate.getYear), latePaymentInterest, false, lpiWithDunningLock),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks)(1))
      ++ List(financialDetailsWithMixedData3Ci.head),

  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks)(1))
      ++ List(financialDetailsWithMixedData3Ci.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val codingOutAmount = 444.23
  val codingOutNotice = s"${messages("selfAssessmentCharges.codingOut-1a")} £43.21 ${messages("selfAssessmentCharges.codingOut-1b")} ${messages("selfAssessmentCharges.codingOut-2", "2020", "2021")} ${messages("selfAssessmentCharges.codingOut-3")}"
  val codingOutNoticeFullyCollected = s"${messages("selfAssessmentCharges.credit-overpaid-prefix")} £0.00 ${messages("selfAssessmentCharges.codingOut-1b")} ${messages("selfAssessmentCharges.codingOut-2", "2020", "2021")} ${messages("selfAssessmentCharges.codingOut-individual")}"

  val codedOutDocumentDetailNICs: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)

  val codedOutDocumentDetail: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21))

  val codedOutDocumentDetailFullyCollected: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
    amountCodedOut = Some(0))

  val codedOutDocumentDetailPayeSA: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_ACCEPTED), outstandingAmount = 0.00,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21))

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(fixedDate.minusDays(15).toString, 0.00)

  val whatYouOweDataWithWithAciValueZeroAndOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList =
      List(financialDetailsWithMixedData3Ci(1)) ++ List(financialDetailsWithMixedData3Ci.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithWithAciValueZeroAndFuturePayments: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData1Ci(1))
      ++ List(financialDetailsWithMixedData1Ci.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithCodingOutNics2: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutNics2Ci()),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(codedOutDocumentDetailCi)
  )

  val whatYouOweDataNoCharges: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val whatYouOweDataWithCodingOutFullyCollected: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutNics2Ci()),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(codedOutDocumentDetailFullyCollectedCi)
  )

  val whatYouOweDataWithMFADebits: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsMFADebitsCi.head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val whatYouOweDataWithCodingOutFuture: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutNics2Ci()),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(codedOutDocumentDetailCi)
  )

  val whatYouOweDataCodingOutWithoutAmountCodingOut: WhatYouOweChargesList = whatYouOweDataWithCodingOutNics2.copy(codedOutDocumentDetail = None)

  val whatYouOweDataWithCancelledPayeSa: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutCancelledPayeSaCi()),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))

  val whatYouOweDataWithPayeSA: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None),
    chargesList =  List(chargeItemWithCodingOutNics2Ci()),
    codedOutDocumentDetail = Some(codedOutDocumentDetailPayeSACi)
  )

  val noUtrModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))

  def claimToAdjustLink(isAgent: Boolean): String = {
    if (isAgent) {
      "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start"
    } else {
      "/report-quarterly/income-and-expenses/view/adjust-poa/start"
    }
  }

  "The Your Self Assessment charges page" when {

    "The Your Self Assessment charges view with financial details model" when {

      "the user has no charges" should {

        s"have the title $yourSelfAssessmentChargesTitle and page header and notes" in new TestSetup(charges = noChargesModel) {
          pageDocument.title() shouldBe yourSelfAssessmentChargesTitle
          pageDocument.selectFirst("h1").text shouldBe yourSelfAssessmentChargesHeading
          pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsDue
          pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
          pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote
        }

        "have the link to their previous Self Assessment online account in the sa-note" in new TestSetup(charges = noChargesModel) {
          verifySelfAssessmentLink()
        }

        "not have any tabs or button Pay now" in new TestSetup(charges = noChargesModel) {
          findElementById("self-assessment-charges-tabs") shouldBe None
          findElementById("payment-button") shouldBe None
        }
      }

      "the user overdue charges" should {

        "display the overdue charges banner" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
            pageDocument.getElementById("overdue-banner").text() shouldBe List(bannerTitle,
              bannerDueNow("£3.00"), bannerText1, bannerText2, bannerLinkText).mkString(" ")
        }

        "display the charges due now tab, with correct table header, charges and total" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          findElementById("self-assessment-charges-tabs").isDefined shouldBe true

          val tableHead = pageDocument.getElementById("over-due-payments-table-head")
          tableHead.select("th").first().text() shouldBe dueDate
          tableHead.select("th").get(1).text() shouldBe chargeType
          tableHead.select("th").get(2).text() shouldBe taxYear
          tableHead.select("th").get(3).text() shouldBe estimatedInterest
          tableHead.select("th").get(4).text() shouldBe amountDue

          val firstChargeRow = pageDocument.getElementById("balancing-charge-type-0")
          firstChargeRow.select("td").first().text() shouldBe fixedDate.minusDays(30).toLongDate
          firstChargeRow.select("td").get(1).text() shouldBe preMTDRemainingBalance
          firstChargeRow.select("td").get(2).text() shouldBe preMtdPayments(
            (fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          firstChargeRow.select("td").get(3).text() shouldBe "£12.67"
          firstChargeRow.select("td").last().text() shouldBe "£123,456.67"

          val secondChargeRow = pageDocument.getElementById("due-0")
          secondChargeRow.select("td").first().text() shouldBe fixedDate.minusDays(10).toLongDate
          secondChargeRow.select("td").get(1).text() shouldBe s"$poa1Text 1"
          secondChargeRow.select("td").get(2).text() shouldBe taxYearSummaryText("2022", "2023")
          secondChargeRow.select("td").get(3).text() shouldBe "£100.00"
          secondChargeRow.select("td").last().text() shouldBe "£50.00"

          val thirdChargeRow = pageDocument.getElementById("due-1")
          thirdChargeRow.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDate
          thirdChargeRow.select("td").get(1).text() shouldBe s"$poa2Text 2"
          thirdChargeRow.select("td").get(2).text() shouldBe taxYearSummaryText("2022", "2023")
          thirdChargeRow.select("td").get(3).text() shouldBe "£100.00"
          thirdChargeRow.select("td").last().text() shouldBe "£75.00"

          pageDocument.getElementById("total-amount").text() shouldBe totalAmount("£3.00")

        }

        "display the payment button and payment content in tab" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          findElementById("payment-button").isDefined shouldBe true
          val paymentButton = pageDocument.getElementById("payment-button")
          paymentButton.text() shouldBe messages("selfAssessmentCharges.payNow")
          pageDocument.getElementById("overdue-payment-text").text() shouldBe paymentProcessingText
        }

        "display the payment plan content and link" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          val paymentPlan = pageDocument.getElementById("payment-plan")
          paymentPlan.text() shouldBe paymentPlanText
        }

        "display poa reconciliation charges if accruing interest" in new TestSetup(charges = whatYouOweWithReviewReconcileData, reviewAndReconcileEnabled = true) {
          val firstReconcileCharge = pageDocument.getElementById("due-0")
          firstReconcileCharge.select("td").get(1).text() shouldBe s"$poaExtra1Text 1"
        }


        "display the paragraph about payments under review when there is a dunningLock" in new TestSetup(charges = whatYouOweDataWithOverdueDataIt(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new TestSetup(
          charges = whatYouOweDataWithOverdueDataIt(twoDunningLocks)) {
          findElementById("payment-under-review-info") shouldBe None
        }

        s"display $paymentUnderReview when there is a dunningLock against a single charge" in new TestSetup(charges = whatYouOweDataWithOverdueLPI(List(None, None), oneDunningLock)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(3)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$poa2Text 2"
        }

        s"display $paymentUnderReview when there is a dunningLock against multiple charges" in new TestSetup(
          charges = whatYouOweDataWithOverdueLPI(List(None, None), twoDunningLocks)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(3)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$poa2Text 2 $paymentUnderReview"
        }

        "show late payment interest as a charge where underlying charge is paid off" in new TestSetup(charges = whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {
          val tableHead = pageDocument.getElementById("over-due-payments-table-head")
          tableHead.select("th").first().text() shouldBe dueDate
          tableHead.select("th").get(1).text() shouldBe chargeType
          tableHead.select("th").get(2).text() shouldBe taxYear
          tableHead.select("th").get(3).text() shouldBe estimatedInterest
          tableHead.select("th").get(4).text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe s"$latePoa1Text 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

          pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124", isInterestCharge = true).url
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url
        }


        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - LPI Dunning Block" in
          new TestSetup(charges = whatYouOweDataWithOverdueLPIDunningLock(Some(34.56), Some(100.0))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe chargeType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYear
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe latePoa1Text + s" 1" + " " + paymentUnderReview
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              fixedDate.getYear, "1040000124", isInterestCharge = true).url
            pageDocument.getElementById("LpiDunningLock").text shouldBe "Payment under review"
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              fixedDate.getYear).url
          }

        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - No LPI Dunning Block" in
          new TestSetup(charges = whatYouOweDataWithOverdueLPIDunningLockZero(Some(34.56), Some(0))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe chargeType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYear
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe latePoa1Text + s" 1"
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              fixedDate.getYear, "1040000124", isInterestCharge = true).url
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              fixedDate.getYear).url

          }

        "have overdue payments header, bullet points and data with POA1 charge type and No Late payment interest" in new TestSetup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
          overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(10).toLongDate
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124").url
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url
        }

        "have overdue payments header, bullet points and data with POA1 charge type" in new TestSetup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
          /*
                overduePaymentsTableRow1.select("td").first().text() shouldBe toDay.minusDays(10).toLongDate
        */
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124").url
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url
        }
        "have overdue payments with POA2 charge type with hyperlink " in new TestSetup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(3)
          overduePaymentsTableRow2.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDate
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe poa2Text + s" 2"
          overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

          pageDocument.getElementById("due-1-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000125").url
        }
      }
//
//      "the user has charges and access viewer with mixed dates" should { //TODO: TO be implemented once we have multiple tabs on this page, and display charges due in the future
//        s"have the title $yourSelfAssessmentChargesTitle and notes" in new TestSetup(charges = whatYouOweDataWithMixedData1) {
//          pageDocument.title() shouldBe yourSelfAssessmentChargesTitle
//        }
//        s"not have MTD payments heading" in new TestSetup(charges = whatYouOweDataWithMixedData1) {
//          findElementById("pre-mtd-payments-heading") shouldBe None
//        }
//
//        s"have overdue table header, bullet points and data with hyperlink" in new TestSetup(charges = whatYouOweDataWithOverdueMixedData2(List(None, None, None))) {
//          val overdueTableHeader: Element = pageDocument.select("tr").first()
//          overdueTableHeader.select("th").first().text() shouldBe dueDate
//          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
//          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
//          overdueTableHeader.select("th").last().text() shouldBe amountDue
//
//          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
//          overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDate
//          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa2Text + s" 1"
//          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
//          overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"
//
//          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
//          dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe fixedDate.plusDays(30).toLongDate
//          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 2"
//          dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
//          dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"
//
//          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
//            fixedDate.getYear, "1040000125").url
//          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
//            fixedDate.getYear).url
//
//          pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
//            fixedDate.getYear, "1040000123").url
//          findElementById("due-1-overdue") shouldBe None
//          pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
//            fixedDate.getYear).url
//        }
//
//      }
    }

//    "the user has charges and access viewer with mixed dates and ACI value of zero" should {
//
//      s"have the mtd payments, bullets and table header and data with Balancing Payment data with no hyperlink" in new TestSetup(
//        charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {
//        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
//        remainingBalanceHeader.select("th").first().text() shouldBe dueDate
//        remainingBalanceHeader.select("th").get(1).text() shouldBe chargeType
//        remainingBalanceHeader.select("th").get(2).text() shouldBe taxYear
//        remainingBalanceHeader.select("th").last().text() shouldBe amountDue
//
//        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
//        remainingBalanceTable.select("td").first().text() shouldBe fixedDate.minusDays(15).toLongDate
//        remainingBalanceTable.select("td").get(1).text() shouldBe preMTDRemainingBalance
//        remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
//          (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)
//        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"
//
//      }
//      s"have overdue table header and data with hyperlink" in new TestSetup(charges = whatYouOweDataTestActiveWithMixedData2(List(None, None, None, None))) {
//        val overdueTableHeader: Element = pageDocument.select("tr").get(0)
//        overdueTableHeader.select("th").first().text() shouldBe dueDate
//        overdueTableHeader.select("th").get(1).text() shouldBe chargeType
//        overdueTableHeader.select("th").get(2).text() shouldBe taxYear
//        overdueTableHeader.select("th").last().text() shouldBe amountDue
//        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
//        overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDate
//        overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa2Text + s" 1"
//        overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
//        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"
//
//        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
//        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe fixedDate.plusDays(30).toLongDate
//        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 2"
//        dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
//        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"
//
//        pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
//          fixedDate.getYear, "1040000125").url
//        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
//          fixedDate.getYear).url
//
//        pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
//          fixedDate.getYear, "1040000123").url
//        findElementById("due-1-overdue") shouldBe None
//        pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
//          fixedDate.getYear).url
//      }
//
//      s"have payment data with button" in new TestSetup(charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {
//
//        pageDocument.getElementById("payment-button").text shouldBe payNow
//
//        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url
//
//      }
//
//    }

    "AdjustPaymentsOnAccount is enabled" when {

      "user has a POA that can be adjusted" when {

        val poaModel = ctaViewModel(true)

        "POA is paid off fully should display link with additional content" in new TestSetup(
          charges = whatYouOweDataWithPaidPOAs(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("selfAssessmentCharges.adjust-poa.paid-2", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(false)
          Option(pageDocument.getElementById("adjust-paid-poa-content")).isDefined shouldBe true
        }

        "POA is not paid off fully should display link" in new TestSetup(
          charges = whatYouOweDataWithZeroMoneyInAccount(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("selfAssessmentCharges.adjust-poa", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(false)
          Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
        }
      }

      "user has no POA that can be adjusted should not display link" in new TestSetup(
        charges = whatYouOweDataNoCharges,
        adjustPaymentsOnAccountFSEnabled = true,
        claimToAdjustViewModel = Some(WYOClaimToAdjustViewModel(true, None)) ) {
        Option(pageDocument.getElementById("adjust-poa-link")) shouldBe None
        Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
      }
    }

    "codingOut is enabled" should {
      "have coding out message displayed at the bottom of the page" in new TestSetup(charges = whatYouOweDataWithCodingOutNics2) {
        Option(pageDocument.getElementById("coding-out-summary-link")).isDefined shouldBe true
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-years/2021/charge?id=CODINGOUT02"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
      "have a class 2 Nics overdue entry" in new TestSetup(charges = whatYouOweDataWithCodingOutNics2) {
        Option(pageDocument.getElementById("due-0")).isDefined shouldBe true
        pageDocument.getElementById("due-0").text().contains(CODING_OUT_CLASS2_NICS) shouldBe true
      }

      "have a cancelled paye self assessment entry" in new TestSetup(charges = whatYouOweDataWithCancelledPayeSa) {
        findElementById("coding-out-notice") shouldBe None
        pageDocument.getElementById("due-0").text().contains(cancelledPayeSelfAssessment) shouldBe true
        findElementById("coding-out-summary-link") shouldBe None
      }
    }

    "At crystallization, the user has the coding out requested amount fully collected" should {
      "only show coding out content under header" in new TestSetup(charges = whatYouOweDataWithCodingOutFullyCollected) {
        pageDocument.getElementById("coding-out-notice").text() shouldBe codingOutNoticeFullyCollected
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-years/2021/charge?id=CODINGOUT02"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
    }
  }

  "MFA Debits is enabled" should {
    "have an HMRC adjustment payment due" in new TestSetup(charges = whatYouOweDataWithMFADebits) {
      pageDocument.title() shouldBe yourSelfAssessmentChargesTitle
      pageDocument.selectFirst("h1").text shouldBe yourSelfAssessmentChargesHeading
      pageDocument.getElementById("due-0").text.contains(hmrcAdjustment)
      pageDocument.select("#due-0 a").get(0).text() shouldBe hmrcAdjustment + s" 1"
    }
  }

  "agent" when {
    "The What you owe view with financial details model" when {

//      s"have the title '${ //TODO: Also deals with future charges, re-implement after charges due in 30 days tab is done
//        messages("htmlTitle.agent", messages("selfAssessmentCharges.heading"))
//      }'" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {
//        pageDocument.title() shouldBe messages("htmlTitle.agent", messages("selfAssessmentCharges.heading"))
//        pageDocument.getElementById("due-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(fixedDate.getYear, "1040000124").url
//        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(fixedDate.getYear).url
//      }

      "not have button Pay now with no chagres" in new AgentTestSetup(charges = noChargesModel) {
        findAgentElementById("payment-button") shouldBe None
      }
      "not have button Pay now with charges" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        findAgentElementById("payment-button") shouldBe None
      }

      "money in your account section with available credits" in new AgentTestSetup(charges = whatYouOweDataWithAvailableCredits()) {
        pageDocument.getElementById("money-in-your-account").text shouldBe messages("selfAssessmentCharges.moneyOnAccount-agent") + " " +
          messages("selfAssessmentCharges.moneyOnAccount-1") + " £300.00" + " " +
          messages("selfAssessmentCharges.moneyOnAccount-agent-2") + " " +
          messages("selfAssessmentCharges.moneyOnAccount-3") + "."
      }

      "money in your account section with no available credits" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        findAgentElementById("money-in-your-account") shouldBe None
      }

      "money in your account section with an available credit of £0.00" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30DaysAvailableCreditZero()) {
        findAgentElementById("money-in-your-account") shouldBe None
      }
    }

    "the user has no charges" should {
      s"have the title ${messages("agent.header.serviceName", messages("selfAssessmentCharges.heading"))} and page header and notes" in new AgentTestSetup(charges = noChargesModel) {
        pageDocument.title() shouldBe messages("htmlTitle.agent", messages("selfAssessmentCharges.heading"))
        pageDocument.selectFirst("h1").text shouldBe yourSelfAssessmentChargesHeading
        pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsAgentDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote
      }
    }

    "AdjustPaymentsOnAccount is enabled" when {

      "user has a POA that can be adjusted" when {

        val poaModel = ctaViewModel(true)

        "POA is paid off fully should display link with additional content" in new AgentTestSetup(
          charges = whatYouOweDataWithPaidPOAs(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("selfAssessmentCharges.adjust-poa.paid-2", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(true)
          Option(pageDocument.getElementById("adjust-paid-poa-content")).isDefined shouldBe true
        }

        "POA is not paid off fully should display link" in new AgentTestSetup(
          charges = whatYouOweDataWithZeroMoneyInAccount(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("selfAssessmentCharges.adjust-poa", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(true)
          Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
        }
      }

      "user has no POA that can be adjusted should not display link" in new AgentTestSetup(
        charges = whatYouOweDataNoCharges,
        adjustPaymentsOnAccountFSEnabled = true,
        claimToAdjustViewModel = Some(WYOClaimToAdjustViewModel(true, None)) ) {
        Option(pageDocument.getElementById("adjust-poa-link")) shouldBe None
        Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
      }
    }
  }

  "what you owe view" should {

    val unallocatedCreditMsg = "You have £100.00 in your account. We’ll use this to pay the amount due on the next due date."
    "show unallocated credits" when {
      "user is an individual with the feature switch on" in new TestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("unallocated-credit-note").text() shouldBe unallocatedCreditMsg
      }

      "user is an agent with the feature switch on" in new AgentTestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("unallocated-credit-note").text() shouldBe unallocatedCreditMsg
      }
    }

    "not show unallocated credits" when {
      "user has no money in his account" in new TestSetup(charges = whatYouOweDataWithZeroMoneyInAccount()) {
        findElementById("unallocated-credit-note") shouldBe None
      }
    }
  }

}
