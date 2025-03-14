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
import models.financialDetails.{BalanceDetails, DocumentDetail, WhatYouOweChargesList}
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
import testConstants.FinancialDetailsTestConstants.{noDunningLocks, outstandingChargesModel}
import testUtils.{TestSupport, ViewSpec}
import views.html.YourSelfAssessmentCharges

import java.time.LocalDate

class YourSelfAssessmentChargesViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec with ChargeConstants{

  val yourSelfAssessmentChargesView: YourSelfAssessmentCharges = app.injector.instanceOf[YourSelfAssessmentCharges]

  val saLink: String = s"${messages("whatYouOwe.sa-link")} ${messages("pagehelp.opensInNewTabText")}"

  def taxYearSummaryText(from: String, to: String): String = s"${messages("whatYouOwe.tax-year-summary.taxYear", from, to)} ${messages("whatYouOwe.taxYear")}"

  def preMtdPayments(from: String, to: String): String = s"${messages("whatYouOwe.pre-mtd-year", from, to)}"


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

    val html: HtmlFormat.Appendable = yourSelfAssessmentChargesView(
      currentDate = dateService.getCurrentDate,
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      reviewAndReconcileEnabled = reviewAndReconcileEnabled,
      creditAndRefundEnabled = true,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel))(FakeRequest(), individualUser, implicitly, dateService)
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

    private val currentDateIs: LocalDate = dateService.getCurrentDate
    val html: HtmlFormat.Appendable = yourSelfAssessmentChargesView(
      currentDateIs,
      hasOverdueOrAccruingInterestCharges = false,
      whatYouOweChargesList = charges,
      hasLpiWithDunningLock = hasLpiWithDunningLock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      reviewAndReconcileEnabled = reviewAndReconcileEnabled,
      creditAndRefundEnabled = true,
      claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(defaultClaimToAdjustViewModel)
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
  val codingOutNotice = s"${messages("whatYouOwe.codingOut-1a")} £43.21 ${messages("whatYouOwe.codingOut-1b")} ${messages("whatYouOwe.codingOut-2", "2020", "2021")} ${messages("whatYouOwe.codingOut-3")}"
  val codingOutNoticeFullyCollected = s"${messages("whatYouOwe.credit-overpaid-prefix")} £0.00 ${messages("whatYouOwe.codingOut-1b")} ${messages("whatYouOwe.codingOut-2", "2020", "2021")} ${messages("whatYouOwe.codingOut-individual")}"

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

}
