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
import enums.CodingOutType._
import enums.GatewayPage.GatewayPage
import enums.OtherCharge
import exceptions.MissingFieldException
import junit.extensions.TestSetup
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargeHistoryModel}
import models.chargeSummary.{ChargeSummaryViewModel, PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.Mockito.mock
import org.scalatest.Assertion
import play.twirl.api.Html
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.FinancialDetailsTestConstants._
import testUtils.ViewSpec
import views.html.ChargeSummary

import java.time.LocalDate

class ChargeSummaryViewSpec extends ViewSpec with FeatureSwitching {

  lazy val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]
  val whatYouOweAgentUrl = controllers.routes.WhatYouOweController.showAgent.url

  import Messages._

  val defaultAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1400, Some(LocalDate.of(2018,3,29)), "adjustment"), List())

  class TestSetup(documentDetail: DocumentDetail,
                  dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                  paymentBreakdown: List[FinancialDetail] = List(),
                  paymentAllocations: List[PaymentHistoryAllocations] = List(),
                  payments: FinancialDetailsModel = payments,
                  chargeHistoryEnabled: Boolean = true,
                  paymentAllocationEnabled: Boolean = false,
                  latePaymentInterestCharge: Boolean = false,
                  codingOutEnabled: Boolean = false,
                  isAgent: Boolean = false,
                  isMFADebit: Boolean = false,
                  adjustmentHistory: AdjustmentHistoryModel = defaultAdjustmentHistory) {
    val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
      currentDate = dateService.getCurrentDate,
      documentDetailWithDueDate = DocumentDetailWithDueDate(documentDetail, dueDate),
      backUrl = "testBackURL",
      gatewayPage = None,
      btaNavPartial = None,
      paymentBreakdown = paymentBreakdown,
      paymentAllocations = paymentAllocations,
      payments = payments,
      chargeHistoryEnabled = chargeHistoryEnabled,
      paymentAllocationEnabled = paymentAllocationEnabled,
      latePaymentInterestCharge = latePaymentInterestCharge,
      codingOutEnabled = codingOutEnabled,
      isAgent = isAgent,
      isMFADebit  = isMFADebit,
      adjustmentHistory = adjustmentHistory,
      documentType =  documentDetail.getDocType)
    val view: Html = chargeSummary(viewModel)
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
    val codingOutMessage2017To2018: String = messages("chargeSummary.codingOutMessage", 2017, 2018)
    val codingOutMessage2017To2018WithStringMessagesArgument: String = messages("chargeSummary.codingOutMessage", "2017", "2018")
    val chargeSummaryCodingOutHeading2017To2018: String = s"$taxYearHeading 6 April 2017 to 5 April 2018 ${messages("chargeSummary.codingOut.text")}"
    val insetPara: String = s"${messages("chargeSummary.codingOutInset-1")} ${messages("chargeSummary.codingOutInset-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.codingOutInset-3")}"
    val paymentBreakdownInterestLocksCharging: String = messages("chargeSummary.paymentBreakdown.interestLocks.charging")

    val poaTextParagraph = messages("chargeSummary.paymentsOnAccount")
    val poaTextBullets = messages("chargeSummary.paymentsOnAccount.bullet1") + " " + messages("chargeSummary.paymentsOnAccount.bullet2")
    val poaTextP2 = messages("chargeSummary.paymentsOnAccount.p2")

    def poaHeading(year: Int, number: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year ${getFirstOrSecond(number)} payment on account"

    def getFirstOrSecond(number: Int): String = {
      require(number > 0, "Number must be greater than zero")
      number match {
        case 1 => "First"
        case 2 => "Second"
        case _=> throw new Error(s"Number must be 1 or 2 but got: $number")
      }
    }
    def poaInterestHeading(year: Int, number: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year Late payment interest on payment on account $number of 2"

    def balancingChargeHeading(year: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year $balancingCharge"

    def balancingChargeInterestHeading(year: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year ${messages("chargeSummary.lpi.balancingCharge.text")}"

    def class2NicHeading(year: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year $paymentBreakdownNic2"

    def cancelledSaPayeHeading(year: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year ${messages("chargeSummary.cancelledPayeSelfAssessment.text")}"

    val dueDate: String = messages("chargeSummary.dueDate")
    val interestPeriod: String = messages("chargeSummary.lpi.interestPeriod")
    val fullPaymentAmount: String = messages("chargeSummary.paymentAmount")
    val paymentAmount: String = messages("chargeSummary.paymentAmountCodingOut")
    val remainingToPay: String = messages("chargeSummary.remainingDue")
    val paymentBreakdownHeading: String = messages("chargeSummary.paymentBreakdown.heading")
    val chargeHistoryHeadingGeneric: String = messages("chargeSummary.chargeHistory.heading")
    val chargeHistoryHeadingPoa1: String = messages("chargeSummary.chargeHistory.Poa1heading")
    val chargeHistoryHeadingPoa2: String = messages("chargeSummary.chargeHistory.Poa2heading")
    val historyRowPOA1Created: String = s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.paymentOnAccount1.text")} £1,400.00"
    val codingOutHeader: String = s"$taxYearHeading ${messages("taxYears.taxYears", "6 April 2017", "5 April 2018")} PAYE self assessment"
    val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("pagehelp.opensInNewTabText")}"
    val paymentprocessingbullet1Agent: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2-agent")} ${messages("pagehelp.opensInNewTabText")}"

    def paymentOnAccountCreated(number: Int) = messages(s"chargeSummary.chargeHistory.created.paymentOnAccount$number.text")

    def paymentOnAccountInterestCreated(number: Int) = s"Late payment interest for payment on account $number of 2 created"

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
    val cancellledPayeTaxCodeInsetLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"

    def remainingTaxYouOwe(year: Int) = messages("chargeSummary.codingOutMessage", year - 1, year)

    val balancingChargeRequest: String = messages("chargeSummary.chargeHistory.request.balancingCharge.text")
    val dunningLockBannerHeader: String = messages("chargeSummary.dunning.locks.banner.title")
    val dunningLockBannerLink: String = s"${messages("chargeSummary.dunning.locks.banner.linkText")} ${messages("pagehelp.opensInNewTabText")}."
    val interestLinkFirstWord: String = messages("chargeSummary.whatYouOwe.textOne")
    val interestLinkFirstWordAgent: String = messages("chargeSummary.whatYouOwe.textOne-agent")
    val interestLinkText: String = messages("chargeSummary.whatYouOwe.linkText")
    val interestLinkTextAgent: String = messages("chargeSummary.whatYouOwe.linkText-agent")
    val interestLinkFullText: String = messages("chargeSummary.interestLocks.text")
    val interestLinkFullTextAgent: String = messages("chargeSummary.interestLocks.text-agent")
    val cancelledPAYESelfAssessment: String = messages("whatYouOwe.cancelled-paye-sa.heading")

    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink ${messages("chargeSummary.dunning.locks.banner.note", s"$formattedAmount", s"$date")}"
  }

  val amendedChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500, LocalDate.of(2018, 7, 6), "amended return", Some("001"))
  val amendedAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, "create"),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), "adjustment"))
  )
  val adjustmentHistoryWithBalancingCharge: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, "create"),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), "amend"))
  )
  val customerRequestChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500, LocalDate.of(2018, 7, 6), "Customer Request", Some("002"))
  val customerRequestAdjustmentHistory : AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, "create"),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), "request"))
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

  "individual" when {

    "The charge summary view" should {

      "have a fallback link" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.hasFallbackBacklinkTo("testBackURL")
      }

      "have the correct heading for a POA 1" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.select("h1").text() shouldBe poaHeading(2018, 1)
      }

      "have the correct heading for a POA 2" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA - POA 2"))) {
        document.select("h1").text() shouldBe poaHeading(2018, 2)
      }

      "have the correct heading for a new balancing charge" in new TestSetup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge"))) {
        document.select("h1").text() shouldBe balancingChargeHeading(2019)
      }

      "have the correct heading for an amend balancing charge" in new TestSetup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge"))) {
        document.select("h1").text() shouldBe balancingChargeHeading(2019)
      }

      "have the correct heading for a POA 1 late interest charge" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe poaInterestHeading(2018, 1)
      }

      "have the correct heading for a POA 2 late interest charge" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe poaInterestHeading(2018, 2)
      }

      s"have the correct heading for a $paymentBreakdownNic2 charge when coding out FS is enabled" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2)), codingOutEnabled = true) {
        document.select("h1").text() shouldBe class2NicHeading(2018)
      }

      s"have the correct heading for a $paymentBreakdownNic2 charge when coding out FS is disabled" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge")), codingOutEnabled = false) {
        document.select("h1").text() shouldBe balancingChargeHeading(2018)
      }

      "have a paragraph explaining which tax year the Class 2 NIC is for" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2), lpiWithDunningLock = None), codingOutEnabled = true) {
        document.select("#main-content p:nth-child(2)").text() shouldBe class2NicTaxYear(2018)
      }

      s"have the correct heading for a Cancelled PAYE Self Assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("h1").text() shouldBe cancelledSaPayeHeading(2018)
      }

      "have a paragraphs explaining Cancelled PAYE self assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading")), lpiWithDunningLock = None), codingOutEnabled = true) {
        document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
        document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
        document.select("#cancelled-coding-out-notice").text() shouldBe cancelledPayeTaxCodeInsetText
        document.select("#cancelled-coding-out-notice a").attr("href") shouldBe cancellledPayeTaxCodeInsetLink

      }

      "have content explaining the definition of a payment on account when charge is a POA1" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.selectById("p1").text() shouldBe poaTextParagraph
        document.selectById("bullets").text() shouldBe poaTextBullets
        document.selectById("p2").text() shouldBe poaTextP2
      }

      "have content explaining the definition of a payment on account when charge is a POA2" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA - POA 2"))) {
        document.selectById("p1").text() shouldBe poaTextParagraph
        document.selectById("bullets").text() shouldBe poaTextBullets
        document.selectById("p2").text() shouldBe poaTextP2
      }

      "display a due date, payment amount and remaining to pay for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        verifySummaryListRowNumeric(1, dueDate, "OVERDUE 15 May 2019")
        verifySummaryListRowNumeric(2, paymentAmount, "£1,400.00")
        verifySummaryListRowNumeric(3, remainingToPay, "£1,400.00")
      }

      "have a paragraph explaining how many days a payment can take to process for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1
      }

      "what you page link with text for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "not display the Payment breakdown list for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "have payment link for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CANCELLED)), codingOutEnabled = true) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "display a payment history" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading")), lpiWithDunningLock = None), paymentBreakdown = paymentBreakdown, codingOutEnabled = true) {
        document.select("main h2").text shouldBe chargeHistoryHeadingGeneric
       }

      "display only the charge creation item when no history found for cancelled PAYE self assessment" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe cancelledSaPayeCreated
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "have the correct heading for a new balancing charge late interest charge" in new TestSetup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe balancingChargeInterestHeading(2019)
      }

      "have the correct heading for an amend balancing charge late interest charge" in new TestSetup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe balancingChargeInterestHeading(2019)
      }

      "not display a notification banner when there are no dunning locks in payment breakdown" in new TestSetup(
        documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdown) {

        document.doesNotHave(Selectors.id("dunningLocksBanner"))
      }

      "display a notification banner when there are dunning locks in payment breakdown" which {

        s"has the '${dunningLockBannerHeader}' heading" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          document.selectById("dunningLocksBanner")
            .select(Selectors.h2).text() shouldBe dunningLockBannerHeader
        }

        "has the link for Payment under review which opens in new tab" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          val link: Elements = document.selectById("dunningLocksBanner").select(Selectors.link)

          link.text() shouldBe dunningLockBannerLink
          link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          link.attr("target") shouldBe "_blank"
        }

        "shows the same remaining amount and a due date as in the charge summary list" which {
          "display a remaining amount" in new TestSetup(documentDetailModel(
            outstandingAmount = 1600), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£1,600.00", "15 May 2019")
          }

          "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new TestSetup(
            documentDetailModel(outstandingAmount = 0), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£0.00", "15 May 2019")
          }
        }
      }

      "display a due date" in new TestSetup(documentDetailModel()) {
        verifySummaryListRowNumeric(1, dueDate, "OVERDUE 15 May 2019")
      }

      "display a due date as N/A" in new TestSetup(documentDetail = documentDetailModel(documentDueDate = None, lpiWithDunningLock = None), dueDate = None) {
        verifySummaryListRowNumeric(1, dueDate, "N/A")
      }

      "display the correct due date for an interest charge" in new TestSetup(documentDetailModel(), latePaymentInterestCharge = true) {
        verifySummaryListRowNumeric(1, dueDate, "OVERDUE 15 June 2018")
      }

      "display an interest period for a late interest charge" in new TestSetup(documentDetailModel(originalAmount = 1500), latePaymentInterestCharge = true) {
        verifySummaryListRowNumeric(2, interestPeriod, "29 Mar 2018 to 15 Jun 2018")
      }

      "display a charge amount" in new TestSetup(documentDetailModel(originalAmount = 1500)) {
        verifySummaryListRowNumeric(2, fullPaymentAmount, "£1,500.00")
      }

      "display a charge amount for a late interest charge" in new TestSetup(documentDetailModel(originalAmount = 1500), latePaymentInterestCharge = true) {
        verifySummaryListRowNumeric(3, fullPaymentAmount, "£100.00")
      }

      "display a remaining amount" in new TestSetup(documentDetailModel(outstandingAmount = 1600)) {
        verifySummaryListRowNumeric(3, remainingToPay, "£1,600.00")
      }

      "display a remaining amount for a late interest charge" in new TestSetup(documentDetailModel(outstandingAmount = 1600), latePaymentInterestCharge = true) {
        verifySummaryListRowNumeric(4, remainingToPay, "£80.00")
      }

      "display a remaining amount of 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new TestSetup(documentDetailModel(outstandingAmount = 0)) {
        verifySummaryListRowNumeric(3, remainingToPay, "£0.00")
      }

      "not display the Payment breakdown list when payments breakdown is empty" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "display the Payment breakdown list" which {

        "has a correct heading" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          document.selectById("heading-payment-breakdown").text shouldBe paymentBreakdownHeading
        }

        "has payment rows with charge types and original amounts" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67")
          verifyPaymentBreakdownRowNumeric(3, "Voluntary Class 2 National Insurance", "£3,456.78")
          verifyPaymentBreakdownRowNumeric(4, "Class 4 National Insurance", "£5,678.90")
          verifyPaymentBreakdownRowNumeric(5, "Capital Gains Tax", "£9,876.54")
          verifyPaymentBreakdownRowNumeric(6, "Student Loans", "£543.21")
        }

        "has payment rows with Under review note when there are dunning locks on a payment" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67 Under review")
          verifyPaymentBreakdownRowNumeric(3, "Capital Gains Tax", "£9,876.54 Under review")
          verifyPaymentBreakdownRowNumeric(4, "Student Loans", "£543.21")
        }

        "has a payment row with Under review note when there is a dunning lock on a lpi charge" in new TestSetup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
          verifyPaymentBreakdownRowNumeric(1, "Late payment interest", "£100.00 Under review")
          verifyPaymentBreakdownRowNumeric(2, "", "")
        }

        "has at least one record with an interest lock" which {

          "has payment rows with Under review note when there are dunning locks on a payment" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithMixedLocks) {
            verifyPaymentBreakdownRowNumeric(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
            verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.dunningLocks.underReview")} ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
          }

          "has payment rows with appropriate messages for each row" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithInterestLocks) {
            verifyPaymentBreakdownRowNumeric(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
            verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
            verifyPaymentBreakdownRowNumeric(3, "Capital Gains Tax", s"£9,876.54 ${messages("chargeSummary.paymentBreakdown.interestLocks.previouslyCharged")}")
            verifyPaymentBreakdownRowNumeric(4, "Student Loans", s"£543.21 $paymentBreakdownInterestLocksCharging")
          }

        }
        "has no records that have an interest lock applied" should {
          "has payment rows but no interest lock message when there are no interest locks but there's accrued interest on a payment" in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
            verifyPaymentBreakdownRowNumeric(1, "Income Tax", "£123.45")
            verifyPaymentBreakdownRowNumeric(2, paymentBreakdownNic2, "£2,345.67")
          }
        }
      }

      "have a payment link when an outstanding amount is to be paid" in new TestSetup(documentDetailModel()) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "have a payment processing information section" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), isAgent = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1Agent
      }

      "have a interest lock payment link when the interest is accruing" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "have a interest lock payment link when the interest has previously" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "have no interest lock payment link when there is no accrued interest" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "not have a payment link when there is an outstanding amount of 0" in new TestSetup(documentDetailModel(outstandingAmount = 0)) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "display a charge history heading as an h2 when there is no Payment Breakdown" in new TestSetup(
        documentDetailModel(lpiWithDunningLock = None, outstandingAmount = 0)) {
        document.select("main h2").text shouldBe chargeHistoryHeadingPoa1
      }

      "display a charge history heading as an h3 when there is a Payment Breakdown" in new TestSetup(
        documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa1
      }

      "display charge history heading as poa1 heading when charge is a poa1" in new TestSetup(
        documentDetailModel(), paymentBreakdown = paymentBreakdown){
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa1
      }
      "display charge history heading as poa2 heading when charge is a poa2" in new TestSetup(
        documentDetailModel(documentDescription = Some("ITSA - POA 2")), paymentBreakdown = paymentBreakdown){
        document.select("main h3").text shouldBe chargeHistoryHeadingPoa2
      }
      "display charge history heading as non-poa heading when charge is not a poa" in new TestSetup(
        documentDetailModel(documentDescription = Some("Other")), paymentBreakdown = paymentBreakdown){
        document.select("main h3").text shouldBe chargeHistoryHeadingGeneric
      }


      "display only the charge creation item when no history found for a payment on account 1 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0)) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display only the charge creation item when no history found for a payment on account 2 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("ITSA - POA 2"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(2)
      }

      "display only the charge creation item when no history found for a new balancing charge" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("TRM New Charge"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe balancingChargeCreated
      }

      s"display only the charge creation item for a $paymentBreakdownNic2 charge" in new TestSetup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2)), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe class2NicChargeCreated
      }

      "display only the charge creation item for a payment on account 1 of 2 late interest charge" in new TestSetup(documentDetailModel(outstandingAmount = 0), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
      }

      "display only the charge creation item for a payment on account 2 of 2 late interest charge" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(2)
      }

      "display only the charge creation item for a new balancing charge late interest charge" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe balancingChargeInterestCreated
      }

      "display the charge creation item when history is found and allocations are disabled" in new TestSetup(documentDetailModel(outstandingAmount = 0),
        adjustmentHistory = amendedAdjustmentHistory, paymentAllocationEnabled = false, paymentAllocations = List(mock(classOf[PaymentHistoryAllocations]))) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(1) td:nth-child(1)").text() shouldBe "Unknown"
        document.select("tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
        document.select("tbody tr:nth-child(1) td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display the correct message for an amended charge for a payment on account 1 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0), adjustmentHistory = amendedAdjustmentHistory) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe firstPoaAdjusted
        document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
      }

      "display the correct message for an amended charge for a payment on account 2 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("ITSA - POA 2")), adjustmentHistory = amendedAdjustmentHistory) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe secondPoaAdjusted
      }

      "display the correct message for an amended charge for a balancing charge" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("TRM Amend Charge")), adjustmentHistory = adjustmentHistoryWithBalancingCharge) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe balancingChargeAmended
      }

      "display the correct message for a customer requested change for a payment on account 1 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0), adjustmentHistory = customerRequestAdjustmentHistory) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(1)
      }

      "display the correct message for a customer requested change for a payment on account 2 of 2" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("ITSA - POA 2")), adjustmentHistory = customerRequestAdjustmentHistory) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(2)
      }

      "display the correct message for a customer requested change for a balancing charge" in new TestSetup(documentDetailModel(outstandingAmount = 0, documentDescription = Some("TRM Amend Charge")), adjustmentHistory = customerRequestAdjustmentHistory) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe balancingChargeRequest
      }

      "show payment allocations in history table" when {

        "allocations are enabled and present in the list" when {

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

          "chargeHistory enabled, having Payment created in the first row" in new TestSetup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(historyRowPOA1Created :: expectedPaymentAllocationRows: _*)
          }

          "chargeHistory enabled with a matching link to the payment allocations page" in new TestSetup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            document.select(Selectors.table).select("a").size shouldBe 10
            document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.routes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url) shouldBe true
          }

          "chargeHistory disabled" in new TestSetup(documentDetailModel(),
            chargeHistoryEnabled = false, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(expectedPaymentAllocationRows: _*)
          }
        }
      }

      "hide payment allocations in history table" when {
        "allocations enabled but list is empty" when {
          "chargeHistory enabled, having Payment created in the first row" in new TestSetup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = Nil) {
            verifyPaymentHistoryContent(historyRowPOA1Created)
          }

          "chargeHistory disabled, not showing the table at all" in new TestSetup(documentDetailModel(),
            chargeHistoryEnabled = false, paymentAllocationEnabled = true, paymentAllocations = Nil) {
            (document select Selectors.table).size shouldBe 0
          }
        }
      }

      "display the coded out details" when {
        val documentDetailCodingOut = documentDetailModel(transactionId = "CODINGOUT02",
          documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED), outstandingAmount = 2500.00,
          originalAmount = 2500.00)

        "Coding Out is Enabled" in new TestSetup(documentDetailCodingOut, codingOutEnabled = true) {
          document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
          document.select("#check-paye-para").text() shouldBe payeTaxCodeTextWithStringMessage(2018)
          document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
          document.select("#coding-out-notice").text() shouldBe insetPara
          document.select("#coding-out-message").text() shouldBe codingOutMessage2017To2018WithStringMessagesArgument
          document.select("#coding-out-notice-link").attr("href") shouldBe cancellledPayeTaxCodeInsetLink
          document.select("a.govuk-button").size() shouldBe 0
          document.select(".govuk-table tbody tr").size() shouldBe 1
          document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2019", "2020")} £2,500.00"
        }
      }

      "Scenario were Class2 NICs has been paid and only coding out information needs to be displayed" when {
        val documentDetailCodingOut = documentDetailModel(transactionId = "CODINGOUT02",
          documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED), outstandingAmount = 2500.00,
          originalAmount = 2500.00)

        "Coding Out is Enabled" in new TestSetup(documentDetailCodingOut, codingOutEnabled = true) {
          document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
          document.select("#coding-out-notice").text() shouldBe insetPara
          document.select("#coding-out-message").text() shouldBe codingOutMessage2017To2018WithStringMessagesArgument
          document.select("#coding-out-notice-link").attr("href") shouldBe cancellledPayeTaxCodeInsetLink
          document.selectById("paymentAmount").text() shouldBe "Payment amount £2,500.00"
          document.selectById("codingOutRemainingToPay").text() shouldBe messages("chargeSummary.codingOutRemainingToPay", "2019", "2020")
          document.select("a.govuk-button").size() shouldBe 0
          document.select(".govuk-table tbody tr").size() shouldBe 1
        }
        "Coding Out is Disabled" in new TestSetup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), codingOutEnabled = false) {
          document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 $balancingCharge"
          verifySummaryListRowNumeric(1, dueDate, "OVERDUE 15 May 2019")
          verifySummaryListRowNumeric(2, fullPaymentAmount, "£1,400.00")
          verifySummaryListRowNumeric(3, remainingToPay, "£1,400.00")
          document.select("#coding-out-notice").text() shouldBe ""
          document.select("#coding-out-message").text() shouldBe ""
          document.select("#coding-out-notice-link").attr("href") shouldBe ""
          document.select("a.govuk-button").size() shouldBe 1
          document.select(".govuk-table tbody tr").size() shouldBe 1
        }
      }

      "MFA Credits" when {
        val paymentAllocations = List(
          paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
          paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("123456789012"), Some("PAYID01")),
        )

        "Display an unpaid MFA Credit" in new TestSetup(
          documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), isMFADebit = true) {
          val summaryListText = "Due date OVERDUE 15 May 2019 Full payment amount £1,400.00 Remaining to pay £1,400.00 "
          val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
          val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
          // heading should be hmrc adjustment
          document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 " +
            messages("chargeSummary.hmrcAdjustment.text")
          // remaining to pay should be the same as payment amount
          document.select(".govuk-summary-list").text() shouldBe summaryListText
          // there should be a "make a payment" button
          document.select("#payment-link-2019").size() shouldBe 1
          // payment history should show only "HMRC adjustment created"
          document.select("#payment-history-table tr").size shouldBe 2
          document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
        }

        "Display a paid MFA Credit" in new TestSetup(
          documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge"),
            outstandingAmount = 0.00), isMFADebit = true,
          paymentAllocationEnabled = true,
          paymentAllocations = paymentAllocations) {
          val summaryListText = "Due date 15 May 2019 Full payment amount £1,400.00 Remaining to pay £0.00 "
          val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
          val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
          val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,500.00"
          val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,600.00"
          val allocationLinkHref = "/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=PAYID01"
          // heading should be hmrc adjustment
          document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 " +
            messages("chargeSummary.hmrcAdjustment.text")
          // remaining to pay should be zero
          document.select(".govuk-summary-list").text() shouldBe summaryListText
          // there should not be a "make a payment" button
          document.select("#payment-link-2019").size() shouldBe 0
          // payment history should show two rows "HMRC adjustment created" and "payment put towards HMRC Adjustment"
          document.select("#payment-history-table tr").size shouldBe 4
          document.select("#payment-history-table tr:nth-child(1)").text() shouldBe paymentHistoryText
          document.select("#payment-history-table tr:nth-child(2)").text() shouldBe MFADebitAllocation1
          document.select("#payment-history-table tr:nth-child(3)").text() shouldBe MFADebitAllocation2
          // allocation link should be to agent payment allocation page
          document.select("#payment-history-table tr:nth-child(3) a").attr("href") shouldBe allocationLinkHref
        }
      }

      "display balancing charge due date as N/A and hide sections - Payment Breakdown ,Make a payment button ,Any payments you make, Payment History" when {
        val balancingDetailZero = DocumentDetail(taxYear = 2018, transactionId = "", documentDescription = Some("TRM Amend Charge"), documentText = Some(""), outstandingAmount = 0, originalAmount = BigDecimal(0), documentDate = LocalDate.of(2018, 3, 29))
        "balancing charge is 0" in new TestSetup(balancingDetailZero, codingOutEnabled = true) {
          document.select(".govuk-summary-list").text() shouldBe "Due date N/A Full payment amount £0.00 Remaining to pay £0.00"
          document.select("p").get(1).text shouldBe "View what you owe to check if you have any other charges to pay."
          document.select("#payment-history-table").isEmpty shouldBe true
          document.select("#heading-payment-breakdown").isEmpty shouldBe true
          document.select(s"#payment-link-${documentDetailModel().taxYear}").isEmpty shouldBe true
          document.select("#payment-days-note").isEmpty shouldBe true
        }
      }
    }
  }

  "The charge summary view when missing mandatory expected fields" should {
    "throw a MissingFieldException" in new TestSetup(documentDetailModel()) {
      val exceptionViewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
        currentDate = dateService.getCurrentDate,
        documentDetailWithDueDate = DocumentDetailWithDueDate(documentDetailModel(), None),
        backUrl = "testBackURL",
        gatewayPage = None,
        btaNavPartial = None,
        paymentBreakdown = paymentBreakdown,
        paymentAllocations = List(),
        payments = payments,
        chargeHistoryEnabled = true,
        paymentAllocationEnabled = false,
        latePaymentInterestCharge = false,
        codingOutEnabled = false,
        isAgent = false,
        isMFADebit  = false,
        adjustmentHistory = defaultAdjustmentHistory,
        documentType = OtherCharge)
      val thrownException = intercept[MissingFieldException] {

        chargeSummary(exceptionViewModel)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
    }
  }

  "agent" when {
    "The charge summary view" should {

      "should not have a payment link when an outstanding amount is to be paid" in new TestSetup(documentDetailModel(), isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "should have a payment processing information section" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), isAgent = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1Agent
      }

      "have a interest lock payment link when the interest is accruing" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkTextAgent
        document.select("#main-content p a").attr("href") shouldBe whatYouOweAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestLinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have a interest lock payment link when the interest has previously" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkTextAgent
        document.select("#main-content p a").attr("href") shouldBe whatYouOweAgentUrl
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWordAgent} ${interestLinkTextAgent} ${interestLinkFullTextAgent}"
      }

      "have no interest lock payment link when there is no accrued interest" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkTextAgent
        document.select("#main-content p a").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(documentDetailModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkTextAgent
        document.select("#main-content p a").attr("href") shouldBe whatYouOweAgentUrl
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new TestSetup(documentDetailModel(), paymentBreakdown = paymentBreakdown, isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "not have a payment link when there is an outstanding amount of 0" in new TestSetup(documentDetailModel(outstandingAmount = 0), isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "list payment allocations with right number of rows and agent payment allocations link" in new TestSetup(documentDetailModel(),
        chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = List(
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

      "Display an unpaid MFA Credit" in new TestSetup(
        documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), isMFADebit = true,
        isAgent = true) {
        val summaryListText = "Due date OVERDUE 15 May 2019 Full payment amount £1,400.00 Remaining to pay £1,400.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        // heading should be hmrc adjustment
        document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 " +
          messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be the same as payment amount
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // there should be a "make a payment" button
        document.select("#payment-link-2019").size() shouldBe 0
        // payment history should show only "HMRC adjustment created"
        document.select("#payment-history-table tr").size shouldBe 2
        document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
      }

      "Display a paid MFA Credit" in new TestSetup(
        documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge"),
          outstandingAmount = 0.00), isMFADebit = true, isAgent = true,
        paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
        val summaryListText = "Due date 15 May 2019 Full payment amount £1,400.00 Remaining to pay £0.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,500.00"
        val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2019 £1,600.00"
        val allocationLinkHref = "/report-quarterly/income-and-expenses/view/agents/payment-made-to-hmrc?documentNumber=PAYID01"
        // heading should be hmrc adjustment
        document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 " +
          messages("chargeSummary.hmrcAdjustment.text")
        // remaining to pay should be zero
        document.select(".govuk-summary-list").text() shouldBe summaryListText
        // there should not be a "make a payment" button
        document.select("#payment-link-2019").size() shouldBe 0
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
