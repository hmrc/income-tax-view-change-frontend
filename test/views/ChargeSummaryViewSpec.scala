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

import config.featureswitch.FeatureSwitching
import enums.ChargeType._
import enums.{AdjustmentReversalReason, AmendedReturnReversalReason, CreateReversalReason, CustomerRequestReason}
import exceptions.MissingFieldException
import models.admin.{FilterCodedOutPoas, ReviewAndReconcilePoa}
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargeHistoryModel}
import models.chargeSummary.{ChargeSummaryViewModel, PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.twirl.api.Html
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import testUtils.ViewSpec
import views.html.ChargeSummary

import java.time.LocalDate

class ChargeSummaryViewSpec extends ViewSpec with FeatureSwitching with ChargeConstants {

  lazy val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]
  val whatYouOweAgentUrl: String = controllers.routes.WhatYouOweController.showAgent().url
  val SAChargesAgentUrl: String = controllers.routes.YourSelfAssessmentChargesController.showAgent().url

  import Messages._

  val defaultAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1400, Some(LocalDate.of(2018,3,29)), AdjustmentReversalReason), List())

  class TestSetup(chargeItem: ChargeItem = chargeItemModel(),
                  dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                  paymentBreakdown: List[FinancialDetail] = List(),
                  paymentAllocations: List[PaymentHistoryAllocations] = List(),
                  reviewAndReconcileCredit: Option[ChargeItem] = None,
                  payments: FinancialDetailsModel = payments,
                  chargeHistoryEnabled: Boolean = true,
                  latePaymentInterestCharge: Boolean = false,
                  reviewAndReconcileEnabled: Boolean = false,
                  isAgent: Boolean = false,
                  adjustmentHistory: AdjustmentHistoryModel = defaultAdjustmentHistory,
                  poaExtraChargeLink: Option[String] = None,
                  whatYouOweUrl: String = "/report-quarterly/income-and-expenses/view/what-you-owe",
                  saChargesUrl: String = "/report-quarterly/income-and-expenses/view/your-self-assessment-charges",
                  yourSelfAssessmentChargesFS: Boolean = false) {

    enable(ReviewAndReconcilePoa)

    val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
      currentDate = dateService.getCurrentDate,
      chargeItem = chargeItem.copy(dueDate = dueDate),
      backUrl = "testBackURL",
      gatewayPage = None,
      btaNavPartial = None,
      paymentBreakdown = paymentBreakdown,
      paymentAllocations = paymentAllocations,
      reviewAndReconcileCredit = reviewAndReconcileCredit,
      payments = payments,
      chargeHistoryEnabled = chargeHistoryEnabled,
      latePaymentInterestCharge = latePaymentInterestCharge,
      reviewAndReconcileEnabled = reviewAndReconcileEnabled,
      penaltiesEnabled = true,
      isAgent = isAgent,
      poaOneChargeUrl = "testUrl1",
      poaTwoChargeUrl = "testUrl2",
      adjustmentHistory = adjustmentHistory,
      poaExtraChargeLink = poaExtraChargeLink)

    val view: Html = chargeSummary(viewModel, whatYouOweUrl, saChargesUrl, yourSelfAssessmentChargesFS )
    val document: Document = Jsoup.parse(view.toString())
    def verifySummaryListRowNumeric(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val summaryListRow = document.select(s".govuk-summary-list:nth-of-type(1) .govuk-summary-list__row:nth-of-type($rowNumber)")
      summaryListRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      summaryListRow.select(".govuk-summary-list__value--numeric").text() shouldBe expectedValueText
    }

    def verifyPaymentBreakdownRowNumeric(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val paymentBreakdownRow = document.select(s".govuk-summary-list:nth-of-type(2) .govuk-summary-list__row:nth-of-type($rowNumber)")
      paymentBreakdownRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      paymentBreakdownRow.select(".govuk-summary-list__value--numeric").text() shouldBe expectedValueText
    }

    def verifyPaymentHistoryContent(rows: String*): Assertion = {
      document.select(Selectors.table).text() shouldBe
        s"""
           |Date Description Amount
           |${rows.mkString("\n")}
           |""".stripMargin.trim.linesIterator.mkString(" ")

    }

  }

  def reviewAndReconcileCreditChargeItem(transactionType: TransactionType): Option[ChargeItem] =
    Some(ChargeItem(
      transactionId = "some-id",
      taxYear = TaxYear(2019, 2020),
      transactionType = transactionType,
      codedOutStatus = None,
      documentDate = LocalDate.of(2018, 8, 6),
      dueDate = Some(LocalDate.of(2018, 8, 6)),
      originalAmount = 1000,
      outstandingAmount = 0,
      interestOutstandingAmount = None,
      latePaymentInterestAmount = None,
      interestFromDate = None,
      interestEndDate = None,
      interestRate = None,
      lpiWithDunningLock = None,
      amountCodedOut = None,
      dunningLock = false,
      poaRelevantAmount = None,

    ))

  def paymentsForCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal, clearingSAPDocument: Option[String], clearingId: Option[String]): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(PaymentHistoryAllocation(
        dueDate = Some(LocalDate.parse(date)),
        amount = Some(amount),
        clearingSAPDocument = clearingSAPDocument,
        clearingId = clearingId)),
      chargeMainType = Some(mainType), chargeType = Some(chargeType))

  object Messages {
    val typePOA1 = "SA Payment on Account 1"
    val typePOA2 = "SA Payment on Account 2"
    val typeBalCharge = "SA Balancing Charge"
    val taxYearHeading: String = messages("taxYears.table.taxYear.heading")
    val balancingCharge: String = messages("chargeSummary.balancingCharge.text")
    val paymentBreakdownNic2: String = messages("chargeSummary.paymentBreakdown.nic2")
    val codingOutMessage2017To2018: String = messages("chargeSummary.codingOutBCDMessage", 2017, 2018)
    val codingOutMessage2016To2017WithStringMessagesArgument: String = messages("chargeSummary.codingOutBCDMessage", "2016", "2017")
    val chargeSummaryCodingOutHeading2017To2018: String = s"${messages("chargeSummary.codingOut.text")}"
    val chargeSummaryPoa1CodedOutHeading: String = messages("chargeSummary.poa1CodedOut.text")
    val chargeSummaryPoa2CodedOutHeading: String = messages("chargeSummary.poa2CodedOut.text")
    val insetPara: String = s"${messages("chargeSummary.codingOutInset-1")} ${messages("chargeSummary.codingOutInset-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.codingOutInset-3")}"
    val paymentBreakdownInterestLocksCharging: String = messages("chargeSummary.paymentBreakdown.interestLocks.charging")

    val poaTextParagraph = messages("chargeSummary.paymentsOnAccount")
    val poaTextBullets = messages("chargeSummary.paymentsOnAccount.bullet1") + " " + messages("chargeSummary.paymentsOnAccount.bullet2")
    val poaTextP2 = messages("chargeSummary.paymentsOnAccount.p2")

    val adjustmentText = messages("chargeSummary.definition.hmrcadjustment")

    val lpiPoa1TextSentence = messages("chargeSummary.lpi.paymentsOnAccount.poa1")
    val lpiPoa2TextSentence = messages("chargeSummary.lpi.paymentsOnAccount.poa2")
    val lpiPoaTextParagraph = messages("chargeSummary.lpi.paymentsOnAccount.textOne") + " " + messages("chargeSummary.lpi.paymentsOnAccount.linkText") + " " + messages("chargeSummary.lpi.paymentsOnAccount.textTwo")
    val lpiPoaTextP3 = messages("chargeSummary.lpi.paymentsOnAccount.p3") + " " + messages("chargeSummary.lpi.paymentsOnAccount.p3LinkText")

    val poa1ReconciliationInterestP1 = messages("chargeSummary.poa1ExtraAmountInterest.p1")
    val poa1ReconciliationInterestP2 = messages("chargeSummary.poa1ExtraAmountInterest.p2")
    val poa1ReconciliationInterestP3 = messages("chargeSummary.poa1ExtraAmountInterest.p3") + " " +  messages("chargeSummary.poa1ExtraAmountInterest.p3LinkText") + " " + messages("chargeSummary.poa1ExtraAmountInterest.p3AfterLink")

    val poa2ReconciliationInterestP1 = messages("chargeSummary.poa2ExtraAmountInterest.p1")
    val poa2ReconciliationInterestP2 = messages("chargeSummary.poa2ExtraAmountInterest.p2")
    val poa2ReconciliationInterestP3 = messages("chargeSummary.poa2ExtraAmountInterest.p3") + " " +  messages("chargeSummary.poa2ExtraAmountInterest.p3LinkText") + " " + messages("chargeSummary.poa2ExtraAmountInterest.p3AfterLink")

    val poaExtraChargeText1: String = messages("chargeSummary.extraCharge.text1")
    val poaExtraChargeTextLink: String = messages("chargeSummary.extraCharge.linkText")
    val poaExtraChargeText2: String = messages("chargeSummary.extraCharge.text2")

    val bcdTextParagraph = messages("chargeSummary.definition.balancingcharge.p1")
    val bcdTextBullets = messages("chargeSummary.definition.balancingcharge.bullet1") + " " + messages("chargeSummary.definition.balancingcharge.bullet2")
    val bcdTextP2 = messages("chargeSummary.definition.balancingcharge.p2")

    val lpiBcdTextSentence = messages("chargeSummary.lpi.balancingCharge.p1")
    val lpiBcdTextParagraph = messages("chargeSummary.lpi.balancingCharge.textOne") + " " + messages("chargeSummary.lpi.balancingCharge.linkText") + " " + messages("chargeSummary.lpi.balancingCharge.textTwo")
    val lpiBcdTextP3 = messages("chargeSummary.lpi.balancingCharge.p3") + " " + messages("chargeSummary.lpi.balancingCharge.p3LinkText")


    def poaHeading(year: Int, number: Int) = s"${year - 1} to $year tax year ${getFirstOrSecond(number)} payment on account"
    def poa1Caption(year: Int) = s"${year - 1} to $year tax year"

    def poa2Heading = s"Second payment on account"

    def getFirstOrSecond(number: Int): String = {
      require(number > 0, "Number must be greater than zero")
      number match {
        case 1 => "First"
        case 2 => "Second"
        case _=> throw new Error(s"Number must be 1 or 2 but got: $number")
      }
    }

    def poa1InterestHeading = s"Late payment interest on first payment on account"

    def poa2InterestHeading = s"Late payment interest on second payment on account"

    def poa1ReconcileHeading = s"First payment on account: extra amount from your tax return"

    def poa2ReconcileHeading = s"Second payment on account: extra amount from your tax return"

    def poa1ReconcileInterestHeading = s"Interest for first payment on account: extra amount"

    def poa2ReconcileInterestHeading = s"Interest for second payment on account: extra amount"

    def poa1ReconciliationCreditHeading = s"First payment on account: credit from your tax return"

    def poa2ReconciliationCreditHeading = s"Second payment on account: credit from your tax return"

    def balancingChargeHeading = s"$balancingCharge"

    def balancingChargeInterestHeading = s"${messages("chargeSummary.lpi.balancingCharge.text")}"

    def class2NicHeading = s"$paymentBreakdownNic2"

    def cancelledSaPayeHeading = s"${messages("chargeSummary.cancelledPayeSelfAssessment.text")}"

    val dueDate: String = messages("chargeSummary.dueDate")
    val interestPeriod: String = messages("chargeSummary.lpi.interestPeriod")
    val fullPaymentAmount: String = messages("chargeSummary.paymentAmount")
    val paymentAmount: String = messages("chargeSummary.paymentAmountCodingOut")
    val amount: String = messages("chargeSummary.chargeHistory.amount")
    val remainingToPay: String = messages("chargeSummary.remainingDue")
    val paymentBreakdownHeading: String = messages("chargeSummary.paymentBreakdown.heading")
    val chargeHistoryHeadingGeneric: String = messages("chargeSummary.chargeHistory.heading")
    val chargeHistoryHeadingPoa1: String = messages("chargeSummary.chargeHistory.Poa1heading")
    val chargeHistoryHeadingPoa2: String = messages("chargeSummary.chargeHistory.Poa2heading")
    val historyRowPOA1Created: String = s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.paymentOnAccount1.text")} £1,400.00"
    val codingOutHeader: String = "2017 to 2018 tax year PAYE self assessment"
    val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("pagehelp.opensInNewTabText")}"
    val paymentprocessingbullet1Agent: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2-agent")} ${messages("pagehelp.opensInNewTabText")}"

    def paymentOnAccountCreated(number: Int) = messages(s"chargeSummary.chargeHistory.created.paymentOnAccount$number.text")

    def paymentOnAccountInterestCreated(number: Int) = s"Late payment interest for payment on account $number of 2 created"

    val hmrcCreated: String = messages("chargeSummary.lpi.chargeHistory.created.reviewAndReconcilePoa1.text")

    val balancingChargeCreated: String = messages("chargeSummary.chargeHistory.created.balancingCharge.text")
    val balancingChargeInterestCreated: String = messages("chargeSummary.lpi.chargeHistory.created.balancingCharge.text")

    def paymentOnAccountAmended(number: Int) = s"Payment on account $number of 2 reduced due to amended return"

    def firstPoaAdjusted = "You updated your first payment on account"
    def secondPoaAdjusted = "You updated your second payment on account"

    val balancingChargeAmended: String = messages("chargeSummary.chargeHistory.amend.balancingCharge.text")

    def paymentOnAccountRequest(number: Int) = s"Payment on account $number of 2 reduced by taxpayer request"

    def class2NicTaxYear(year: Int) = messages("chargeSummary.nic2TaxYear", (year - 1).toString, year.toString)

    val class2NicChargeCreated: String = messages("chargeSummary.chargeHistory.created.class2Nic.text")
    val cancelledSaPayeCreated: String = messages("chargeSummary.chargeHistory.created.cancelledPayeSelfAssessment.text")

    def payeTaxCodeText(year: Int) = s"${messages("chargeSummary.check-paye-tax-code-1")} ${messages("chargeSummary.check-paye-tax-code-2")} ${messages("chargeSummary.check-paye-tax-code-3", year - 1, year)}"

    def payeTaxCodeTextWithStringMessage(year: Int) = s"${messages("chargeSummary.check-paye-tax-code-1")} ${messages("chargeSummary.check-paye-tax-code-2")} ${messages("chargeSummary.check-paye-tax-code-3", (year - 1).toString, year.toString)}"

    val payeTaxCodeLink = s"https://www.tax.service.gov.uk/check-income-tax/tax-codes/${getCurrentTaxYearEnd.getYear}"
    val cancelledPayeTaxCodeInsetText = s"${messages("chargeSummary.cancelledPayeInset-1")} ${messages("chargeSummary.cancelledPayeInset-2")} ${messages("pagehelp.opensInNewTabText")}. ${messages("chargeSummary.cancelledPayeInset-3")}"
    val cancelledPayeTaxCodeInsetLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"

    def remainingTaxYouOwe(year: Int) = messages("chargeSummary.codingOutBCDMessage", year - 1, year)

    val balancingChargeRequest: String = messages("chargeSummary.chargeHistory.request.balancingCharge.text")
    val dunningLockBannerHeader: String = messages("chargeSummary.dunning.locks.banner.title")
    val dunningLockBannerLink: String = s"${messages("chargeSummary.dunning.locks.banner.linkText")} ${messages("pagehelp.opensInNewTabText")}."
    val interestLinkFirstWord: String = messages("chargeSummary.whatYouOwe.textOne")
    val interestLinkFirstWordAgent: String = messages("chargeSummary.whatYouOwe.textOne-agent")
    val interestLinkText: String = messages("chargeSummary.whatYouOwe.linkText")
    val interestSALinkText: String = messages("chargeSummary.selfAssessmentCharges.linkText")
    val interestLinkTextAgent: String = messages("chargeSummary.whatYouOwe.linkText-agent")
    val interestSALinkTextAgent: String = messages("chargeSummary.selfAssessmentCharges.linkText-agent")
    val interestLinkFullText: String = messages("chargeSummary.interestLocks.text")
    val interestLinkFullTextAgent: String = messages("chargeSummary.interestLocks.text-agent")
    val cancelledPAYESelfAssessment: String = messages("whatYouOwe.cancelled-paye-sa.heading")
    val incomeTax: String = messages("chargeSummary.check-paye-tax-code-1")

    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink ${messages("chargeSummary.dunning.locks.banner.note", s"$formattedAmount", s"$date")}"
  }

  val amendedChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500, LocalDate.of(2018, 7, 6), "amended return", Some("001"))
  val amendedAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), AdjustmentReversalReason))
  )
  val adjustmentHistoryWithBalancingCharge: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), AmendedReturnReversalReason))
  )
  val customerRequestChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500, LocalDate.of(2018, 7, 6), "Customer Request", Some("002"))
  val customerRequestAdjustmentHistory : AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), CustomerRequestReason))
  )

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, additionalSubItems = Seq(SubItem(
      amount = Some(500.0),
      dueDate = Some(LocalDate.parse("2018-09-08")),
      clearingDate = Some(LocalDate.parse("2018-09-07")),
      clearingSAPDocument = Some("123456789012"),
      paymentAmount = Some(500.0),
      paymentLot = None,
      paymentLotItem = None)
    )),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB),
    financialDetail(originalAmount = 3456.78, chargeType = VOLUNTARY_NIC2_NI),
    financialDetail(originalAmount = 5678.9, chargeType = NIC4_WALES),
    financialDetail(originalAmount = 9876.54, chargeType = CGT),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWithDunningLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWithInterestLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT, interestLock = Some("Manual RPI Signal"), accruedInterest = Some(35)),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWhenInterestAccrues: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithPreviouslyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"), accruedInterest = Some(30))
  )

  val paymentBreakdownWithMixedLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, dunningLock = Some("Stand over order"), interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithOnlyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB)
  )

  val paymentBreakdownWithOnlyInterestLock: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"))
  )

  def subItemWithClearingSapDocument(clearingSAPDocument: String): SubItem = SubItem(dueDate = Some(LocalDate.parse("2017-08-07")), clearingSAPDocument = Some(clearingSAPDocument), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))

  val payments: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    documentDetails = List(DocumentDetail(9999, "PAYID01", Some("Payment on Account"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, Some("lotItem"), Some("lot"))),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYID01"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))))
  )

  def checkPaymentProcessingInfo(document: Document): Unit = {
    document.select("#payment-days-note").text() shouldBe messages("chargeSummary.payment-days-note")
    document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe
      s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")}"
    document.select("#payment-processing-bullets li:nth-child(2)").text() shouldBe messages("chargeSummary.payments-bullet2")
  }

  "user is an individual" when {

    "render the row for the charge" should {
      "charge is a Review and Reconcile credit for Payment on Account 1" in new TestSetup(
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaOneReconciliationCredit)
      ) {
        document.selectById("rar-due-date").text() shouldBe("6 Aug 2018")
        document.selectById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(2020, "some-id").url
        document.selectById("rar-total-amount").text() shouldBe "£1,000.00"
      }
      "charge is a Review and Reconcile credit for Payment on Account 2" in new TestSetup(
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaTwoReconciliationCredit)
      ) {
        document.selectById("rar-due-date").text() shouldBe ("6 Aug 2018")
        document.selectById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(2020, "some-id").url
        document.selectById("rar-total-amount").text() shouldBe "£1,000.00"
      }
    }

    "charge is a POA1" when {

      val basePoaOneDebit = chargeItemModel(transactionType = PoaOneDebit)

      "no late payment interest" should {

        "have the correct heading and caption" in new TestSetup(
          chargeItem = basePoaOneDebit
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe "First payment on account"
        }

        "have content explaining the definition of a payment on account when charge is a POA1" in new TestSetup(
          chargeItem = basePoaOneDebit
        ) {
          document.select("#charge-explanation>:nth-child(1)").text() shouldBe poaTextParagraph
          document.select("#charge-explanation>:nth-child(2)").text() shouldBe poaTextBullets
          document.select("#charge-explanation>:nth-child(3)").text() shouldBe poaTextP2
        }

        "display only the charge creation item when no history found for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0)
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
          document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
          document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
        }


        "display the correct message for an amended charge for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0),
          adjustmentHistory = amendedAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe firstPoaAdjusted
          document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
        }

        "display the correct message for a customer requested change for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0),
          adjustmentHistory = customerRequestAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(1)
        }
      }

      "late payment interest" should {

        "have the correct heading and caption for a late interest charge" in new TestSetup(
          chargeItem = basePoaOneDebit,
          latePaymentInterestCharge = true
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe poa1InterestHeading
        }

        "have content explaining the definition of a late payment interest charge on account when charge is a POA1" in new TestSetup(
          chargeItem = basePoaOneDebit,
          latePaymentInterestCharge = true
        ) {
          document.selectById("lpi-poa2").text() shouldBe lpiPoa1TextSentence
          document.selectById("lpi-poa5").text() shouldBe lpiPoaTextParagraph
          document.selectById("lpi-poa6").text() shouldBe lpiPoaTextP3
        }
      }

      "have the correct caption" in new TestSetup(
        chargeItem = basePoaOneDebit
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      }

      "display only the charge creation item for a payment on account 1 of 2 late interest charge" in new TestSetup(
        basePoaOneDebit.copy(outstandingAmount = 0),
        latePaymentInterestCharge = true
      ) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
      }

      "charge is coded out" when {
        "coding out is accepted" should {
          "display the accepted coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
            disable(FilterCodedOutPoas)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa1CodedOutHeading
              document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
              document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 1
              document.select(".govuk-table tbody tr").size() shouldBe 1
              document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2018", "2019")} £2,500.00"
            }
          }
        }
        "coding out is fully collected" should {
          "display the fully collected coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(FullyCollected), originalAmount = 2500.00)
            disable(FilterCodedOutPoas)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa1CodedOutHeading
              document.getElementById("charge-explanation").child(0).text() shouldBe "Payments on account are 2 advance payments made towards your next tax bill. They pay for:"
              document.getElementById("charge-explanation").child(1).child(0).text() shouldBe "Income Tax"
              document.getElementById("charge-explanation").child(1).child(1).text() shouldBe "Class 4 National Insurance contributions (opens in new tab)"
              document.getElementById("charge-explanation").child(1).child(1).link.attr("href") shouldBe "https://www.gov.uk/self-employed-national-insurance-rates"
              document.getElementById("charge-explanation").child(2).text() shouldBe "HMRC estimates the total amount based on your previous year’s tax bill. Each payment is half of that amount."
              document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
              document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 1
              document.select(".govuk-table tbody tr").size() shouldBe 1
              document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2018", "2019")} £2,500.00"
            }
          }
        }
      }
    }

    "charge is a POA2" when {

      val basePoaTwoDebit = chargeItemModel(transactionType = PoaTwoDebit)

      "no late payment interest" should {
        "have the correct heading and caption for a POA 2" in new TestSetup(
          chargeItem = basePoaTwoDebit
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe poa2Heading
        }

        "have content explaining the definition of a payment on account when charge is a POA2" in new TestSetup(
          chargeItem = basePoaTwoDebit
        ) {
          document.select("#charge-explanation>:nth-child(1)").text() shouldBe poaTextParagraph
          document.select("#charge-explanation>:nth-child(2)").text() shouldBe poaTextBullets
          document.select("#charge-explanation>:nth-child(3)").text() shouldBe poaTextP2
        }

        "display only the charge creation item when no history found for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0)
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(2)
        }

        "display the correct message for an amended charge for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0),
          adjustmentHistory = amendedAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe secondPoaAdjusted
        }

        "display the correct message for a customer requested change for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0),
          adjustmentHistory = customerRequestAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(2)
        }
      }


      "late payment interest" should {

        "have the correct heading aand caption for a POA 2 late interest charge" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe poa2InterestHeading
        }

        "have content explaining the definition of a late payment interest charge on account when charge is a POA2" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.selectById("lpi-poa4").text() shouldBe lpiPoa2TextSentence
          document.selectById("lpi-poa5").text() shouldBe lpiPoaTextParagraph
          document.selectById("lpi-poa6").text() shouldBe lpiPoaTextP3
        }

        "display only the charge creation item for a payment on account 2 of 2 late interest charge" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(2)
        }

        "have a link to extra charge if it is a poa with an extra charge" in new TestSetup(chargeItem = basePoaTwoDebit, poaExtraChargeLink = Some("testLink")) {
          document.select("#poa-extra-charge-content").text() shouldBe s"$poaExtraChargeText1 $poaExtraChargeTextLink $poaExtraChargeText2"
          document.select("#poa-extra-charge-link").attr("href") shouldBe "testLink"
          document.select("#poa-extra-charge-link").text() shouldBe poaExtraChargeTextLink
        }
        "not have this extra poa charge content if there is no such charge" in new TestSetup(chargeItem = basePoaTwoDebit) {
          document.doesNotHave(Selectors.id("poa-extra-charge-content"))
        }
      }

      "charge is coded out" when {
        "coding out is accepted" should {
          "display the coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
            disable(FilterCodedOutPoas)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa2CodedOutHeading
              document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
              document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 1
              document.select(".govuk-table tbody tr").size() shouldBe 1
              document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2018", "2019")} £2,500.00"
            }
          }
        }
        "coding out is fully collected" should {
          "display the fully collected coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(FullyCollected), originalAmount = 2500.00)
            disable(FilterCodedOutPoas)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa2CodedOutHeading
              document.getElementById("charge-explanation").child(0).text() shouldBe "Payments on account are 2 advance payments made towards your next tax bill. They pay for:"
              document.getElementById("charge-explanation").child(1).child(0).text() shouldBe "Income Tax"
              document.getElementById("charge-explanation").child(1).child(1).text() shouldBe "Class 4 National Insurance contributions (opens in new tab)"
              document.getElementById("charge-explanation").child(1).child(1).link.attr("href") shouldBe "https://www.gov.uk/self-employed-national-insurance-rates"
              document.getElementById("charge-explanation").child(2).text() shouldBe "HMRC estimates the total amount based on your previous year’s tax bill. Each payment is half of that amount."
              document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
              document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 1
              document.select(".govuk-table tbody tr").size() shouldBe 1
              document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2018", "2019")} £2,500.00"
            }
          }
        }

        "coding out is cancelled" should {
          "display the coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Cancelled))
            disable(FilterCodedOutPoas)

            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe cancelledSaPayeHeading
              document.select("#charge-explanation > p:nth-child(1)").text() shouldBe poaTextParagraph
              document.select("#charge-explanation>:nth-child(2)").text() shouldBe poaTextBullets
              document.select("#charge-explanation > p:nth-child(3)").text() shouldBe poaTextP2
              document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
              document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
              document.select("#cancelled-coding-out-notice").text() shouldBe cancelledPayeTaxCodeInsetText
              document.select(".govuk-table").size() shouldBe 1
              document.select(".govuk-table tbody tr").size() shouldBe 1
              document.select(".govuk-table tbody tr").get(0).text() shouldBe
                s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.cancelledPayeSelfAssessment.text", "2018", "2019")} £1,400.00"
            }
          }
        }
      }
    }

    "charge is a POA 1 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaOneReconciliationDebit), reviewAndReconcileEnabled = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa1ReconcileHeading
    }
    "charge is a POA 2 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaTwoReconciliationDebit), reviewAndReconcileEnabled = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa2ReconcileHeading
    }
    "charge is interest for a POA 1 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaOneReconciliationDebit), reviewAndReconcileEnabled = true, latePaymentInterestCharge = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa1ReconcileInterestHeading

      document.selectById("poa1-extra-charge-p1").text() shouldBe poa1ReconciliationInterestP1
      document.selectById("poa1-extra-charge-p2").text() shouldBe poa1ReconciliationInterestP2
      document.selectById("poa1-extra-charge-p3").text() shouldBe poa1ReconciliationInterestP3

      verifySummaryListRowNumeric(1, dueDate, "Overdue 15 June 2018")
      verifySummaryListRowNumeric(2, interestPeriod, "29 Mar 2018 to 15 Jun 2018")
      verifySummaryListRowNumeric(3, amount, "£100.00")
      verifySummaryListRowNumeric(4, remainingToPay, "£80.00")

      document.select("tbody tr").size() shouldBe 1
      document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
      document.select("tbody tr td:nth-child(2)").text() shouldBe hmrcCreated
      document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
    }
    "charge is interest for a POA 2 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaTwoReconciliationDebit), reviewAndReconcileEnabled = true, latePaymentInterestCharge = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa2ReconcileInterestHeading

      document.selectById("poa2-extra-charge-p1").text() shouldBe poa2ReconciliationInterestP1
      document.selectById("poa2-extra-charge-p2").text() shouldBe poa2ReconciliationInterestP2
      document.selectById("poa2-extra-charge-p3").text() shouldBe poa2ReconciliationInterestP3

      verifySummaryListRowNumeric(1, dueDate, "Overdue 15 June 2018")
      verifySummaryListRowNumeric(2, interestPeriod, "29 Mar 2018 to 15 Jun 2018")
      verifySummaryListRowNumeric(3, amount, "£100.00")
      verifySummaryListRowNumeric(4, remainingToPay, "£80.00")

      document.select("tbody tr").size() shouldBe 1
      document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
      document.select("tbody tr td:nth-child(2)").text() shouldBe hmrcCreated
      document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
    }

    "charge is a POA 1 reconciliation credit" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaOneReconciliationCredit, originalAmount = -100), reviewAndReconcileEnabled = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa1ReconciliationCreditHeading

      document.selectById("rar-credit-explanation").text shouldBe "HMRC has added a credit to your account because your tax return shows that your adjusted first payment on account was too high."

      document.selectById("allocation").text() shouldBe "Allocation"

      document.selectById("heading-row").text() shouldBe "Where your money went Date Amount"
      document.selectById("table-row-1").text() shouldBe "First payment on account 2017 to 2018 tax year 15 May 2019 £100.00"

      document.selectById("poa-allocation-link").attr("href") shouldBe "testUrl1"
    }
    "charge is a POA 2 reconciliation credit" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaTwoReconciliationCredit, originalAmount = -100), reviewAndReconcileEnabled = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa2ReconciliationCreditHeading

      document.selectById("rar-credit-explanation").text shouldBe "HMRC has added a credit to your account because your tax return shows that your adjusted second payment on account was too high."

      document.selectById("allocation").text() shouldBe "Allocation"

      document.selectById("heading-row").text() shouldBe "Where your money went Date Amount"
      document.selectById("table-row-1").text() shouldBe "Second payment on account 2017 to 2018 tax year 15 May 2019 £100.00"

      document.selectById("poa-allocation-link").attr("href") shouldBe "testUrl2"
    }

    "charge is a balancing payment" when {

      val baseBalancing = chargeItemModel(transactionType = BalancingCharge)
      val baseBalancingNics2 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2))
      val baseBalancingAccepted = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
      val baseBalancingCancelled = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled))

      "is Nics2" should {

        s"have the correct heading snd caption for a $paymentBreakdownNic2 charge with a sub-transaction type" in new TestSetup(
          chargeItem = baseBalancingNics2
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe class2NicHeading
        }

        s"have the correct heading and caption for a $paymentBreakdownNic2 charge with no sub-transaction type" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = BalancingCharge)
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe balancingChargeHeading
        }

        "have a paragraph explaining which tax year the Class 2 NIC is for" in new TestSetup(
          chargeItem = baseBalancingNics2.copy(lpiWithDunningLock = None)
        ) {
          document.select("#nic2TaxYear").text() shouldBe class2NicTaxYear(2018)
        }

        s"display only the charge creation item for a $paymentBreakdownNic2 charge" in new TestSetup(
          chargeItem = baseBalancingNics2
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(2)").text() shouldBe class2NicChargeCreated
        }

      }

      "is accepted" should {

        val creditItemCodingOut = baseBalancingAccepted.copy(
          transactionId = "CODINGOUT02")

        "display the coded out details" when {

          "Coding Out is Enabled" in new TestSetup(creditItemCodingOut) {
            document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
            document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
            document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
            document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
            document.select("#coding-out-notice").text() shouldBe insetPara
            document.select("#coding-out-message").text() shouldBe codingOutMessage2016To2017WithStringMessagesArgument
            document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
            document.select(".govuk-table").size() shouldBe 1
            document.select(".govuk-table tbody tr").size() shouldBe 1
            document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2018", "2019")} £2,500.00"
          }
        }

        "Scenario were Class2 NICs has been paid and only coding out information needs to be displayed" when {

          "Coding Out is Enabled" in new TestSetup(
            creditItemCodingOut
          ) {
            document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
            document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
            document.select("#coding-out-notice").text() shouldBe insetPara
            document.select("#coding-out-message").text() shouldBe codingOutMessage2016To2017WithStringMessagesArgument
            document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
            document.selectById("paymentAmount").text() shouldBe "Payment amount £2,500.00"
            document.selectById("codingOutRemainingToPay").text() shouldBe messages("chargeSummary.codingOutRemainingToPay", "2018", "2019")
            document.select(".govuk-table tbody tr").size() shouldBe 1
          }

           "Coding Out is Disabled" in new TestSetup(
            creditItemCodingOut.copy(
              taxYear = TaxYear.forYearEnd(2019),
              codedOutStatus = None,
              outstandingAmount = 2500.00,
              originalAmount = 2500.00)
          ) {
             document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
             document.select("h1").text() shouldBe s"$balancingCharge"
            verifySummaryListRowNumeric(1, dueDate, "Overdue 15 May 2019")
            verifySummaryListRowNumeric(2, fullPaymentAmount, "£2,500.00")
            verifySummaryListRowNumeric(3, remainingToPay, "£2,500.00")
            document.select("#coding-out-notice").text() shouldBe ""
            document.select("#coding-out-message").text() shouldBe ""
            document.select("#coding-out-notice-link").attr("href") shouldBe ""
            document.select(".govuk-table").size() shouldBe 1
            document.select(".govuk-table tbody tr").size() shouldBe 1
          }
        }
      }

      "is cancelled" should {
        "have the correct heading and caption for a Cancelled PAYE Self Assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe cancelledSaPayeHeading
        }

        "have a paragraphs explaining Cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None)
        ) {
          document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
          document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
          document.select("#cancelled-coding-out-notice").text() shouldBe cancelledPayeTaxCodeInsetText
          document.select("#cancelled-coding-out-notice a").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
        }

        "not display the Payment breakdown list for cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None),
          paymentBreakdown = Nil
        ) {
          document.doesNotHave(Selectors.id("heading-payment-breakdown"))
        }

        "display a payment history" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None),
          paymentBreakdown = paymentBreakdown
        ) {
          document.select("main h2").text shouldBe chargeHistoryHeadingGeneric
         }

        "display only the charge creation item when no history found for cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None)
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
          document.select("tbody tr td:nth-child(2)").text() shouldBe cancelledSaPayeCreated
          document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
        }
      }

      "have the correct heading and caption for a new balancing charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019))) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeHeading
      }

      "have content explaining the definition of a balancing charge when charge is a balancing charge" in new TestSetup(
        chargeItem = baseBalancing) {
        document.select("#charge-explanation>:nth-child(1)").text() shouldBe bcdTextParagraph
        document.select("#charge-explanation>:nth-child(2)").text() shouldBe bcdTextBullets
        document.select("#charge-explanation>:nth-child(3)").text() shouldBe bcdTextP2
      }

      "have content explaining the definition of a late balancing charge when charge is a balancing charge" in new TestSetup(
        chargeItem = baseBalancing,
        latePaymentInterestCharge = true) {
        document.selectById("lpi-bcd1").text() shouldBe lpiBcdTextSentence
        document.selectById("lpi-bcd2").text() shouldBe lpiBcdTextParagraph
        document.selectById("lpi-bcd3").text() shouldBe lpiBcdTextP3
      }

      "have the correct heading and caption for a new balancing charge late interest charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019)),
        latePaymentInterestCharge = true
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeInterestHeading
      }

      "have the correct heading and caption for an amend balancing charge late interest charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019)),
        latePaymentInterestCharge = true) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeInterestHeading
      }
      
      "display balancing charge due date as N/A and hide sections - Payment Breakdown," +
        "Any payments you make, Payment History when balancing charge is 0" in new TestSetup(
          baseBalancing.copy(
              taxYear = TaxYear.forYearEnd(2018),
              outstandingAmount = 0,
              originalAmount = BigDecimal(0),
              documentDate = LocalDate.of(2018, 3, 29),
              lpiWithDunningLock = None
            )) {
          document.select(".govuk-summary-list").text() shouldBe "Due date N/A Amount £0.00 Still to pay £0.00"
          document.select("p").get(3).text shouldBe "View what you owe to check if you have any other charges to pay."
          document.select("#payment-history-table").isEmpty shouldBe true
          document.select("#heading-payment-breakdown").isEmpty shouldBe true
          document.select(s"#payment-link-${chargeItemModel().taxYear}").isEmpty shouldBe true
          document.select("#payment-days-note").isEmpty shouldBe true
        }
      }

    "charge is a MFA debit" should {

      val mfaChargeItem = chargeItemModel(transactionType = MfaDebitCharge)

      val paymentAllocations = List(
        paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
        paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("123456789012"), Some("PAYID01")),
      )
      "have content explaining the definition of a HMRC adjustment when charge is a MFA Debit" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = MfaDebitCharge)) {
        document.select("#charge-explanation>:nth-child(1)").text() shouldBe adjustmentText
      }

      "Display an unpaid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019))
      ) {
        val summaryListText = "Due date Overdue 15 May 2019 Amount £1,400.00 Still to pay £1,400.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be the same as payment amount
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // payment history should show only "HMRC adjustment created"
        document.select("#payment-history-table tr").size shouldBe 2
        document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
      }

      "Display a paid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019), outstandingAmount = 0.0),
        paymentAllocations = paymentAllocations
      ) {
        val summaryListText = "Due date 15 May 2019 Amount £1,400.00 Still to pay £0.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,500.00"
        val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,600.00"
        val allocationLinkHref = "/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=PAYID01"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be zero
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // payment history should show two rows "HMRC adjustment created" and "payment put towards HMRC Adjustment"
        document.select("#payment-history-table tr").size shouldBe 4
        document.select("#payment-history-table tr:nth-child(1)").text() shouldBe paymentHistoryText
        document.select("#payment-history-table tr:nth-child(2)").text() shouldBe MFADebitAllocation1
        document.select("#payment-history-table tr:nth-child(3)").text() shouldBe MFADebitAllocation2
        // allocation link should be to agent payment allocation page
        document.select("#payment-history-table tr:nth-child(3) a").attr("href") shouldBe allocationLinkHref
      }
    }

    "payment breakdown" should {
      "not display a notification banner when there are no dunning locks" in new TestSetup(
        chargeItem = chargeItemModel().copy(lpiWithDunningLock = None),
        paymentBreakdown = paymentBreakdown
      ) {
        document.doesNotHave(Selectors.id("dunningLocksBanner"))
      }

      "display a notification banner when there are dunning locks" which {

        s"has the '${dunningLockBannerHeader}' heading" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithDunningLocks
        ) {
          document.selectById("dunningLocksBanner")
            .select(Selectors.h2).text() shouldBe dunningLockBannerHeader
        }

        "has the link for Payment under review which opens in new tab" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithDunningLocks
        ) {
          val link: Elements = document.selectById("dunningLocksBanner").select(Selectors.link)

          link.text() shouldBe dunningLockBannerLink
          link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          link.attr("target") shouldBe "_blank"
        }

        "shows the same remaining amount and a due date as in the charge summary list" which {
          "display a remaining amount" in new TestSetup(
            chargeItem = chargeItemModel(outstandingAmount = 1600),
            paymentBreakdown = paymentBreakdownWithDunningLocks
          ) {
            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£1,600.00", "15 May 2019")
          }

          "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new TestSetup(
            chargeItem = chargeItemModel().copy(outstandingAmount = 0),
            paymentBreakdown = paymentBreakdownWithDunningLocks
          ) {
            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£0.00", "15 May 2019")
          }
        }
      }
    }

    "have a fallback link" in new TestSetup() {
      document.hasFallbackBacklink()
    }

    "displaying charge amount" when {
      "when no late interest charge, display original amount" in new TestSetup(
        chargeItem = chargeItemModel(originalAmount = 1500),
      ) {
        verifySummaryListRowNumeric(2, fullPaymentAmount, "£1,500.00")
      }

      "when a late interest charge display late payment interest amount " in new TestSetup(
        chargeItem = chargeItemModel(
          originalAmount = 1500,
          latePaymentInterestAmount = Some(100.0)),
        latePaymentInterestCharge = true
      ) {
        verifySummaryListRowNumeric(3, fullPaymentAmount, "£100.00")
      }
    }

    "displaying due date" when {

      "when no interest charge, display due date" in new TestSetup(dueDate = Option(LocalDate.of(2019, 5, 15))) {
        verifySummaryListRowNumeric(1, dueDate, "Overdue 15 May 2019")
      }

      "no due date, display as N/A" in new TestSetup(
        chargeItem = chargeItemModel(dueDate = None, lpiWithDunningLock = None),
        dueDate = None)
      {
        verifySummaryListRowNumeric(1, dueDate, "N/A")
      }

      "an interest charge, display interest end date" in new TestSetup(
        chargeItem = chargeItemModel(interestEndDate = Option(LocalDate.of(2018, 6, 15))),
        dueDate = Option(LocalDate.of(2019, 5, 15)),
        latePaymentInterestCharge = true
      ){
        verifySummaryListRowNumeric(1, dueDate, "Overdue 15 June 2018")
      }
    }

    "display remaining amount" when {
      "there is an outstanding amount" in new TestSetup(
        chargeItem = chargeItemModel(outstandingAmount = 1600)) {
        verifySummaryListRowNumeric(3, remainingToPay, "£1,600.00")
      }

      "an outstanding amount on a late interest charge" in new TestSetup(
        chargeItem = chargeItemModel(outstandingAmount = 1600),
        latePaymentInterestCharge = true
      ) {
        verifySummaryListRowNumeric(4, remainingToPay, "£80.00")
      }

      "cleared amount == original amount and outstanding amount is not, display a remaining amount of 0" in new TestSetup(
        chargeItem = chargeItemModel(outstandingAmount = 0)) {
        verifySummaryListRowNumeric(3, remainingToPay, "£0.00")
      }
    }

    "display an interest period for a late interest charge" in new TestSetup(
      chargeItem = chargeItemModel(originalAmount = 1500),
      latePaymentInterestCharge = true) {
      verifySummaryListRowNumeric(2, interestPeriod, "29 Mar 2018 to 15 Jun 2018")
    }

    "not display the Payment breakdown list when payments breakdown is empty" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = Nil) {
      document.doesNotHave(Selectors.id("heading-payment-breakdown"))
    }

    "display the Payment breakdown list" which {

      "has a correct heading and caption" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
        document.selectById("heading-payment-breakdown").text shouldBe paymentBreakdownHeading
      }

      "has payment rows with charge types and original amounts" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ) {
        verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
        verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67")
        verifyPaymentBreakdownRowNumeric(3, "Voluntary Class 2 National Insurance", "£3,456.78")
        verifyPaymentBreakdownRowNumeric(4, "Class 4 National Insurance", "£5,678.90")
        verifyPaymentBreakdownRowNumeric(5, "Capital Gains Tax", "£9,876.54")
        verifyPaymentBreakdownRowNumeric(6, "Student Loans", "£543.21")
      }

      "has payment rows with Under review note when there are dunning locks on a payment" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdownWithDunningLocks
      ) {
        verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
        verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67 Under review")
        verifyPaymentBreakdownRowNumeric(3, "Capital Gains Tax", "£9,876.54 Under review")
        verifyPaymentBreakdownRowNumeric(4, "Student Loans", "£543.21")
      }

      "has a payment row with Under review note when there is a dunning lock on a lpi charge" in new TestSetup(
        chargeItem = chargeItemModel(),
        latePaymentInterestCharge = true
      ) {
        verifyPaymentBreakdownRowNumeric(1, "Late payment interest", "£100.00 Under review")
        verifyPaymentBreakdownRowNumeric(2, "", "")
      }

      "has at least one record with an interest lock" which {

        "has payment rows with Under review note when there are dunning locks on a payment" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithMixedLocks
        ) {
          verifyPaymentBreakdownRowNumeric(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
          verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.dunningLocks.underReview")} ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
        }

        "has payment rows with appropriate messages for each row" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithInterestLocks
        ) {
          verifyPaymentBreakdownRowNumeric(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
          verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
          verifyPaymentBreakdownRowNumeric(3, "Capital Gains Tax", s"£9,876.54 ${messages("chargeSummary.paymentBreakdown.interestLocks.previouslyCharged")}")
          verifyPaymentBreakdownRowNumeric(4, "Student Loans", s"£543.21 $paymentBreakdownInterestLocksCharging")
        }

      }
      "has no records that have an interest lock applied" should {
        "has payment rows but no interest lock message when there are no interest locks but there's accrued interest on a payment" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest
        ) {
          verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67")
        }
      }
    }


    "have a interest lock payment link when the interest is accruing" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWhenInterestAccrues
    ) {
      document.select("#what-you-owe-interest-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-interest-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      document.select("#p-interest-locks-msg").text().contains(s"$interestLinkFirstWord $interestLinkText $interestLinkFullText") shouldBe true
    }

    "have a interest lock payment link when the interest has previously" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest
    ) {
      document.select("#what-you-owe-interest-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-interest-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
    }

    "have no interest lock payment link when there is no accrued interest" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
    }

    "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyInterestLock
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
    }

    "have a interest lock payment link when the interest is accruing and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWhenInterestAccrues,
      yourSelfAssessmentChargesFS = true
    ) {
      document.select("#SAChargesInterestLink").text() shouldBe interestSALinkText
      document.select("#SAChargesInterestLink").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
      document.select("#p-interest-locks-msg").text().contains(s"$interestLinkFirstWord $interestSALinkText $interestLinkFullText") shouldBe true
    }

    "have a interest lock payment link when the interest has previously and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest,
      yourSelfAssessmentChargesFS = true
    ) {
      document.select("#SAChargesInterestLink").text() shouldBe interestSALinkText
      document.select("#SAChargesInterestLink").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
      document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestSALinkText $interestLinkFullText"
    }

    "have no interest lock payment link when there is no accrued interest and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest,
      yourSelfAssessmentChargesFS = true
    ) {
      document.select("#SAChargesLink").text() shouldBe interestSALinkText
      document.select("#SAChargesLink").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
    }

    "have no interest lock payment link when there is an intererst lock but no accrued interest and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyInterestLock,
      yourSelfAssessmentChargesFS = true
    ) {
      document.select("#SAChargesLink").text() shouldBe interestSALinkText
      document.select("#SAChargesLink").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
    }

    "charge history" should {
      "display a charge history heading as an h2 when there is no Payment Breakdown" in new TestSetup(
        chargeItem = chargeItemModel().copy(outstandingAmount = 0, lpiWithDunningLock = None)
      ) {
        document.select("main h2").text shouldBe chargeHistoryHeadingPoa1
      }

      "display a charge history heading as an h3 when there is a Payment Breakdown" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ) {
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa1
      }

      "display charge history heading as poa1 heading when charge is a poa1" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa1
      }

      "display charge history heading as poa2 heading when charge is a poa2" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = PoaTwoDebit),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa2
      }

      "display charge history heading as non-poa heading when charge is not a poa" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = MfaDebitCharge),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("main h3").text shouldBe chargeHistoryHeadingGeneric
      }

      "display charge history heading as late payment interest history when charge is a late payment interest" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = PoaTwoDebit),
        paymentBreakdown = paymentBreakdown,
        latePaymentInterestCharge = true
      ){
        document.select("main h3").text shouldBe "Late payment interest history"
      }

      "show payment allocations in history table" when {

        "allocations are present in the list" when {

          val paymentAllocations = List(
            paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
            paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("223456789012"), Some("PAYID01")),

            paymentsForCharge(typePOA2, ITSA_WALES, "2018-04-01", 2400.0, Some("323456789012"), Some("PAYID01")),
            paymentsForCharge(typePOA2, NIC4_GB, "2018-04-15", 2500.0, Some("423456789012"), Some("PAYID01")),

            paymentsForCharge(typeBalCharge, ITSA_ENGLAND_AND_NI, "2019-12-10", 3400.0, Some("523456789012"), Some("PAYID01")),
            paymentsForCharge(typeBalCharge, NIC4_NI, "2019-12-11", 3500.0, Some("623456789012"), Some("PAYID01")),
            paymentsForCharge(typeBalCharge, NIC2_WALES, "2019-12-12", 3600.0, Some("723456789012"), Some("PAYID01")),
            paymentsForCharge(typeBalCharge, CGT, "2019-12-13", 3700.0, Some("823456789012"), Some("PAYID01")),
            paymentsForCharge(typeBalCharge, SL, "2019-12-14", 3800.0, Some("923456789012"), Some("PAYID01")),
            paymentsForCharge(typeBalCharge, VOLUNTARY_NIC2_GB, "2019-12-15", 3900.0, Some("023456789012"), Some("PAYID01")),
          )

          val expectedPaymentAllocationRows = List(
            "30 Mar 2018 Payment allocated to Income Tax for first payment on account 2018 £1,500.00",
            "31 Mar 2018 Payment allocated to Class 4 National Insurance for first payment on account 2018 £1,600.00",
            "1 Apr 2018 Payment allocated to Income Tax for second payment on account 2018 £2,400.00",
            "15 Apr 2018 Payment allocated to Class 4 National Insurance for second payment on account 2018 £2,500.00",
            "10 Dec 2019 Payment allocated to Income Tax for Balancing payment 2018 £3,400.00",
            "11 Dec 2019 Payment allocated to Class 4 National Insurance for Balancing payment 2018 £3,500.00",
            "12 Dec 2019 Payment allocated to Class 2 National Insurance for Balancing payment 2018 £3,600.00",
            "13 Dec 2019 Payment allocated to Capital Gains Tax for Balancing payment 2018 £3,700.00",
            "14 Dec 2019 Payment allocated to Student Loans for Balancing payment 2018 £3,800.00",
            "15 Dec 2019 Payment allocated to Voluntary Class 2 National Insurance for Balancing payment 2018 £3,900.00"
          )

          "chargeHistory enabled, having Payment created in the first row" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = true, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(historyRowPOA1Created :: expectedPaymentAllocationRows: _*)
          }

          "chargeHistory enabled with a matching link to the payment allocations page" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = true, paymentAllocations = paymentAllocations) {
            document.select(Selectors.table).select("a").size shouldBe 10
            document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.routes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url) shouldBe true
          }

          "chargeHistory disabled" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = false, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(expectedPaymentAllocationRows: _*)
          }
        }
      }

      "hide payment allocations in history table" when {
        "the allocations list is empty" when {
          "chargeHistory enabled, having Payment created in the first row" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = true, paymentAllocations = Nil) {
            verifyPaymentHistoryContent(historyRowPOA1Created)
          }

          "chargeHistory disabled, not showing the table at all" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = false, paymentAllocations = Nil) {
            (document select Selectors.table).size shouldBe 0
          }
        }
      }
    }
  }

  "The charge summary view when missing mandatory expected fields" should {
    "throw a MissingFieldException" in new TestSetup(chargeItem = chargeItemModel()) {
      val exceptionViewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
        currentDate = dateService.getCurrentDate,
        chargeItem = chargeItemModel(dueDate = None),
        backUrl = "testBackURL",
        gatewayPage = None,
        btaNavPartial = None,
        paymentBreakdown = paymentBreakdown,
        paymentAllocations = List(),
        payments = payments,
        chargeHistoryEnabled = true,
        latePaymentInterestCharge = false,
        reviewAndReconcileCredit = None,
        reviewAndReconcileEnabled = false,
        penaltiesEnabled = true,
        poaOneChargeUrl = "",
        poaTwoChargeUrl = "",
        adjustmentHistory = defaultAdjustmentHistory)
      val thrownException = intercept[MissingFieldException] {

        chargeSummary(exceptionViewModel, "/report-quarterly/income-and-expenses/view/what-you-owe", "/report-quarterly/income-and-expenses/view/your-self-assessment-charges", false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
    }
  }

  "agent" when {

    "render the row for the charge" should {
      "charge is a Review and Reconcile credit for Payment on Account 1" in new TestSetup(
        isAgent = true,
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaOneReconciliationCredit)
      ) {
        document.selectById("rar-due-date").text() shouldBe ("6 Aug 2018")
        document.selectById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(2020, "some-id").url
        document.selectById("rar-total-amount").text() shouldBe "£1,000.00"
      }
      "charge is a Review and Reconcile credit for Payment on Account 2" in new TestSetup(
        isAgent = true,
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaTwoReconciliationCredit)
      ) {
        document.selectById("rar-due-date").text() shouldBe ("6 Aug 2018")
        document.selectById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(2020, "some-id").url
        document.selectById("rar-total-amount").text() shouldBe "£1,000.00"
      }
    }

    "The charge summary view" should {

      "have a interest lock payment link when the interest is accruing" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true,
        whatYouOweUrl = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes") {
        document.select("#what-you-owe-interest-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-interest-link-agent").attr("href") shouldBe whatYouOweAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestLinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have a interest lock payment link when the interest has previously" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true,
        whatYouOweUrl = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes") {
        document.select("#what-you-owe-interest-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-interest-link-agent").attr("href") shouldBe whatYouOweAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestLinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have no interest lock payment link when there is no accrued interest" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true,
        whatYouOweUrl = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes") {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true,
        whatYouOweUrl = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes") {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have a interest lock payment link when the interest is accruing and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true,
        saChargesUrl = "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges", yourSelfAssessmentChargesFS = true) {
        document.select("#SAChargesAgentInterestLink").text() shouldBe interestSALinkTextAgent
        document.select("#SAChargesAgentInterestLink").attr("href") shouldBe SAChargesAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestSALinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have a interest lock payment link when the interest has previously and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true,
        saChargesUrl = "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges", yourSelfAssessmentChargesFS = true) {
        document.select("#SAChargesAgentInterestLink").text() shouldBe interestSALinkTextAgent
        document.select("#SAChargesAgentInterestLink").attr("href") shouldBe SAChargesAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestSALinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have no interest lock payment link when there is no accrued interest and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true,
        saChargesUrl = "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges", yourSelfAssessmentChargesFS = true) {
        document.select("#SAChargesAgentLink").text() shouldBe interestSALinkTextAgent
        document.select("#SAChargesAgentLink").attr("href") shouldBe SAChargesAgentUrl
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest and have SACharges links when yourSelfAssessmentChargesFS is true" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true,
        saChargesUrl = "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges", yourSelfAssessmentChargesFS = true) {
        document.select("#SAChargesAgentLink").text() shouldBe interestSALinkTextAgent
        document.select("#SAChargesAgentLink").attr("href") shouldBe SAChargesAgentUrl
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new TestSetup(chargeItem = chargeItemModel(), paymentBreakdown = paymentBreakdown, isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "not have a payment link when there is an outstanding amount of 0" in new TestSetup(
        chargeItem = chargeItemModel(outstandingAmount = 0.0),
        isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "list payment allocations with right number of rows and agent payment allocations link" in new TestSetup(chargeItem = chargeItemModel(),
        chargeHistoryEnabled = true, paymentAllocations = List(
          paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01"))), isAgent = true) {
        document.select(Selectors.table).select("a").size shouldBe 1
        document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent("PAYID01").url) shouldBe true
      }
    }

    "MFA Credits" when {

      val paymentAllocations = List(
        paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
        paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("123456789012"), Some("PAYID01"))
      )

      val mfaChargeItem = chargeItemModel(transactionType = MfaDebitCharge)

      "Display an unpaid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019)),
        isAgent = true) {
        val summaryListText = "Due date Overdue 15 May 2019 Amount £1,400.00 Still to pay £1,400.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be the same as payment amount
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // payment history should show only "HMRC adjustment created"
        document.select("#payment-history-table tr").size shouldBe 2
        document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
      }

      "Display a paid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019), outstandingAmount = 0.0), isAgent = true,
        paymentAllocations = paymentAllocations) {
        val summaryListText = "Due date 15 May 2019 Amount £1,400.00 Still to pay £0.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,500.00"
        val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,600.00"
        val allocationLinkHref = "/report-quarterly/income-and-expenses/view/agents/payment-made-to-hmrc?documentNumber=PAYID01"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be zero
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // payment history should show two rows "HMRC adjustment created" and "payment put towards HMRC Adjustment"
        document.select("#payment-history-table tr").size shouldBe 4

        document.select("#payment-history-table tr:nth-child(1)").text() shouldBe paymentHistoryText
        document.select("#payment-history-table tr:nth-child(2)").text() shouldBe MFADebitAllocation1
        document.select("#payment-history-table tr:nth-child(3)").text() shouldBe MFADebitAllocation2
        // allocation link should be to agent payment allocation page
        document.select("#payment-history-table tr:nth-child(3) a").attr("href") shouldBe allocationLinkHref
      }
    }
  }
}
