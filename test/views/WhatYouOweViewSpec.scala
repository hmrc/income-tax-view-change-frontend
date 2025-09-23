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
import controllers.routes.{ChargeSummaryController, CreditAndRefundController, PaymentController}
import enums.CodingOutType._
import implicits.ImplicitDateFormatter
import models.financialDetails._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.outstandingCharges._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testNino, testUserTypeAgent, testUserTypeIndividual}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import testUtils.{TestSupport, ViewSpec}
import views.html.WhatYouOwe

import java.time.LocalDate

class WhatYouOweViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec with ChargeConstants {

  val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]
  val whatYouOweTitle: String = messages("htmlTitle", messages("whatYouOwe.heading"))
  val whatYouOweHeading: String = messages("whatYouOwe.heading")
  val whatYouOweAgentHeading: String = messages("whatYouOwe.heading-agent")
  val noPaymentsDue: String = messages("whatYouOwe.no-payments-due")
  val noPaymentsAgentDue: String = messages("whatYouOwe.no-payments-due-agent")
  val saLink: String = s"${messages("whatYouOwe.sa-link")} ${messages("pagehelp.opensInNewTabText")}"
  val saNote: String = s"${messages("whatYouOwe.sa-note")} $saLink."
  val saNoteAgent: String = s"${messages("whatYouOwe.sa-note-agent-1")}. ${messages("whatYouOwe.sa-note-agent-2")} ${messages("whatYouOwe.sa-link-agent")} ${messages("pagehelp.opensInNewTabText")}. ${messages("whatYouOwe.sa-note-agent-3")}"
  val osChargesNote: String = messages("whatYouOwe.outstanding-charges-note")
  val paymentUnderReviewPara: String = s"${messages("whatYouOwe.dunningLock.text", s"${messages("whatYouOwe.dunningLock.link")} ${messages("pagehelp.opensInNewTabText")}")}."
  val chargeType: String = messages("tax-year-summary.payments.charge-type")
  val taxYear: String = messages("whatYouOwe.tableHead.tax-year")
  val amountDue: String = messages("whatYouOwe.tableHead.amount-due")
  val paymentProcessingBullet1: String = s"${messages("whatYouOwe.payments-made-bullet-1.1")} ${messages("whatYouOwe.payments-made-bullet-1.2")} ${messages("pagehelp.opensInNewTabText")}"
  val paymentProcessingBullet2: String = messages("whatYouOwe.payments-made-bullet-2")
  val paymentProcessingBulletAgent1: String = s"${messages("whatYouOwe.payments-made-bullet-1.1")} ${messages("whatYouOwe.payments-made-bullet-agent-1.2")} ${messages("pagehelp.opensInNewTabText")}"
  val paymentProcessingBulletAgent2: String = messages("whatYouOwe.payments-made-bullet-agent-2")
  val paymentsMade: String = messages("whatYouOwe.payments-made")
  val paymentsMadeAgent: String = messages("whatYouOwe.payments-made-agent")
  val poa1Text: String = messages("whatYouOwe.paymentOnAccount1.text")
  val latePoa1Text: String = messages("whatYouOwe.lpi.paymentOnAccount1.text")
  val poa2Text: String = messages("whatYouOwe.paymentOnAccount2.text")
  val poaExtra1Text: String = messages("whatYouOwe.reviewAndReconcilePoa1.text")
  val poaExtra2Text: String = messages("whatYouOwe.reviewAndReconcilePoa2.text")
  val poa1ReconcileInterest: String = messages("whatYouOwe.lpi.reviewAndReconcilePoa1.text")
  val poa2ReconcileInterest: String = messages("whatYouOwe.lpi.reviewAndReconcilePoa2.text")
  val lspText: String = messages("whatYouOwe.lateSubmissionPenalty.text")
  val lpp1Text: String = messages("whatYouOwe.firstLatePaymentPenalty.text")
  val lpp2Text: String = messages("whatYouOwe.secondLatePaymentPenalty.text")
  val lspInterest: String = messages("whatYouOwe.lpi.lateSubmissionPenalty.text")
  val lpp1Interest: String = messages("whatYouOwe.lpi.firstLatePaymentPenalty.text")
  val lpp2Interest: String = messages("whatYouOwe.lpi.secondLatePaymentPenalty.text")
  val remainingBalance: String = messages("whatYouOwe.balancingCharge.text")
  val preMTDRemainingBalance: String = s"${messages("whatYouOwe.balancingCharge.text")} ${messages("whatYouOwe.pre-mtd-digital")}"
  val remainingBalanceLine1: String = messages("whatYouOwe.remaining-balance.line1")
  val remainingBalanceLine1Agent: String = messages("whatYouOwe.remaining-balance.line1.agent")
  val paymentUnderReview: String = messages("whatYouOwe.paymentUnderReview")
  val poaHeading: String = messages("whatYouOwe.payment-on-account.heading")
  val poaLine1: String = messages("whatYouOwe.payment-on-account.line1")
  val poaLine1Agent: String = messages("whatYouOwe.payment-on-account.line1.agent")
  val lpiHeading: String = messages("whatYouOwe.late-payment-interest.heading")
  val lpiLine1: String = messages("whatYouOwe.late-payment-interest.line1")
  val lpiLine1Agent: String = messages("whatYouOwe.late-payment-interest.line1.agent")
  val overdueTag: String = messages("whatYouOwe.over-due")
  val poa1WithTaxYearAndUnderReview: String = s"$poa1Text 1 $paymentUnderReview"
  val poa1WithTaxYearOverdueAndUnderReview: String = s"$overdueTag $poa1Text 1 $paymentUnderReview"
  val dueDate: String = messages("whatYouOwe.tableHead.due-date")
  val payNow: String = messages("whatYouOwe.payNow")
  val saPaymentOnAccount1: String = "SA Payment on Account 1"
  val saPaymentOnAccount2: String = "SA Payment on Account 2"
  val hmrcAdjustment: String = messages("whatYouOwe.hmrcAdjustment.text")
  val hmrcAdjustmentHeading: String = messages("whatYouOwe.hmrcAdjustment.heading")
  val hmrcAdjustmentLine1: String = messages("whatYouOwe.hmrcAdjustment.line1")
  val itsaPOA1: String = "ITSA- POA 1"
  val itsaPOA2: String = "ITSA - POA 2"
  val cancelledPayeSelfAssessment: String = messages("whatYouOwe.cancelledPayeSelfAssessment.text")
  val poa1CollectedCodedOut = messages("whatYouOwe.poa1CodedOut.text")
  val poa2CollectedCodedOut = messages("whatYouOwe.poa2CodedOut.text")
  val paymentPlanText: String = s"${messages("selfAssessmentCharges.payment-plan-1")} ${messages("selfAssessmentCharges.payment-plan-link-text")} (opens in new tab)."


  val interestEndDateFuture: LocalDate = LocalDate.of(2100, 1, 1)

  def ctaViewModel: WYOClaimToAdjustViewModel = {
      WYOClaimToAdjustViewModel(
        poaTaxYear = Some(TaxYear(
          startYear = 2024,
          endYear = 2025)
        )
      )
    }

  def interestFromToDate(from: String, to: String, rate: String) =
    s"${messages("whatYouOwe.over-due.interest.line1")} ${messages("whatYouOwe.over-due.interest.line2", from, to, rate)}"

  def taxYearSummaryText(from: String, to: String): String = s"${messages("whatYouOwe.tax-year-summary.taxYear", from, to)} ${messages("whatYouOwe.taxYear")}"

  def preMtdPayments(from: String, to: String) = s"${messages("whatYouOwe.pre-mtd-year", from, to)}"

  class TestSetup(charges: WhatYouOweChargesList,
                  currentTaxYear: Int = fixedDate.getYear,
                  hasLpiWithDunningLock: Boolean = false,
                  dunningLock: Boolean = false,
                  taxYear: Int = fixedDate.getYear,
                  migrationYear: Int = fixedDate.getYear - 1,
                  adjustPaymentsOnAccountFSEnabled: Boolean = false,
                  claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None,
                  LPP2Url: String = ""
                 ) {
    val individualUser: MtdItUser[_] = defaultMTDITUser(
      Some(testUserTypeIndividual),
      IncomeSourceDetailsModel(testNino, "testMtditid", Some(migrationYear.toString), List(), List())
    )

    val defaultClaimToAdjustViewModel = ctaViewModel

    val wyoViewModel: WhatYouOweViewModel = WhatYouOweViewModel(
      currentDate = dateService.getCurrentDate,
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      creditAndRefundUrl = CreditAndRefundController.show().url,
      creditAndRefundEnabled = true,
      taxYearSummaryUrl = _ => controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear).url,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel),
      lpp2Url = LPP2Url,
      adjustPoaUrl = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = false).url,
      chargeSummaryUrl = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) =>
        ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url,
      paymentHandOffUrl = PaymentController.paymentHandoff(_, None).url,
      selfServeTimeToPayEnabled = true,
      selfServeTimeToPayStartUrl = "/self-serve-time-to-pay"
    )

    val html: HtmlFormat.Appendable = whatYouOweView(wyoViewModel)(FakeRequest(), individualUser, implicitly, dateService)
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
                       dunningLock: Boolean = false,
                       taxYear: Int = fixedDate.getYear,
                       hasLpiWithDunningLock: Boolean = false,
                       adjustPaymentsOnAccountFSEnabled: Boolean = false,
                       claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None) {

    val defaultClaimToAdjustViewModel = ctaViewModel

    val agentUser: MtdItUser[_] =
      defaultMTDITUser(Some(testUserTypeAgent), IncomeSourceDetailsModel("AA111111A", "testMtditid", Some(migrationYear.toString), List(), List()))

    def findAgentElementById(id: String): Option[Element] = {
      Option(pageDocument.getElementById(id))
    }

    val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]
    private val currentDateIs: LocalDate = dateService.getCurrentDate

    val wyoViewModelAgent: WhatYouOweViewModel = WhatYouOweViewModel(
      currentDate = currentDateIs,
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      creditAndRefundUrl = CreditAndRefundController.showAgent().url,
      creditAndRefundEnabled = true,
      taxYearSummaryUrl = _ => controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel),
      lpp2Url = "",
      adjustPoaUrl = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = true).url,
      chargeSummaryUrl = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) =>
        ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url,
      paymentHandOffUrl = PaymentController.paymentHandoff(_, None).url,
      selfServeTimeToPayEnabled = true,
      selfServeTimeToPayStartUrl = "/self-serve-time-to-pay"
    )

    val html: HtmlFormat.Appendable = whatYouOweView(wyoViewModelAgent)(FakeRequest(), agentUser, implicitly, dateService)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }


  def financialDetailsOverdueWithLpiDunningLock(accruingInterestAmount: Option[BigDecimal], lpiWithDunningLock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLPIDunningLock(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.minusDays(10).toString), Some(fixedDate.minusDays(1).toString)),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    accruingInterestAmount = accruingInterestAmount,
    lpiWithDunningLock = lpiWithDunningLock
  )

  def financialDetailsOverdueWithLpiDunningLockZero(accruingInterestAmount: Option[BigDecimal], lpiWithDunningLock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLpiDunningLockZero(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.minusDays(10).toString), Some(fixedDate.minusDays(1).toString)),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    accruingInterestAmount = accruingInterestAmount,
    lpiWithDunningLock = lpiWithDunningLock
  )

  def whatYouOweDataWithOverdueInterestData(accruingInterestAmount: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueInterestDataCi(accruingInterestAmount),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueAccruedInterest(accruingInterestAmount: List[Option[BigDecimal]],
                                               dunningLock: List[Option[String]] = noDunningLocks,
                                               outstandingAmount: List[BigDecimal] = List(50.0, 75.0)): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpi(accruingInterestAmount, dunningLock, outstandingAmount = outstandingAmount),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueLpiDunningLock(accruingInterestAmount: Option[BigDecimal],
                                              lpiWithDunningLock: Option[BigDecimal],
                                              outstandingAmount: List[BigDecimal] = List(50.0, 75.0)): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpi(
      List(accruingInterestAmount, accruingInterestAmount),
      List(None, None),
      List(lpiWithDunningLock, lpiWithDunningLock),
      outstandingAmount),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueLpiDunningLockZero(accruingInterestAmount: Option[BigDecimal],
                                                  lpiWithDunningLock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpiDunningLockZeroCi(TaxYear.forYearEnd(fixedDate.getYear), accruingInterestAmount, false, lpiWithDunningLock),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueMixedData2(accruingInterestAmount: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(accruingInterestAmount, noDunningLocks)(1))
      ++ List(financialDetailsWithMixedData3Ci.head),

  )

  def whatYouOweDataTestActiveWithMixedData2(accruingInterestAmount: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(accruingInterestAmount, noDunningLocks)(1))
      ++ List(financialDetailsWithMixedData3Ci.head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val codingOutAmount = 444.23
  val codingOutNotice = s"${messages("whatYouOwe.codingOut-1a")} £43.21 ${messages("whatYouOwe.codingOut-1b")} ${messages("whatYouOwe.codingOut-2", "2020", "2021")} ${messages("whatYouOwe.codingOut-3")}"
  val codingOutNoticeFullyCollected = s"${messages("whatYouOwe.credit-overpaid-prefix")} £0.00 ${messages("whatYouOwe.codingOut-1b")} ${messages("whatYouOwe.codingOut-2", "2020", "2021")} ${messages("whatYouOwe.codingOut-individual")}"

  val codedOutDocumentDetailNICs: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None)

  val codedOutDocumentDetail: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
    amountCodedOut = Some(43.21))

  val codedOutDocumentDetailFullyCollected: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
    amountCodedOut = Some(0))

  val codedOutDocumentDetailPayeSA: DocumentDetail = DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some(CODING_OUT_ACCEPTED), outstandingAmount = 0.00,
    originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
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
    codedOutDetails = Some(codedOutDetails)
  )

  val whatYouOweDataNoCharges: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(),
    outstandingChargesModel = None,
    codedOutDetails = None
  )

  val whatYouOweDataWithCodingOutFullyCollected: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutNics2Ci()),
    outstandingChargesModel = None,
    codedOutDetails = Some(CodingOutDetails(0.00, TaxYear.forYearEnd(2021)))
  )

  val whatYouOweDataWithMFADebits: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsMFADebitsCi.head),
    outstandingChargesModel = None,
    codedOutDetails = None
  )

  val whatYouOweDataWithCodingOutFuture: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutNics2Ci()),
    outstandingChargesModel = None,
    codedOutDetails = Some(codedOutDetails)
  )

  val whatYouOweDataCodingOutWithoutAmountCodingOut: WhatYouOweChargesList = whatYouOweDataWithCodingOutNics2.copy(codedOutDetails = None)

  val whatYouOweDataWithCancelledPayeSa: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithCodingOutCancelledPayeSaCi()),
    outstandingChargesModel = None,
    codedOutDetails = None
  )

  val whatYouOweWithPoaOneCollected: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithPoaCodingOutAccepted()),
    outstandingChargesModel = None,
    codedOutDetails = Some(codedOutDetails)
  )

  val whatYouOweWithPoaTwoCollected: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(chargeItemWithPoaCodingOutAccepted().copy(transactionType = PoaTwoDebit)),
    outstandingChargesModel = None,
    codedOutDetails = Some(codedOutDetails)
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))

  val whatYouOweDataWithPayeSA: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None),
    chargesList =  List(chargeItemWithCodingOutNics2Ci()),
    codedOutDetails = Some(codedOutDetails)
  )

  val noUtrModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))

  def claimToAdjustLink(isAgent: Boolean): String = {
    if (isAgent) {
      "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start"
    } else {
      "/report-quarterly/income-and-expenses/view/adjust-poa/start"
    }
  }

  "individual" when {
    "The What you owe view with financial details model" when {
      "the user has charges and access viewer before 30 days of due date" should {

        "have the Balancing Payment title " in new TestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe chargeType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYear
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue
        }
        "Balancing Payment row data exists and should not contain hyperlink and overdue tag " in new TestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe fixedDate.plusDays(35).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)

          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

          findElementById("balancing-charge-type-overdue") shouldBe None
        }
        "have POA data in same table" in new TestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days(dueDates = dueDateOverdue)) {

          val poa1Table: Element = pageDocument.select("tr").get(2)
          poa1Table.select("td").first().text() shouldBe fixedDate.minusDays(10).toLongDateShort
          poa1Table.select("td").get(1).text() shouldBe "Overdue " + poa1Text + s" 1"
          poa1Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa1Table.select("td").last().text() shouldBe "£50.00"

          val poa2Table: Element = pageDocument.select("tr").get(3)
          poa2Table.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDateShort
          poa2Table.select("td").get(1).text() shouldBe "Overdue " + poa2Text + s" 2"
          poa2Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa2Table.select("td").last().text() shouldBe "£75.00"

        }
        "have review and reconcile extra payments with Accrues Interest tags in the same table" in new TestSetup(
          charges = whatYouOweWithReviewReconcileDataNotYetDue) {
          val poaExtra1Table: Element = pageDocument.getElementsByClass("govuk-table__row").get(1)
          poaExtra1Table.select("td").first().text() shouldBe fixedDate.plusYears(100).minusDays(1).toLongDateShort
          poaExtra1Table.select("td").get(1).text() shouldBe "Accrues interest " + poaExtra1Text + " 1"
          poaExtra1Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poaExtra1Table.select("td").last().text() shouldBe "£50.00"

          val poa2ExtraTable: Element = pageDocument.getElementsByClass("govuk-table__row").get(2)
          poa2ExtraTable.select("td").first().text() shouldBe fixedDate.plusYears(100).plusDays(30).toLongDateShort
          poa2ExtraTable.select("td").get(1).text() shouldBe "Accrues interest " + poaExtra2Text + " 2"
          poa2ExtraTable.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa2ExtraTable.select("td").last().text() shouldBe "£75.00"
        }
        "have interest charges for paid reconciliation charges in the same table" in new TestSetup(charges = whatYouOweReconciliationInterestData) {
          val poaExtra1Table: Element = pageDocument.getElementsByClass("govuk-table__row").get(1)
          poaExtra1Table.select("td").first().text() shouldBe interestEndDateFuture.toLongDateShort
          poaExtra1Table.select("td").get(1).text() shouldBe poa1ReconcileInterest + " 1"
          poaExtra1Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poaExtra1Table.select("td").last().text() shouldBe "£100.00"

          val poa2ExtraTable: Element = pageDocument.getElementsByClass("govuk-table__row").get(2)
          poa2ExtraTable.select("td").first().text() shouldBe interestEndDateFuture.toLongDateShort
          poa2ExtraTable.select("td").get(1).text() shouldBe poa2ReconcileInterest + " 2"
          poa2ExtraTable.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa2ExtraTable.select("td").last().text() shouldBe "£40.00"
        }
        "have penalty charges in table" in new TestSetup(whatYouOweAllPenalties, LPP2Url = appConfig.incomeTaxPenaltiesFrontendLPP2Calculation("chargeRefLPP2")) {
          val lpp1Row: Element = pageDocument.getElementsByClass("govuk-table__row").get(3)
          lpp1Row.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lpp1Row.select("td").get(1).text() shouldBe lpp1Text + " 3"
          lpp1Row.select("td").get(1).getElementsByClass("govuk-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(fixedDate.getYear, id1040000123).url
          lpp1Row.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lpp1Row.select("td").last().text() shouldBe "£50.00"

          val lpp2Row: Element = pageDocument.getElementsByClass("govuk-table__row").get(4)
          lpp2Row.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lpp2Row.select("td").get(1).text() shouldBe lpp2Text + " 4"
          lpp2Row.select("td").get(1).getElementsByClass("govuk-link").attr("href") shouldBe appConfig.incomeTaxPenaltiesFrontendLPP2Calculation("chargeRefLPP2")
          lpp2Row.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lpp2Row.select("td").last().text() shouldBe "£75.00"

          val lspRow: Element = pageDocument.getElementsByClass("govuk-table__row").get(1)
          lspRow.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lspRow.select("td").get(1).text() shouldBe lspText + " 1"
          lspRow.select("td").get(1).getElementsByClass("govuk-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(fixedDate.getYear, id1040000123).url
          lspRow.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lspRow.select("td").last().text() shouldBe "£50.00"
        }
        "have interest charges for paid penalties in table" in new TestSetup(whatYouOweAllPenaltiesInterest) {
          val lspRow: Element = pageDocument.getElementsByClass("govuk-table__row").get(1)
          lspRow.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lspRow.select("td").get(1).text() shouldBe lspInterest + " 1"
          lspRow.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lspRow.select("td").last().text() shouldBe "£100.00"

          val lpp1Row: Element = pageDocument.getElementsByClass("govuk-table__row").get(3)
          lpp1Row.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lpp1Row.select("td").get(1).text() shouldBe lpp1Interest + " 3"
          lpp1Row.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lpp1Row.select("td").last().text() shouldBe "£99.00"

          val lpp2Row: Element = pageDocument.getElementsByClass("govuk-table__row").get(4)
          lpp2Row.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          lpp2Row.select("td").get(1).text() shouldBe lpp2Interest + " 4"
          lpp2Row.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          lpp2Row.select("td").last().text() shouldBe "£98.00"
        }

        "should have payment processing bullets when payment due in more than 30 days" in new TestSetup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote
        }

        "display the paragraph about payments under review and bullet points when there is a dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "display bullets and not display the paragraph about payments under review when there are no dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks)) {
          findElementById("payment-under-review-info") shouldBe None


          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "money in your account section with available credits" in new TestSetup(charges = whatYouOweDataWithAvailableCredits()) {
          pageDocument.getElementById("money-in-your-account").text shouldBe messages("whatYouOwe.moneyOnAccount") + " " +
            messages("whatYouOwe.moneyOnAccount-1") + " £300.00" + " " +
            messages("whatYouOwe.moneyOnAccount-2") + " " +
            messages("whatYouOwe.moneyOnAccount-3") + "."
        }

        "money in your account section with no available credits" in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
          findElementById("money-in-your-account") shouldBe None
        }

        "money in your account section with available credits equal to £0 " in new TestSetup(charges = whatYouOweDataWithDataDueIn30DaysAvailableCreditZero()) {
          findElementById("money-in-your-account") shouldBe None
        }

      }

      "the user has charges and access viewer within 30 days of due date" should {
        s"have the Balancing Payment header and table data" in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe chargeType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYear
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe fixedDate.plusDays(30).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)
          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        }

        "have POA data in same table as balancing payment " in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {
          val poa1Table: Element = pageDocument.select("tr").get(2)
          poa1Table.select("td").first().text() shouldBe fixedDate.toLongDateShort
          poa1Table.select("td").get(1).text() shouldBe poa1Text + s" 1"
          poa1Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa1Table.select("td").last().text() shouldBe "£50.00"

          val poa2Table: Element = pageDocument.select("tr").get(3)
          poa2Table.select("td").first().text() shouldBe fixedDate.plusDays(1).toLongDateShort
          poa2Table.select("td").get(1).text() shouldBe poa2Text + s" 2"
          poa2Table.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)

          poa2Table.select("td").last().text() shouldBe "£75.00"

        }

        "have Data for due within 30 days" in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {

          pageDocument.getElementById("due-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124").url
          findElementById("due-0-overdue") shouldBe None
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url
        }

        "have data with POA2 with hyperlink and no overdue" in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {
          pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(fixedDate.getYear, "1040000125").url
          findElementById("due-1-overdue") shouldBe None
        }

        "have payment details and should not contain future payments " +
          "and overdue payment headers" in new TestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {
          pageDocument.getElementById("payment-button").text shouldBe payNow
          pageDocument.getElementById("payment-button").
            attr("href") shouldBe controllers.routes.PaymentController.
            paymentHandoff(5000).url
        }

        "display the paragraph about payments under review when there is a dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }
        "should have payment processing bullets when there is dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks), dunningLock = true) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }
        "not display the paragraph about payments under review when there are no dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {
          findElementById("payment-under-review-info") shouldBe None
        }
        "should have payment processing bullets when there is no dunningLock" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        s"display $paymentUnderReview when there is a dunningLock against a single charge" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(oneDunningLock)(dateService)) {
          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(3)

          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearAndUnderReview
          dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe s"$poa2Text 2"
        }

        "should have payment processing bullets when there is a single charge" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(oneDunningLock)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        s"display $paymentUnderReview when there is a dunningLock against multiple charges" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)(dateService)) {
          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(3)

          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearAndUnderReview
          dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe s"$poa2Text 2 $paymentUnderReview"
        }

        "should have payment processing bullets when there is multiple charge" in new TestSetup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        "display the payment plan content and link" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          val paymentPlan: Element = pageDocument.getElementById("payment-plan")
          paymentPlan.text() shouldBe paymentPlanText
        }
      }

      "the user has charges and access viewer after due date" should {

        "have the mtd payments header, table header and data with Balancing Payment data with no hyperlink but have overdue tag" in new TestSetup(
          charges = whatYouOweDataWithOverdueDataAndInterest()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe chargeType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYear
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe fixedDate.minusDays(30).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe overdueTag + " " + preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)
          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

          val interestTable: Element = pageDocument.select("tr").get(2)
          interestTable.select("td").first().text() shouldBe ""
          interestTable.select("td").get(1).text() shouldBe messages("whatYouOwe.balancingCharge.interest.line1.text") + " " +
            messages("whatYouOwe.balancingCharge.interest.line2.text",
              fixedDate.minusDays(30).toLongDateShort, fixedDate.toLongDateShort)
          interestTable.select("td").get(2).text() shouldBe preMtdPayments(
            (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)
          interestTable.select("td").last().text() shouldBe "£12.67"

          pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe overdueTag
        }
        "have payment type dropdown details and bullet point list" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2" in
          new TestSetup(charges = whatYouOweDataWithOverdueAccruedInterest(List(Some(34.56), None), outstandingAmount = List(0.0,0.0))) {
            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe chargeType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYear
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe s"$overdueTag $latePoa1Text 1"
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              fixedDate.getYear, "1040000124", isInterestCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              fixedDate.getYear).url
          }

        "should have payment processing bullets when there is POA1 charge and lpi on poa 1 of 2" in new TestSetup(charges = whatYouOweDataWithOverdueAccruedInterest(List(Some(34.56), None))) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - LPI Dunning Block" in
          new TestSetup(charges = whatYouOweDataWithOverdueLpiDunningLock(Some(34.56), Some(100.0), outstandingAmount = List(0.0,0.0))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe chargeType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYear
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
              latePoa1Text + s" 1" + " " + paymentUnderReview
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              fixedDate.getYear, "1040000124", isInterestCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("LpiDunningLock").text shouldBe "Payment under review"
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              fixedDate.getYear).url

            pageDocument.getElementById("payments-made").text shouldBe paymentsMade
            val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
            paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
            paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
            pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          }

        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - No LPI Dunning Block" in
          new TestSetup(charges = whatYouOweDataWithOverdueLpiDunningLockZero(Some(34.56), Some(0))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe chargeType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYear
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
              latePoa1Text + s" 1"
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              fixedDate.getYear, "1040000124", isInterestCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              fixedDate.getYear).url

            pageDocument.getElementById("payments-made").text shouldBe paymentsMade
            val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
            paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
            paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
            pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"

          }

        "have overdue payments header, bullet points and data with POA1 charge type and No Late payment interest" in new TestSetup(charges = whatYouOweDataWithOverdueAccruedInterest(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(10).toLongDateShort
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
            poa1Text + s" 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "have overdue payments header, bullet points and data with POA1 charge type" in new TestSetup(charges = whatYouOweDataWithOverdueAccruedInterest(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          /*
                overduePaymentsTableRow1.select("td").first().text() shouldBe toDay.minusDays(10).toLongDateShort
        */
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
            poa1Text + s" 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000124").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }
        "have overdue payments with POA2 charge type with hyperlink and overdue tag" in new TestSetup(charges = whatYouOweDataWithOverdueAccruedInterest(List(None, None))) {
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow2.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDateShort
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" 2"
          overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

          pageDocument.getElementById("due-1-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000125").url
          pageDocument.getElementById("due-1-overdue").text shouldBe overdueTag
        }

        "have accruing interest displayed below each overdue POA" in new TestSetup(charges = whatYouOweDataWithOverdueInterestData(List(None, None))) {

          pageDocument.getElementById("accrued-interest-charge-type-0").text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "2.6")
          pageDocument.getElementById("accrued-interest-amount-due-0").text() shouldBe "£42.50"

          pageDocument.getElementById("accrued-interest-charge-type-1").text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
          pageDocument.getElementById("accrued-interest-amount-due-1").text() shouldBe "£24.05"
        }
        "should have payment processing bullets when there is accruing interest" in new TestSetup(charges = whatYouOweDataWithOverdueInterestData(List(None, None))) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        "only show interest for POA when there is no late Payment Interest" in new TestSetup(charges = whatYouOweDataWithOverdueInterestData(List(Some(34.56), None))) {

          Option(pageDocument.getElementById("accrued-interest-charge-type-0")).isDefined shouldBe false

          pageDocument.getElementById("accrued-interest-charge-type-1").text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
          pageDocument.getElementById("accrued-interest-amount-due-1").text() shouldBe "£24.05"
        }

        "not have a paragraph explaining interest rates when there is no accruing interest" in new TestSetup(charges = whatYouOweDataWithOverdueDataIt()) {
          findElementById(".interest-rate") shouldBe None
        }

        "have payments data with button" in new TestSetup(charges = whatYouOweDataWithOverdueData()) {
          pageDocument.getElementById("payment-button").text shouldBe payNow

          pageDocument.getElementById("payment-button").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

        }

        "display the payment plan content and link" in new TestSetup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          val paymentPlan: Element = pageDocument.getElementById("payment-plan")
          paymentPlan.text() shouldBe paymentPlanText
        }

        "display the paragraph about payments under review when there is a dunningLock" in new TestSetup(
          charges = whatYouOweDataWithOverdueDataIt(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new TestSetup(
          charges = whatYouOweDataWithOverdueDataIt(twoDunningLocks)) {
          findElementById("payment-under-review-info") shouldBe None
        }

        s"display $paymentUnderReview when there is a dunningLock against a single charge" in new TestSetup(
          charges = whatYouOweDataWithOverdueAccruedInterest(List(None, None), oneDunningLock)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$overdueTag $poa2Text 2"
        }

        s"display $paymentUnderReview when there is a dunningLock against multiple charges" in new TestSetup(
          charges = whatYouOweDataWithOverdueAccruedInterest(List(None, None), twoDunningLocks)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$overdueTag $poa2Text 2 $paymentUnderReview"
        }
      }

      "the user has charges and access viewer with mixed dates" should {
        s"have the title $whatYouOweTitle and notes" in new TestSetup(charges = whatYouOweDataWithMixedData1) {
          pageDocument.title() shouldBe whatYouOweTitle
        }
        s"not have MTD payments heading" in new TestSetup(charges = whatYouOweDataWithMixedData1) {
          findElementById("pre-mtd-payments-heading") shouldBe None
        }

        "should have payment processing bullets when there is mixed dates" in new TestSetup(charges = whatYouOweDataWithMixedData1) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        s"have overdue table header, bullet points and data with hyperlink and overdue tag" in new TestSetup(charges = whatYouOweDataWithOverdueMixedData2(List(None, None, None))) {
          val overdueTableHeader: Element = pageDocument.select("tr").first()
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe chargeType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYear
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
          overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDateShort
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" 1"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe fixedDate.plusDays(30).toLongDateShort
          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 2"
          dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
          dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000125").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url

          pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            fixedDate.getYear, "1040000123").url
          findElementById("due-1-overdue") shouldBe None
          pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            fixedDate.getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

      }
      s"have payment data with button" in new TestSetup(charges = whatYouOweDataWithMixedData1) {

        pageDocument.getElementById("payment-button").text shouldBe payNow

        pageDocument.getElementById("payment-button").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(10000).url

        findElementById("pre-mtd-payments-heading") shouldBe None
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {

      s"have the mtd payments, bullets and table header and data with Balancing Payment data with no hyperlink but have overdue tag" in new TestSetup(
        charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe chargeType
        remainingBalanceHeader.select("th").get(2).text() shouldBe taxYear
        remainingBalanceHeader.select("th").last().text() shouldBe amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe fixedDate.minusDays(15).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe overdueTag + " " + preMTDRemainingBalance
        remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
          (fixedDate.getYear - 2).toString, (fixedDate.getYear - 1).toString)
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe overdueTag
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new TestSetup(charges = whatYouOweDataTestActiveWithMixedData2(List(None, None, None, None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").get(0)
        overdueTableHeader.select("th").first().text() shouldBe dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe chargeType
        overdueTableHeader.select("th").get(2).text() shouldBe taxYear
        overdueTableHeader.select("th").last().text() shouldBe amountDue
        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
        overduePaymentsTableRow1.select("td").first().text() shouldBe fixedDate.minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" 1"
        overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe fixedDate.plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" 2"
        dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((fixedDate.getYear - 1).toString, fixedDate.getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          fixedDate.getYear, "1040000125").url
        pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
          fixedDate.getYear).url

        pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          fixedDate.getYear, "1040000123").url
        findElementById("due-1-overdue") shouldBe None
        pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
          fixedDate.getYear).url

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }

      s"have payment data with button" in new TestSetup(charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {

        pageDocument.getElementById("payment-button").text shouldBe payNow

        pageDocument.getElementById("payment-button").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

      }

    }

    "the user has no charges" should {

      s"have the title $whatYouOweTitle and page header and notes" in new TestSetup(charges = noChargesModel) {
        pageDocument.title() shouldBe whatYouOweTitle
        pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
        pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote

      }

      "have the link to their previous Self Assessment online account in the sa-note" in new TestSetup(charges = noChargesModel) {
        verifySelfAssessmentLink()
      }

      "not have button Pay now" in new TestSetup(charges = noChargesModel) {
        findElementById("payment-button") shouldBe None
      }
      "not have payment processing bullets" in new TestSetup(charges = noChargesModel) {
        findElementById("payments-made") shouldBe None
        findElementById("payments-made-bullets") shouldBe None
        findElementById("sa-tax-bill") shouldBe None
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

      }
    }

    "AdjustPaymentsOnAccount is enabled" when {

      "user has a POA that can be adjusted" when {

        val poaModel = ctaViewModel

        "POA is paid off fully should display link with additional content" in new TestSetup(
          charges = whatYouOweDataWithPaidPOAs(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
            pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("whatYouOwe.adjust-poa.paid-2", "2024", "2025")
            pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(false)
            Option(pageDocument.getElementById("adjust-paid-poa-content")).isDefined shouldBe true
        }

        "POA is not paid off fully should display link" in new TestSetup(
          charges = whatYouOweDataWithZeroMoneyInAccount(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
            pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("whatYouOwe.adjust-poa", "2024", "2025")
            pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(false)
            Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
        }
      }

      "user has no POA that can be adjusted should not display link" in new TestSetup(
        charges = whatYouOweDataNoCharges,
        adjustPaymentsOnAccountFSEnabled = true,
        claimToAdjustViewModel = Some(WYOClaimToAdjustViewModel(None)) ) {
        Option(pageDocument.getElementById("adjust-poa-link")) shouldBe None
        Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
      }
    }

    "codingOut is enabled" should {
      "have coding out message displayed at the bottom of the page" in new TestSetup(charges = whatYouOweDataWithCodingOutNics2, taxYear = whatYouOweDataWithCodingOutNics2.codedOutDetails.get.codingTaxYear.endYear) {
        Option(pageDocument.getElementById("coding-out-summary-link")).isDefined shouldBe true
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-year-summary/2021"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
      "have a class 2 Nics overdue entry" in new TestSetup(charges = whatYouOweDataWithCodingOutNics2) {
        Option(pageDocument.getElementById("due-0")).isDefined shouldBe true
        pageDocument.getElementById("due-0").text().contains(CODING_OUT_CLASS2_NICS) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
      }

      "should have payment processing bullets when there is coding out" in new TestSetup(charges = whatYouOweDataWithCodingOutNics2) {

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

      }

      "have a cancelled paye self assessment entry" in new TestSetup(charges = whatYouOweDataWithCancelledPayeSa) {
        findElementById("coding-out-notice") shouldBe None
        pageDocument.getElementById("due-0").text().contains(cancelledPayeSelfAssessment) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        findElementById("coding-out-summary-link") shouldBe None
      }

      "have a Payment on Account 1 entry" in new TestSetup(charges = whatYouOweWithPoaOneCollected, taxYear = whatYouOweWithPoaOneCollected.codedOutDetails.get.codingTaxYear.endYear) {
        pageDocument.getElementById("due-0").text().contains(poa1CollectedCodedOut) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/tax-year-summary/2021"
      }

      "have a Payment on Account 2 entry" in new TestSetup(charges = whatYouOweWithPoaTwoCollected, taxYear = whatYouOweWithPoaTwoCollected.codedOutDetails.get.codingTaxYear.endYear) {
        pageDocument.getElementById("due-0").text().contains(poa2CollectedCodedOut) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/tax-year-summary/2021"
      }
    }

    "At crystallisation, the user has the coding out requested amount fully collected" should {
      "only show coding out content under header" in new TestSetup(charges = whatYouOweDataWithCodingOutFullyCollected, taxYear = whatYouOweDataWithCodingOutFullyCollected.codedOutDetails.get.codingTaxYear.endYear) {
        pageDocument.getElementById("coding-out-notice").text() shouldBe codingOutNoticeFullyCollected
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-year-summary/2021"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
    }
  }

  "MFA Debits is enabled" should {
    "have an HMRC adjustment payment due" in new TestSetup(charges = whatYouOweDataWithMFADebits) {
      pageDocument.title() shouldBe whatYouOweTitle
      pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
      pageDocument.getElementById("due-0").text.contains(hmrcAdjustment)
      pageDocument.select("#due-0 a").get(0).text() shouldBe hmrcAdjustment + s" 1"
      pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
    }
  }

  "agent" when {
    "The What you owe view with financial details model" when {

      s"have the title '${
        messages("htmlTitle.agent", messages("whatYouOwe.heading-agent"))
      }'" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()(dateService)) {
        pageDocument.title() shouldBe messages("htmlTitle.agent", messages("whatYouOwe.heading-agent"))
        pageDocument.getElementById("due-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(fixedDate.getYear, "1040000124").url
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(fixedDate.getYear).url
      }

      "not have button Pay now with no chagres" in new AgentTestSetup(charges = noChargesModel) {
        findAgentElementById("payment-button") shouldBe None
      }
      "not have button Pay now with charges" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        findAgentElementById("payment-button") shouldBe None
      }

      "money in your account section with available credits" in new AgentTestSetup(charges = whatYouOweDataWithAvailableCredits()) {
        pageDocument.getElementById("money-in-your-account").text shouldBe messages("whatYouOwe.moneyOnAccount-agent") + " " +
          messages("whatYouOwe.moneyOnAccount-1") + " £300.00" + " " +
          messages("whatYouOwe.moneyOnAccount-agent-2") + " " +
          messages("whatYouOwe.moneyOnAccount-3") + "."
      }

      "money in your account section with no available credits" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        findAgentElementById("money-in-your-account") shouldBe None
      }

      "money in your account section with an available credit of £0.00" in new AgentTestSetup(charges = whatYouOweDataWithDataDueIn30DaysAvailableCreditZero()) {
        findAgentElementById("money-in-your-account") shouldBe None
      }
    }

    "should have payment processing bullets when there is multiple charge" in new AgentTestSetup(
      charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {

      pageDocument.getElementById("payments-made").text shouldBe paymentsMadeAgent
      val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
      paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBulletAgent1
      paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBulletAgent2
      pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      pageDocument.getElementById("sa-note-migrated").text shouldBe saNoteAgent

    }

    "the user has no charges" should {
      s"have the title ${messages("agent.header.serviceName", messages("whatYouOwe.heading-agent"))} and page header and notes" in new AgentTestSetup(charges = noChargesModel) {
        pageDocument.title() shouldBe messages("htmlTitle.agent", messages("whatYouOwe.heading-agent"))
        pageDocument.selectFirst("h1").text shouldBe whatYouOweAgentHeading
        pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsAgentDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote
      }
    }

      "user has a POA that can be adjusted" when {

        val poaModel = ctaViewModel

        "POA is paid off fully should display link with additional content" in new AgentTestSetup(
          charges = whatYouOweDataWithPaidPOAs(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("whatYouOwe.adjust-poa.paid-2", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(true)
          Option(pageDocument.getElementById("adjust-paid-poa-content")).isDefined shouldBe true
        }

        "POA is not paid off fully should display link" in new AgentTestSetup(
          charges = whatYouOweDataWithZeroMoneyInAccount(),
          adjustPaymentsOnAccountFSEnabled = true,
          claimToAdjustViewModel = Some(poaModel) ) {
          pageDocument.getElementById("adjust-poa-link").text() shouldBe messages("whatYouOwe.adjust-poa", "2024", "2025")
          pageDocument.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustLink(true)
          Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
        }
      }

      "user has no POA that can be adjusted should not display link" in new AgentTestSetup(
        charges = whatYouOweDataNoCharges,
        adjustPaymentsOnAccountFSEnabled = true,
        claimToAdjustViewModel = Some(WYOClaimToAdjustViewModel(None)) ) {
        Option(pageDocument.getElementById("adjust-poa-link")) shouldBe None
        Option(pageDocument.getElementById("adjust-paid-poa-content")) shouldBe None
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
