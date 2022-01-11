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

import models.chargeHistory.ChargeHistoryModel
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.twirl.api.Html
import testConstants.FinancialDetailsTestConstants._
import testUtils.ViewSpec
import views.html.ChargeSummary
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd

import java.time.LocalDate

class ChargeSummaryViewSpec extends ViewSpec {

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
    val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]
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

  val typePOA1 = "SA Payment on Account 1"
  val typePOA2 = "SA Payment on Account 2"
  val typeBalCharge = "SA Balancing Charge"

  def paymentsForCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentsWithChargeType =
    PaymentsWithChargeType(
      payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
        lot = Some("lot"), lotItem = Some("lotItem"), date = Some(date), transactionId = None)),
      mainType = Some(mainType), chargeType = Some(chargeType))

  object Messages {
    def poaHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"

    def poaInterestHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Late payment interest on payment on account $number of 2"

    def balancingChargeHeading(year: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Balancing payment"

    def balancingChargeInterestHeading(year: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Late payment interest on Balancing payment"

    def class2NicHeading(year: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Class 2 National Insurance"

    def cancelledSaPayeHeading(year: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Cancelled Self Assessment payment (through your PAYE tax code)"

    val dueDate = "Due date"
    val interestPeriod = "Interest period"
    val fullPaymentAmount = "Full payment amount"
    val paymentAmount = "Payment amount"
    val remainingToPay = "Remaining to pay"
    val paymentBreakdownHeading = "Payment breakdown"
    val chargeHistoryHeading = "Payment history"
    val historyRowPOA1Created = "29 Mar 2018 Payment on account 1 of 2 created £1,400.00"
    val codingOutHeader = "Tax year 6 April 2017 to 5 April 2018 PAYE self assessment"
    val codingOutInsetPara = "If this tax cannot be collected through your PAYE tax code (opens in new tab) for any reason, you will need to pay the remaining amount. You will have 42 days to make this payment before you may charged interest and penalties."

    def paymentOnAccountCreated(number: Int) = s"Payment on account $number of 2 created"

    def paymentOnAccountInterestCreated(number: Int) = s"Late payment interest for payment on account $number of 2 created"

    val balancingChargeCreated = "Balancing payment created"
    val balancingChargeInterestCreated = "Late payment interest for Balancing payment created"

    def paymentOnAccountAmended(number: Int) = s"Payment on account $number of 2 reduced due to amended return"

    val balancingChargeAmended = "Balancing payment reduced due to amended return"

    def paymentOnAccountRequest(number: Int) = s"Payment on account $number of 2 reduced by taxpayer request"

    def class2NicTaxYear(year: Int) = s"This is the Class 2 National Insurance payment for the ${year - 1} to $year tax year."
    val class2NicChargeCreated = "Class 2 National Insurance created"
    val cancelledSaPayeCreated = "Cancelled Self Assessment payment (through your PAYE tax code) created"

    def payeTaxCodeText(year: Int) = s"Check if your PAYE tax code has changed for the ${year - 1} to $year tax year."
    val payeTaxCodeLink = s"https://www.tax.service.gov.uk/check-income-tax/tax-codes/${getCurrentTaxYearEnd.getYear}"
    val cancelledPayeTaxCodeInsetText = "You have previously agreed to pay some of your Self Assessment bill through your PAYE tax code (opens in new tab). HMRC has been unable to collect all of these payments from you, so this is the remaining tax you need to pay."
    val cancellledPayeTaxCodeInsetLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"
    def remainingTaxYouOwe(year: Int) = s"This is the remaining tax you owe for the ${year - 1} to $year tax year."

    val balancingChargeRequest = "Balancing payment reduced by taxpayer request"
    val dunningLockBannerHeader = "Important"
    val dunningLockBannerLink = "This tax decision is being reviewed (opens in new tab)."

    val interestLinkText = "View what you owe"
    val interestLinkFullText = "to check if you have any interest on this payment"

    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink You still need to pay the total of $formattedAmount as you may be charged interest if not paid by $date."
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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    documentDetails = List(DocumentDetail("9999", "PAYID01", Some("Payment on Account"), Some("documentText"), Some(-5000), Some(-15000), LocalDate.of(2018, 8, 6), None, None, None, None, None, None,None, Some("lotItem"), Some("lot"))),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYIDO1"), items = Some(Seq(SubItem(dueDate = Some("2017-08-07"), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))))))
  )


  "individual" when {
    "The charge summary view" should {

      "have the correct heading for a POA 1" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
        document.select("h1").text() shouldBe Messages.poaHeading(2018, 1)
      }

      "have the correct heading for a POA 2" in new Setup(documentDetailModel(documentDescription = Some("ITSA - POA 2"))) {
        document.select("h1").text() shouldBe Messages.poaHeading(2018, 2)
      }

      "have the correct heading for a new balancing charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge"))) {
        document.select("h1").text() shouldBe Messages.balancingChargeHeading(2019)
      }

      "have the correct heading for an amend balancing charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge"))) {
        document.select("h1").text() shouldBe Messages.balancingChargeHeading(2019)
      }

      "have the correct heading for a POA 1 late interest charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.poaInterestHeading(2018, 1)
      }

      "have the correct heading for a POA 2 late interest charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.poaInterestHeading(2018, 2)
      }

      "have the correct heading for a Class 2 National Insurance charge when coding out FS is enabled" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance")), codingOutEnabled = true) {
        document.select("h1").text() shouldBe Messages.class2NicHeading(2018)
      }

      "have the correct heading for a Class 2 National Insurance charge when coding out FS is disabled" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge")), codingOutEnabled = false) {
        document.select("h1").text() shouldBe Messages.balancingChargeHeading(2018)
      }

      "have a paragraph explaining which tax year the Class 2 NIC is for" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance"), lpiWithDunningBlock = None), codingOutEnabled = true) {
        document.select("#main-content p:nth-child(2)").text() shouldBe Messages.class2NicTaxYear(2018)
      }

      "have the correct heading for a Cancelled PAYE Self Assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        document.select("h1").text() shouldBe Messages.cancelledSaPayeHeading(2018)
      }

      "have a paragraphs explaining Cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some("Cancelled PAYE Self Assessment"), lpiWithDunningBlock = None), codingOutEnabled = true) {
        document.select("#check-paye-para").text() shouldBe Messages.payeTaxCodeText(2018)
        document.select("#paye-tax-code-link").attr("href") shouldBe Messages.payeTaxCodeLink
        document.select("#cancelled-coding-out-notice").text() shouldBe Messages.cancelledPayeTaxCodeInsetText
        document.select("#cancelled-coding-out-notice a").attr("href") shouldBe Messages.cancellledPayeTaxCodeInsetLink

      }

      "display a due date, payment amount and remaining to pay for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        verifySummaryListRow(1, Messages.dueDate, "OVERDUE 15 May 2019")
        verifySummaryListRow(2, Messages.paymentAmount, "£1,400.00")
        verifySummaryListRow(3, Messages.remainingToPay, "£1,400.00")
      }

      "have a paragraph explaining how many days a payment can take to process for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        document.select("#payment-days-note").text() shouldBe "Payments can take up to 7 days to process."
      }

      "what you page link with text for cancelled PAYE self assessment" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe Messages.interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/payments-owed"
        document.select("#main-content p:nth-child(6)").text() shouldBe s"${Messages.interestLinkText} ${Messages.interestLinkFullText}"
      }

      "not display the Payment breakdown list for cancelled PAYE self assessment" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "have payment link for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        document.select("div#payment-link-2018").text() shouldBe "Pay now"
      }

      "display a payment history" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"),
        documentText = Some("Cancelled PAYE Self Assessment"), lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdown, codingOutEnabled = true) {
        document.select("main h2").text shouldBe Messages.chargeHistoryHeading
      }

      "display only the charge creation item when no history found for cancelled PAYE self assessment" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.cancelledSaPayeCreated
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "have the correct heading for a new balancing charge late interest charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.balancingChargeInterestHeading(2019)
      }

      "have the correct heading for an amend balancing charge late interest charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("TRM Amend Charge")), latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.balancingChargeInterestHeading(2019)
      }

      "not display a notification banner when there are no dunning locks in payment breakdown" in new Setup(
        documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdown) {

        document.doesNotHave(Selectors.id("dunningLocksBanner"))
      }

      "display a notification banner when there are dunning locks in payment breakdown" which {

        s"has the '${Messages.dunningLockBannerHeader}' heading" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          document.selectById("dunningLocksBanner")
            .select(Selectors.h2).text() shouldBe Messages.dunningLockBannerHeader
        }

        "has the link for Payment under review which opens in new tab" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          val link: Elements = document.selectById("dunningLocksBanner").select(Selectors.link)

          link.text() shouldBe Messages.dunningLockBannerLink
          link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          link.attr("target") shouldBe "_blank"
        }

        "shows the same remaining amount and a due date as in the charge summary list" which {
          "display a remaining amount" in new Setup(documentDetailModel(
            outstandingAmount = Some(1600)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£1,600.00", "15 May 2019")
          }

          "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(
            documentDetailModel(outstandingAmount = Some(0)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£0.00", "15 May 2019")
          }

          "display the original amount if no cleared or outstanding amount is present" in new Setup(
            documentDetailModel(outstandingAmount = None, originalAmount = Some(1700)), paymentBreakdown = paymentBreakdownWithDunningLocks) {

            document.selectById("dunningLocksBanner")
              .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£1,700.00", "15 May 2019")
          }
        }
      }

      "display a due date" in new Setup(documentDetailModel()) {
        verifySummaryListRow(1, Messages.dueDate, "OVERDUE 15 May 2019")
      }

      "display the correct due date for an interest charge" in new Setup(documentDetailModel(), latePaymentInterestCharge = true) {
        verifySummaryListRow(1, Messages.dueDate, "OVERDUE 15 June 2018")
      }

      "display an interest period for a late interest charge" in new Setup(documentDetailModel(originalAmount = Some(1500)), latePaymentInterestCharge = true) {
        verifySummaryListRow(2, Messages.interestPeriod, "29 Mar 2018 to 15 Jun 2018")
      }

      "display a charge amount" in new Setup(documentDetailModel(originalAmount = Some(1500))) {
        verifySummaryListRow(2, Messages.fullPaymentAmount, "£1,500.00")
      }

      "display a charge amount for a late interest charge" in new Setup(documentDetailModel(originalAmount = Some(1500)), latePaymentInterestCharge = true) {
        verifySummaryListRow(3, Messages.fullPaymentAmount, "£100.00")
      }

      "display a remaining amount" in new Setup(documentDetailModel(outstandingAmount = Some(1600))) {
        verifySummaryListRow(3, Messages.remainingToPay, "£1,600.00")
      }

      "display a remaining amount for a late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(1600)), latePaymentInterestCharge = true) {
        verifySummaryListRow(4, Messages.remainingToPay, "£80.00")
      }

      "display a remaining amount of 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        verifySummaryListRow(3, Messages.remainingToPay, "£0.00")
      }

      "display the original amount if no cleared or outstanding amount is present" in new Setup(documentDetailModel(outstandingAmount = None, originalAmount = Some(1700))) {
        verifySummaryListRow(3, Messages.remainingToPay, "£1,700.00")
      }

      "not display the Payment breakdown list when payments breakdown is empty" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "display the Payment breakdown list" which {

        "has a correct heading" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          document.selectById("heading-payment-breakdown").text shouldBe Messages.paymentBreakdownHeading
        }

        "has payment rows with charge types and original amounts" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67")
          verifyPaymentBreakdownRow(3, "Voluntary Class 2 National Insurance", "£3,456.78")
          verifyPaymentBreakdownRow(4, "Class 4 National Insurance", "£5,678.90")
          verifyPaymentBreakdownRow(5, "Capital Gains Tax", "£9,876.54")
          verifyPaymentBreakdownRow(6, "Student Loans", "£543.21")
        }

        "has payment rows with Under review note when there are dunning locks on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithDunningLocks) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67 Under review")
          verifyPaymentBreakdownRow(3, "Capital Gains Tax", "£9,876.54 Under review")
          verifyPaymentBreakdownRow(4, "Student Loans", "£543.21")
        }

        "has a payment row with Under review note when there is a dunning lock on a lpi charge" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1")), latePaymentInterestCharge = true) {
          verifyPaymentBreakdownRow(1, "Late payment interest", "£100.00 Under review")
          verifyPaymentBreakdownRow(2, "", "")
        }

        "has at least one record with an interest lock" which {

          "has payment rows with Under review note when there are dunning locks on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithMixedLocks) {
            verifyPaymentBreakdownRow(1, "Income Tax", "£123.45 We are charging you interest on this payment")
            verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67 Under review We are not currently charging interest on this payment")
          }

          "has payment rows with appropriate messages for each row" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithInterestLocks) {
            verifyPaymentBreakdownRow(1, "Income Tax", "£123.45 We are charging you interest on this payment")
            verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67 We are not currently charging interest on this payment")
            verifyPaymentBreakdownRow(3, "Capital Gains Tax", "£9,876.54 We have previously charged you interest on this payment")
            verifyPaymentBreakdownRow(4, "Student Loans", "£543.21 We are charging you interest on this payment")
          }

        }
        "has no records that have an interest lock applied" should {
          "has payment rows but no interest lock message when there are no interest locks but there's accrued interest on a payment" in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
            verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
            verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67")
          }
        }
      }

      "have a payment link when an outstanding amount is to be paid" in new Setup(documentDetailModel()) {
        document.select("div#payment-link-2018").text() shouldBe "Pay now"
      }

      "have a paragraph explaining how many days a payment can take to process" in new Setup(documentDetailModel(lpiWithDunningBlock = None)) {
        document.select("#main-content p:nth-child(5)").text() shouldBe "Payments can take up to 7 days to process."
      }

      "have a interest lock payment link when the interest is accruing" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues) {
        document.select("#main-content p a").text() shouldBe Messages.interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/payments-owed"
        document.select("#main-content p:nth-child(6)").text() shouldBe s"${Messages.interestLinkText} ${Messages.interestLinkFullText}"
      }

      "have a interest lock payment link when the interest has previously" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe Messages.interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/payments-owed"
        document.select("#main-content p:nth-child(6)").text() shouldBe s"${Messages.interestLinkText} ${Messages.interestLinkFullText}"
      }

      "have no interest lock payment link when there is no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/payments-owed"
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/payments-owed"
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new Setup(documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("div#payment-link-2018").text() shouldBe "Pay now"
      }

      "not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "display a charge history heading as an h2 when there is no Payment Breakdown" in new Setup(
        documentDetailModel(lpiWithDunningBlock = None, outstandingAmount = Some(0))) {
        document.select("main h2").text shouldBe Messages.chargeHistoryHeading
      }

      "display a charge history heading as an h3 when there is a Payment Breakdown" in new Setup(
        documentDetailModel(), paymentBreakdown = paymentBreakdown) {
        document.select("main h3").text shouldBe Messages.chargeHistoryHeading
      }

      "display only the charge creation item when no history found for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display only the charge creation item when no history found for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(2)
      }

      "display only the charge creation item when no history found for a new balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM New Charge"))) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.balancingChargeCreated
      }

      "display only the charge creation item for a Class 2 National Insurance charge" in new Setup(documentDetailModel(documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance")), codingOutEnabled = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.class2NicChargeCreated
      }

      "display only the charge creation item for a payment on account 1 of 2 late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0)), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountInterestCreated(1)
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
      }

      "display only the charge creation item for a payment on account 2 of 2 late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountInterestCreated(2)
      }

      "display only the charge creation item for a new balancing charge late interest charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM New Charge")), latePaymentInterestCharge = true) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.balancingChargeInterestCreated
      }

      "display the charge creation item when history is found and allocations are disabled" in new Setup(documentDetailModel(outstandingAmount = Some(0)),
        chargeHistory = List(amendedChargeHistoryModel), paymentAllocationEnabled = false, paymentAllocations = List(mock[PaymentsWithChargeType])) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(1) td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(1)
        document.select("tbody tr:nth-child(1) td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display the correct message for an amended charge for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountAmended(1)
        document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
      }

      "display the correct message for an amended charge for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountAmended(2)
      }

      "display the correct message for an amended charge for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge")), chargeHistory = List(amendedChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.balancingChargeAmended
      }

      "display the correct message for a customer requested change for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountRequest(1)
      }

      "display the correct message for a customer requested change for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2")), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountRequest(2)
      }

      "display the correct message for a customer requested change for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge")), chargeHistory = List(customerRequestChargeHistoryModel)) {
        document.select("tbody tr").size() shouldBe 2
        document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.balancingChargeRequest
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
            verifyPaymentHistoryContent(Messages.historyRowPOA1Created :: expectedPaymentAllocationRows: _*)
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
            verifyPaymentHistoryContent(Messages.historyRowPOA1Created)
          }

          "chargeHistory disabled, not showing the table at all" in new Setup(documentDetailModel(),
            chargeHistoryEnabled = false, paymentAllocationEnabled = true, paymentAllocations = Nil) {
            (document select Selectors.table).size shouldBe 0
          }
        }
      }

      "display the coded out details" when {
        val documentDetailCodingOut = documentDetailModel(amountCodedOut = Some(2500.00), transactionId = "CODINGOUT02",
          documentDescription = Some("TRM New Charge"), documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(2500.00),
          originalAmount = Some(2500.00))
        object CodingOutMessages {
          val header = "Tax year 6 April 2017 to 5 April 2018 Self Assessment payment (through your PAYE tax code)"
          val insetPara = "If this tax cannot be collected through your PAYE tax code (opens in new tab) for any reason, you will need to pay the remaining amount. You will have 42 days to make this payment before you may charged interest and penalties."
          val summaryMessage = "This is the remaining tax you owe for the 2017 to 2018 tax year."
          val noticeLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"
          val remainingText = "Collected through your PAYE tax code for 2017 to 2018 tax year"
          val payHistoryLine1 = "29 Mar 2018 Amount collected through your PAYE tax code for 2017 to 2018 tax year £2,500.00"
        }
        "Coding Out is Enabled" in new Setup(documentDetailCodingOut, codingOutEnabled = true) {
          document.select("h1").text() shouldBe CodingOutMessages.header
          document.select("#check-paye-para").text() shouldBe Messages.payeTaxCodeText(2018)
          document.select("#paye-tax-code-link").attr("href") shouldBe Messages.payeTaxCodeLink
          document.select("#coding-out-notice").text() shouldBe CodingOutMessages.insetPara
          document.select("#coding-out-message").text() shouldBe CodingOutMessages.summaryMessage
          document.select("#coding-out-notice-link").attr("href") shouldBe CodingOutMessages.noticeLink
          document.select(".govuk-summary-list__row").size() shouldBe 2
          document.select(".govuk-summary-list__row .govuk-summary-list__value").get(0).text() shouldBe "£2,500.00"
          document.select(".govuk-summary-list__row .govuk-summary-list__value").get(1).text() shouldBe CodingOutMessages.remainingText
          document.select("a.govuk-button").size() shouldBe 0
          document.select(".govuk-table tbody tr").size() shouldBe 1
          document.select(".govuk-table tbody tr").get(0).text() shouldBe CodingOutMessages.payHistoryLine1
        }
      }
    }
  }
  "agent" when {
    "The charge summary view" should {

      "should not have a payment link when an outstanding amount is to be paid" in new Setup(documentDetailModel(), isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "should not have a paragraph explaining how many days a payment can take to process" in new Setup(documentDetailModel(lpiWithDunningBlock = None), isAgent = true) {
        document.select("#main-content p:nth-child(5)").text() shouldBe ""
      }

      "have a interest lock payment link when the interest is accruing" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true) {
        document.select("#main-content p a").text() shouldBe Messages.interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
        document.select("#main-content p:nth-child(6)").text() shouldBe s"${Messages.interestLinkText} ${Messages.interestLinkFullText}"
      }

      "have a interest lock payment link when the interest has previously" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe Messages.interestLinkText
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
        document.select("#main-content p:nth-child(6)").text() shouldBe s"${Messages.interestLinkText} ${Messages.interestLinkFullText}"
      }

      "have no interest lock payment link when there is no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new Setup(documentDetailModel(lpiWithDunningBlock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true) {
        document.select("#main-content p a").text() shouldBe "what you owe"
        document.select("#main-content p a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
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
        document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.agent.routes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url) shouldBe true
      }
    }
  }
}
