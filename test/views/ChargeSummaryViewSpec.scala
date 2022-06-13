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

package views

import exceptions.MissingFieldException
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.twirl.api.Html
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.FinancialDetailsTestConstants._
import testUtils.ViewSpec
import views.html.ChargeSummary

import java.time.LocalDate

class ChargeSummaryViewSpec extends ViewSpec {

  lazy val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]

  import Messages._

  class Setup(documentDetail: DocumentDetail,
              dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
              paymentBreakdown: List[FinancialDetail] = List(),
              chargeHistory: List[ChargeHistoryModel] = List(),
              paymentAllocations: List[PaymentsWithChargeType] = List(),
              payments: FinancialDetailsModel = payments,
              chargeHistoryEnabled: Boolean = true,
              paymentAllocationEnabled: Boolean = false,
              latePaymentInterestCharge: Boolean = false,
              codingOutEnabled: Boolean = false,
              isAgent: Boolean = false) {
    val view: Html = chargeSummary(DocumentDetailWithDueDate(documentDetail, dueDate), "testBackURL",
      paymentBreakdown, chargeHistory, paymentAllocations, payments, chargeHistoryEnabled, paymentAllocationEnabled, latePaymentInterestCharge, codingOutEnabled, isAgent)
    val document: Document = Jsoup.parse(view.toString())

    def verifySummaryListRow(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val summaryListRow = document.select(s".govuk-summary-list:nth-of-type(1) .govuk-summary-list__row:nth-of-type($rowNumber)")
      summaryListRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      summaryListRow.select(".govuk-summary-list__value").text() shouldBe expectedValueText
    }

    def verifyPaymentBreakdownRow(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val paymentBreakdownRow = document.select(s".govuk-summary-list:nth-of-type(2) .govuk-summary-list__row:nth-of-type($rowNumber)")
      paymentBreakdownRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      paymentBreakdownRow.select(".govuk-summary-list__value").text() shouldBe expectedValueText
    }

    def verifyPaymentHistoryContent(rows: String*): Assertion = {
      document select Selectors.table text() shouldBe
        s"""
           |Date Description Amount
           |${rows.mkString("\n")}
           |""".stripMargin.trim.linesIterator.mkString(" ")
    }

  }

  def paymentsForCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
        lot = Some("lot"), lotItem = Some("lotItem"), date = Some(date), transactionId = None)),
      mainType = Some(mainType), chargeType = Some(chargeType))

  object Messages {
    val typePOA1 = "SA Payment on Account 1"
    val typePOA2 = "SA Payment on Account 2"
    val typeBalCharge = "SA Balancing Charge"
    val taxYearHeading: String = messages("taxYears.table.taxYear.heading")
    val balancingCharge: String = messages("chargeSummary.balancingCharge.text")
    val paymentBreakdownNic2: String = messages("chargeSummary.paymentBreakdown.nic2")
    val codingOutMessage2017To2018: String = messages("chargeSummary.codingOutMessage", "2017", "2018")
    val chargeSummaryCodingOutHeading2017To2018: String = s"$taxYearHeading 6 April 2017 to 5 April 2018 ${messages("chargeSummary.codingOut.text")}"
    val insetPara: String = s"${messages("chargeSummary.codingOutInset-1")} ${messages("chargeSummary.codingOutInset-2")}${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.codingOutInset-3")}"
    val paymentBreakdownInterestLocksCharging: String = messages("chargeSummary.paymentBreakdown.interestLocks.charging")

    def poaHeading(year: Int, number: Int) = s"$taxYearHeading 6 April ${year - 1} to 5 April $year Payment on account $number of 2"

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
    val chargeHistoryHeading: String = messages("chargeSummary.chargeHistory.heading")
    val historyRowPOA1Created: String =  s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.paymentOnAccount1.text")} £1,400.00"
    val codingOutHeader: String = s"$taxYearHeading ${messages("taxYears.taxYears", "6 April 2017", "5 April 2018")} PAYE self assessment"
    val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")}${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.payments-bullet1-3")}"

    def paymentOnAccountCreated(number: Int) = s"Payment on account $number of 2 created"

    def paymentOnAccountInterestCreated(number: Int) = s"Late payment interest for payment on account $number of 2 created"

    val balancingChargeCreated: String = messages("chargeSummary.chargeHistory.created.balancingCharge.text")
    val balancingChargeInterestCreated: String = messages("chargeSummary.lpi.chargeHistory.created.balancingCharge.text")

    def paymentOnAccountAmended(number: Int) = s"Payment on account $number of 2 reduced due to amended return"

    val balancingChargeAmended: String = messages("chargeSummary.chargeHistory.amend.balancingCharge.text")

    def paymentOnAccountRequest(number: Int) = s"Payment on account $number of 2 reduced by taxpayer request"

    def class2NicTaxYear(year: Int) =  messages("chargeSummary.nic2TaxYear", s"${year - 1}", s"$year")

    val class2NicChargeCreated: String = messages("chargeSummary.chargeHistory.created.class2Nic.text")
    val cancelledSaPayeCreated: String = messages("chargeSummary.chargeHistory.created.cancelledPayeSelfAssessment.text")

    def payeTaxCodeText(year: Int) = s"${messages("chargeSummary.check-paye-tax-code-1")} ${messages("chargeSummary.check-paye-tax-code-2")} ${messages("chargeSummary.check-paye-tax-code-3", s"${year - 1}", s"$year")}"

    val payeTaxCodeLink = s"https://www.tax.service.gov.uk/check-income-tax/tax-codes/${getCurrentTaxYearEnd.getYear}"
    val cancelledPayeTaxCodeInsetText = s"${messages("chargeSummary.cancelledPayeInset-1")} ${messages("chargeSummary.cancelledPayeInset-2")}${messages("pagehelp.opensInNewTabText")}. ${messages("chargeSummary.cancelledPayeInset-3")}"
    val cancellledPayeTaxCodeInsetLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"

    def remainingTaxYouOwe(year: Int) = messages("chargeSummary.codingOutMessage", s"${year - 1}", s"$year")

    val balancingChargeRequest: String = messages("chargeSummary.chargeHistory.request.balancingCharge.text")
    val dunningLockBannerHeader: String = messages("chargeSummary.dunning.locks.banner.title")
    val dunningLockBannerLink: String = s"${messages("chargeSummary.dunning.locks.banner.linkText")}${messages("pagehelp.opensInNewTabText")}."
    val interestLinkFirstWord: String = messages("chargeSummary.whatYouOwe.textOne")
    val interestLinkText: String = messages("chargeSummary.whatYouOwe.linkText")
    val interestLinkFullText: String = messages("chargeSummary.interestLocks.text")
    val cancelledPAYESelfAssessment: String = messages("whatYouOwe.cancelled-paye-sa.heading")

    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink ${messages("chargeSummary.dunning.locks.banner.note", s"$formattedAmount", s"$date")}"
  }

  val amendedChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", "", "", 1500, LocalDate.of(2018, 7, 6), "amended return")
  val customerRequestChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", "", "", 1500, LocalDate.of(2018, 7, 6), "Customer Request")

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI"),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB"),
    financialDetail(originalAmount = 3456.78, chargeType = "Voluntary NIC2-NI"),
    financialDetail(originalAmount = 5678.9, chargeType = "NIC4 Wales"),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT"),
    financialDetail(originalAmount = 543.21, chargeType = "SL")
  )

  val paymentBreakdownWithDunningLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI"),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT", dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 543.21, chargeType = "SL")
  )

  val paymentBreakdownWithInterestLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI", accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", interestLock = Some("Clerical Interest Signal")),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT", interestLock = Some("Manual RPI Signal"), accruedInterest = Some(35)),
    financialDetail(originalAmount = 543.21, chargeType = "SL")
  )

  val paymentBreakdownWhenInterestAccrues: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI", accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithPreviouslyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", interestLock = Some("Clerical Interest Signal"), accruedInterest = Some(30))
  )

  val paymentBreakdownWithMixedLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI"),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", dunningLock = Some("Stand over order"), interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithOnlyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI", accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB")
  )

  val paymentBreakdownWithOnlyInterestLock: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", interestLock = Some("Clerical Interest Signal"))
  )

  val payments: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    codingDetails = None,
    documentDetails = List(DocumentDetail("9999", "PAYID01", Some("Payment on Account"), Some("documentText"), Some(-5000), Some(-15000), LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, Some("lotItem"), Some("lot"))),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYIDO1"), items = Some(Seq(SubItem(dueDate = Some("2017-08-07"), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))))))
  )

  def checkPaymentProcessingInfo(document: Document): Unit = {
    document.select("#payment-days-note").text() shouldBe messages("chargeSummary.payment-days-note")
    document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe
      s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("chargeSummary.payments-bullet1-3")}"
    document.select("#payment-processing-bullets li:nth-child(2)").text() shouldBe messages("chargeSummary.payments-bullet2")
  }

  "individual" when {
    "The charge summary view" should {

      "have a fallback link" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.hasFallbackBacklinkTo("testBackURL")
      }

      "have the correct heading for a POA 1" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.select("h1").text() shouldBe poaHeading(2018, 1)
      }

      "have the correct heading for a POA 2" in new Setup(documentDetailModel(documentDescription = Some("ITSA - POA 2"))) {
        document.select("h1").text() shouldBe poaHeading(2018, 2)
      }

      "have the correct heading for a new balancing charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge"))) {
        document.select("h1").text() shouldBe balancingChargeHeading(2019)
      }

      "have the correct heading for an amend balancing charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge"))) {
        document.select("h1").text() shouldBe balancingChargeHeading(2019)
      }

      "have the correct heading for a POA 1 late interest charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe poaInterestHeading(2018, 1)
      }

      "have the correct heading for a POA 2 late interest charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe poaInterestHeading(2018, 2)
      }

      s"have the correct heading for a $paymentBreakdownNic2 charge when coding out FS is enabled" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2)), codingOutEnabled = true) {
        document.select("h1").text() shouldBe class2NicHeading(2018)
      }

      s"have the correct heading for a $paymentBreakdownNic2 charge when coding out FS is disabled" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge")), codingOutEnabled = false) {
        document.select("h1").text() shouldBe balancingChargeHeading(2018)
      }

      "have a paragraph explaining which tax year the Class 2 NIC is for" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2), lpiWithDunningBlock = None), codingOutEnabled = true) {
        document.select("#main-content p:nth-child(2)").text() shouldBe class2NicTaxYear(2018)
      }

      s"have the correct heading for a Cancelled PAYE Self Assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("h1").text() shouldBe cancelledSaPayeHeading(2018)
      }

      "have a paragraphs explaining Cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading")), lpiWithDunningBlock = None), codingOutEnabled = true) {
        document.select("#check-paye-para").text() shouldBe payeTaxCodeText(2018)
        document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
        document.select("#cancelled-coding-out-notice").text() shouldBe cancelledPayeTaxCodeInsetText
        document.select("#cancelled-coding-out-notice a").attr("href") shouldBe cancellledPayeTaxCodeInsetLink

      }

      "display a due date, payment amount and remaining to pay for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        verifySummaryListRow(1, dueDate, "OVERDUE 15 May 2019")
        verifySummaryListRow(2, paymentAmount, "£1,400.00")
        verifySummaryListRow(3, remainingToPay, "£1,400.00")
      }

      "have a paragraph explaining how many days a payment can take to process for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1
      }

      "what you page link with text for cancelled PAYE self assessment" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "not display the Payment breakdown list for cancelled PAYE self assessment" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "have payment link for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "display a payment history" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading")), lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdown, codingOutEnabled = true) {
        document.select("main h2").text shouldBe chargeHistoryHeading
      }

      "display only the charge creation item when no history found for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe cancelledSaPayeCreated
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "have the correct heading for a new balancing charge late interest charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe balancingChargeInterestHeading(2019)
      }

      "have the correct heading for an amend balancing charge late interest charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe balancingChargeInterestHeading(2019)
      }

      "not display a notification banner when there are no dunning locks in payment breakdown" in new Setup(
        documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdown) {

        document.doesNotHave(Selectors.id("dunningLocksBanner"))
      }

      "display a notification banner when there are dunning locks in payment breakdown" which {

        s"has the '${dunningLockBannerHeader}' heading" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          document.selectById("dunningLocksBanner")
            .select(Selectors.h2).text() shouldBe dunningLockBannerHeader
        }

        "has the link for Payment under review which opens in new tab" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          val link: Elements = document.selectById("dunningLocksBanner").select(Selectors.link)

          link.text() shouldBe dunningLockBannerLink
          link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          link.attr("target") shouldBe "_blank"
        }

        "shows the same remaining amount and a due date as in the charge summary list" which {
          "display a remaining amount" in new Setup(documentDetailModel(
            outstandingAmount = Some(1600)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£1,600.00", "15 May 2019")
          }

          "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(
            documentDetailModel(outstandingAmount = Some(0)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£0.00", "15 May 2019")
          }

          "display the original amount if no cleared or outstanding amount is present" in new Setup(
            documentDetailModel(outstandingAmount = None, originalAmount = Some(1700)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe dunningLockBannerText("£1,700.00", "15 May 2019")
          }
        }
      }

      "display a due date" in new Setup(documentDetailModel()) {
        verifySummaryListRow(1, dueDate, "OVERDUE 15 May 2019")
      }

      "display the correct due date for an interest charge" in new Setup(documentDetailModel(), latePaymentInterestCharge = true) {
        verifySummaryListRow(1, dueDate, "OVERDUE 15 June 2018")
      }

      "display an interest period for a late interest charge" in new Setup(documentDetailModel(originalAmount = Some(1500)), latePaymentInterestCharge = true) {
        verifySummaryListRow(2, interestPeriod, "29 Mar 2018 to 15 Jun 2018")
      }

      "display a charge amount" in new Setup(documentDetailModel(originalAmount = Some(1500))) {
        verifySummaryListRow(2, fullPaymentAmount, "£1,500.00")
      }

      "display a charge amount for a late interest charge" in new Setup(documentDetailModel(originalAmount = Some(1500)), latePaymentInterestCharge = true) {
        verifySummaryListRow(3, fullPaymentAmount, "£100.00")
      }

      "display a remaining amount" in new Setup(documentDetailModel(outstandingAmount = Some(1600))) {
        verifySummaryListRow(3, remainingToPay, "£1,600.00")
      }

      "display a remaining amount for a late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(1600)), latePaymentInterestCharge = true) {
        verifySummaryListRow(4, remainingToPay, "£80.00")
      }

      "display a remaining amount of 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        verifySummaryListRow(3, remainingToPay, "£0.00")
      }

      "display the original amount if no cleared or outstanding amount is present" in new Setup(documentDetailModel(outstandingAmount = None, originalAmount = Some(1700))) {
        verifySummaryListRow(3, remainingToPay, "£1,700.00")
      }

      "not display the Payment breakdown list when payments breakdown is empty" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "display the Payment breakdown list" which {

        "has a correct heading" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          document.selectById("heading-payment-breakdown").text shouldBe paymentBreakdownHeading
        }

        "has payment rows with charge types and original amounts" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, paymentBreakdownNic2, "£2,345.67")
          verifyPaymentBreakdownRow(3, "Voluntary Class 2 National Insurance", "£3,456.78")
          verifyPaymentBreakdownRow(4, "Class 4 National Insurance", "£5,678.90")
          verifyPaymentBreakdownRow(5, "Capital Gains Tax", "£9,876.54")
          verifyPaymentBreakdownRow(6, "Student Loans", "£543.21")
        }

        "has payment rows with Under review note when there are dunning locks on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, paymentBreakdownNic2, "£2,345.67 Under review")
          verifyPaymentBreakdownRow(3, "Capital Gains Tax", "£9,876.54 Under review")
          verifyPaymentBreakdownRow(4, "Student Loans", "£543.21")
        }

        "has a payment row with Under review note when there is a dunning lock on a lpi charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
          verifyPaymentBreakdownRow(1, "Late payment interest", "£100.00 Under review")
          verifyPaymentBreakdownRow(2, "", "")
        }

        "has at least one record with an interest lock" which {

          "has payment rows with Under review note when there are dunning locks on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithMixedLocks) {
            verifyPaymentBreakdownRow(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
            verifyPaymentBreakdownRow(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.dunningLocks.underReview")} ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
          }

          "has payment rows with appropriate messages for each row" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithInterestLocks) {
            verifyPaymentBreakdownRow(1, "Income Tax", s"£123.45 $paymentBreakdownInterestLocksCharging")
            verifyPaymentBreakdownRow(2, paymentBreakdownNic2, s"£2,345.67 ${messages("chargeSummary.paymentBreakdown.interestLocks.notCharging")}")
            verifyPaymentBreakdownRow(3, "Capital Gains Tax", s"£9,876.54 ${messages("chargeSummary.paymentBreakdown.interestLocks.previouslyCharged")}")
            verifyPaymentBreakdownRow(4, "Student Loans", s"£543.21 $paymentBreakdownInterestLocksCharging")
          }

        }
        "has no records that have an interest lock applied" should {
          "has payment rows but no interest lock message when there are no interest locks but there's accrued interest on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
            verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
            verifyPaymentBreakdownRow(2, paymentBreakdownNic2, "£2,345.67")
          }
        }
      }

      "have a payment link when an outstanding amount is to be paid" in new Setup(documentDetailModel()) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "have a payment processing information section" in new Setup(documentDetailModel(lpiWithDunningBlock = None), isAgent = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1
      }

      "have a interest lock payment link when the interest is accruing" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "have a interest lock payment link when the interest has previously" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"$interestLinkFirstWord $interestLinkText $interestLinkFullText"
      }

      "have no interest lock payment link when there is no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("div#payment-link-2018").text() shouldBe s"${messages("paymentDue.payNow")} ${messages("paymentDue.pay-now-hidden", "2017", "2018")}"
      }

      "not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "display a charge history heading as an h2 when there is no Payment Breakdown" in new Setup(
        documentDetailModel(lpiWithDunningBlock = None, outstandingAmount = Some(0))) {
        document.select("main h2").text shouldBe chargeHistoryHeading
      }

      "display a charge history heading as an h3 when there is a Payment Breakdown" in new Setup(
        documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("main h3").text shouldBe chargeHistoryHeading
      }

      "display only the charge creation item when no history found for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display only the charge creation item when no history found for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(2)
      }

      "display only the charge creation item when no history found for a new balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM New Charge"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe balancingChargeCreated
      }

      s"display only the charge creation item for a $paymentBreakdownNic2 charge" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some(paymentBreakdownNic2)), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe class2NicChargeCreated
      }

      "display only the charge creation item for a payment on account 1 of 2 late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0)), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
      }

      "display only the charge creation item for a payment on account 2 of 2 late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountInterestCreated(2)
      }

      "display only the charge creation item for a new balancing charge late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe balancingChargeInterestCreated
      }

      "display the charge creation item when history is found and allocations are disabled" in new Setup(documentDetailModel(outstandingAmount = Some(0)),
        chargeHistory = List(amendedChargeHistoryModel), paymentAllocationEnabled = false, paymentAllocations = List(mock[PaymentsWithChargeType])) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(1) td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
        document.select("tbody tr:nth-child(1) td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display the correct message for an amended charge for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountAmended(1)
        document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
      }

      "display the correct message for an amended charge for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountAmended(2)
      }

      "display the correct message for an amended charge for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge")), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe balancingChargeAmended
      }

      "display the correct message for a customer requested change for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(1)
      }

      "display the correct message for a customer requested change for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe paymentOnAccountRequest(2)
      }

      "display the correct message for a customer requested change for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge")), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe balancingChargeRequest
      }

      "show payment allocations in history table" when {

        "allocations are enabled and present in the list" when {

          val paymentAllocations = List(
            paymentsForCharge(typePOA1, "ITSA NI", "2018-03-30", 1500.0),
            paymentsForCharge(typePOA1, "NIC4 Scotland", "2018-03-31", 1600.0),

            paymentsForCharge(typePOA2, "ITSA Wales", "2018-04-01", 2400.0),
            paymentsForCharge(typePOA2, "NIC4-GB", "2018-04-15", 2500.0),

            paymentsForCharge(typeBalCharge, "ITSA England & NI", "2019-12-10", 3400.0),
            paymentsForCharge(typeBalCharge, "NIC4-NI", "2019-12-11", 3500.0),
            paymentsForCharge(typeBalCharge, "NIC2 Wales", "2019-12-12", 3600.0),
            paymentsForCharge(typeBalCharge, "CGT", "2019-12-13", 3700.0),
            paymentsForCharge(typeBalCharge, "SL", "2019-12-14", 3800.0),
            paymentsForCharge(typeBalCharge, "Voluntary NIC2-GB", "2019-12-15", 3900.0),
          )

          val expectedPaymentAllocationRows = List(
            "30 Mar 2018 Payment allocated to Income Tax for payment on account 1 of 2 2018 £1,500.00",
            "31 Mar 2018 Payment allocated to Class 4 National Insurance for payment on account 1 of 2 2018 £1,600.00",
            "1 Apr 2018 Payment allocated to Income Tax for payment on account 2 of 2 2018 £2,400.00",
            "15 Apr 2018 Payment allocated to Class 4 National Insurance for payment on account 2 of 2 2018 £2,500.00",
            "10 Dec 2019 Payment allocated to Income Tax for Balancing payment 2018 £3,400.00",
            "11 Dec 2019 Payment allocated to Class 4 National Insurance for Balancing payment 2018 £3,500.00",
            "12 Dec 2019 Payment allocated to Class 2 National Insurance for Balancing payment 2018 £3,600.00",
            "13 Dec 2019 Payment allocated to Capital Gains Tax for Balancing payment 2018 £3,700.00",
            "14 Dec 2019 Payment allocated to Student Loans for Balancing payment 2018 £3,800.00",
            "15 Dec 2019 Payment allocated to Voluntary Class 2 National Insurance for Balancing payment 2018 £3,900.00"
          )

          "chargeHistory enabled, having Payment created in the first row" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(historyRowPOA1Created :: expectedPaymentAllocationRows: _*)
          }

          "chargeHistory enabled with a matching link to the payment allocations page" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            document.select(Selectors.table).select("a").size shouldBe 10
            document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.routes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url) shouldBe true
          }

          "chargeHistory disabled" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = false, paymentAllocationEnabled = true, paymentAllocations = paymentAllocations) {
            verifyPaymentHistoryContent(expectedPaymentAllocationRows: _*)
          }
        }

      }
      "hide payment allocations in history table" when {
        "allocations enabled but list is empty" when {
          "chargeHistory enabled, having Payment created in the first row" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = Nil) {
            verifyPaymentHistoryContent(historyRowPOA1Created)
          }

          "chargeHistory disabled, not showing the table at all" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = false, paymentAllocationEnabled = true, paymentAllocations = Nil) {
            (document select Selectors.table).size shouldBe 0
          }
        }
      }

      "display the coded out details" when {
        val documentDetailCodingOut = documentDetailModel(transactionId = "CODINGOUT02",
          documentDescription = Some("TRM New Charge"), documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(2500.00),
          originalAmount = Some(2500.00))

        "Coding Out is Enabled" in new Setup(documentDetailCodingOut, codingOutEnabled = true) {
          document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
          document.select("#check-paye-para").text() shouldBe payeTaxCodeText(2018)
          document.select("#paye-tax-code-link").attr("href") shouldBe payeTaxCodeLink
          document.select("#coding-out-notice").text() shouldBe insetPara
          document.select("#coding-out-message").text() shouldBe codingOutMessage2017To2018
          document.select("#coding-out-notice-link").attr("href") shouldBe cancellledPayeTaxCodeInsetLink
          document.select("a.govuk-button").size() shouldBe 0
          document.select(".govuk-table tbody tr").size() shouldBe 1
          document.select(".govuk-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.codingOutPayHistoryAmount", "2019", "2020")} £2,500.00"
        }
      }

      "Scenario were Class2 NICs has been paid and only coding out information needs to be displayed" when {
        val documentDetailCodingOut = documentDetailModel(transactionId = "CODINGOUT02",
          documentDescription = Some("TRM New Charge"), documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(2500.00),
          originalAmount = Some(2500.00))

        "Coding Out is Enabled" in new Setup(documentDetailCodingOut, codingOutEnabled = true) {
          document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
          document.select("#coding-out-notice").text() shouldBe insetPara
          document.select("#coding-out-message").text() shouldBe codingOutMessage2017To2018
          document.select("#coding-out-notice-link").attr("href") shouldBe cancellledPayeTaxCodeInsetLink
          document.selectById("paymentAmount").text() shouldBe "Payment amount £2,500.00"
          document.selectById("codingOutRemainingToPay").text() shouldBe messages("chargeSummary.codingOutRemainingToPay", "2019", "2020")
          document.select("a.govuk-button").size() shouldBe 0
          document.select(".govuk-table tbody tr").size() shouldBe 1
        }
        "Coding Out is Disabled" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), codingOutEnabled = false) {
          document.select("h1").text() shouldBe s"$taxYearHeading 6 April 2018 to 5 April 2019 $balancingCharge"
          verifySummaryListRow(1, dueDate, "OVERDUE 15 May 2019")
          verifySummaryListRow(2, fullPaymentAmount, "£1,400.00")
          verifySummaryListRow(3, remainingToPay, "£1,400.00")
          document.select("#coding-out-notice").text() shouldBe ""
          document.select("#coding-out-message").text() shouldBe ""
          document.select("#coding-out-notice-link").attr("href") shouldBe ""
          document.select("a.govuk-button").size() shouldBe 1
          document.select(".govuk-table tbody tr").size() shouldBe 1
        }
      }


    }
  }

  "The charge summary view when missing mandatory expected fields" should {
    "throw a MissingFieldException" in {
      val thrownException = intercept[MissingFieldException] {
        chargeSummary(DocumentDetailWithDueDate(documentDetailModel(), None), "testBackURL",
          paymentBreakdown, List(), List(), payments, true, false, false, false, false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
    }
  }

  "agent" when {
    "The charge summary view" should {

      "should not have a payment link when an outstanding amount is to be paid" in new Setup(documentDetailModel(), isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "should have a payment processing information section" in new Setup(documentDetailModel(lpiWithDunningBlock = None), isAgent = true) {
        document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe paymentprocessingbullet1
      }

      "have a interest lock payment link when the interest is accruing" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWord} ${interestLinkText} ${interestLinkFullText}"
      }

      "have a interest lock payment link when the interest has previously" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/what-you-owe"
        document.select("#p-interest-locks-msg").text() shouldBe s"${interestLinkFirstWord} ${interestLinkText} ${interestLinkFullText}"
      }

      "have no interest lock payment link when there is no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/what-you-owe"
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/what-you-owe"
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown, isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailModel(outstandingAmount = Some(0)), isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }
      "list payment allocations with right number of rows and agent payment allocations link" in new Setup(documentDetailModel(),
        chargeHistoryEnabled = true, paymentAllocationEnabled = true, paymentAllocations = List(
          paymentsForCharge(typePOA1, "ITSA NI", "2018-03-30", 1500.0)), isAgent = true) {
        document.select(Selectors.table).select("a").size shouldBe 1
        document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent("PAYID01").url) shouldBe true
      }
    }
  }
}
