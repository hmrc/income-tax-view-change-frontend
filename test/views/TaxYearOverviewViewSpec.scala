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

import config.featureswitch.FeatureSwitching
import exceptions.MissingFieldException
import implicits.ImplicitCurrencyFormatter.{CurrencyFormatter, CurrencyFormatterInt}
import implicits.ImplicitDateFormatterImpl
import models.financialDetails.DocumentDetailWithDueDate
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.nextUpdates.{NextUpdateModelWithIncomeType, ObligationsModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.FinancialDetailsTestConstants.{fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import testConstants.NextUpdatesTestConstants._
import testUtils.ViewSpec
import views.html.TaxYearOverview

import java.time.LocalDate

class TaxYearOverviewViewSpec extends ViewSpec with FeatureSwitching {

  val testYear: Int = 2018

  val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val taxYearOverviewView: TaxYearOverview = app.injector.instanceOf[TaxYearOverview]

  import implicitDateFormatter._

  def completeOverview(crystallised: Option[Boolean]): TaxYearOverviewViewModel = TaxYearOverviewViewModel(
    timestamp = Some("2020-01-01T00:35:34.185Z"),
    income = 1,
    deductions = 2.02,
    totalTaxableIncome = 3,
    taxDue = 4.04,
    crystallised = crystallised
  )

  val testDunningLockChargesList: List[DocumentDetailWithDueDate] = List(
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("ITSA- POA 1")),
      dueDate = Some(LocalDate.of(2019, 6, 15)), dunningLock = true),
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("ITSA - POA 2")),
      dueDate = Some(LocalDate.of(2019, 7, 15)), dunningLock = false),
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge")),
      dueDate = Some(LocalDate.of(2019, 8, 15)), dunningLock = true))

  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    dueDate = Some(LocalDate.of(2019, 6, 15)), isLatePaymentInterest = true),
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("ITSA - POA 2"), latePaymentInterestAmount = Some(80.0)),
      dueDate = Some(LocalDate.of(2019, 7, 15)), isLatePaymentInterest = true),
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"), interestOutstandingAmount = Some(0.0)),
      dueDate = Some(LocalDate.of(2019, 8, 15)), isLatePaymentInterest = true),
    fullDocumentDetailWithDueDateModel)

  val class2NicsChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    dueDate = Some(LocalDate.of(2021, 7, 31))),
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance"))))

  val payeChargeList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("PAYE Self Assessment")), codingOutEnabled = true))


  val immediatelyRejectedByNps: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance")), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy(
        documentDescription = Some("TRM New Charge"), interestOutstandingAmount = Some(0.0)))
  )

  val rejectedByNpsPartWay: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance")), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy
      (documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true)
  )

  val codingOutPartiallyCollected: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("Class 2 National Insurance")), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy(
        documentDescription = Some("TRM New Charge"), interestOutstandingAmount = Some(0.0))),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy
      (documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true)
  )

  val documentDetailWithDueDateMissingDueDate: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    dueDate = None
  ))

  val emptyChargeList: List[DocumentDetailWithDueDate] = List.empty

  val testWithOneMissingDueDateChargesList: List[DocumentDetailWithDueDate] = List(
    fullDocumentDetailWithDueDateModel.copy(dueDate = None),
    fullDocumentDetailWithDueDateModel
  )

  val testWithMissingOriginalAmountChargesList: List[DocumentDetailWithDueDate] = List(
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(originalAmount = None))
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))

  def estimateView(documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = testChargesList, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), documentDetailsWithDueDates, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def class2NicsView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), class2NicsChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = codingOutEnabled)

  def estimateViewWithNoCalcData(isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, None, testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def multipleDunningLockView(isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), testDunningLockChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def crystallisedView(isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(true))), testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def payeView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), payeChargeList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def immediatelyRejectedByNpsView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), immediatelyRejectedByNps, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def rejectedByNpsPartWayView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), rejectedByNpsPartWay, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def codingOutPartiallyCollectedView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearOverviewView(
    testYear, Some(completeOverview(Some(false))), codingOutPartiallyCollected, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  object taxYearOverviewMessages {
    val title: String = "Tax year overview - Business Tax account - GOV.UK"
    val agentTitle: String = "Tax year overview - Your client’s Income Tax details - GOV.UK"
    val heading: String = "Tax year overview"
    val secondaryHeading: String = s"6 April ${testYear - 1} to 5 April $testYear"
    val calculationDate: String = "Calculation date"
    val calcDate: String = "1 January 2020"
    val estimate: String = s"6 April ${testYear - 1} to 1 January 2020 estimate"
    val totalDue: String = "Total Due"
    val taxDue: String = "£4.04"
    val calcDateInfo: String = "This calculation is only based on your completed updates for this tax year up to 5 January 2017. It is not your final tax bill for the year and is a year to date estimate based on the information you have entered so far."
    val taxCalculation: String = s"6 April ${testYear - 1} to 5 January $testYear calculation"
    val taxCalculationHeading: String = "Calculation"
    val taxCalculationTab: String = "Calculation"
    val taxCalculationNoData: String = "No calculation yet"
    val taxCalculationNoDataNote: String = "You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software."
    val payments: String = "Payments"
    val updates: String = "Updates"
    val income: String = "Income"
    val section: String = "Section"
    val allowancesAndDeductions: String = "Allowances and deductions"
    val totalIncomeDue: String = "Total income on which tax is due"
    val incomeTaxNationalInsuranceDue: String = "Income Tax and National Insurance contributions due"
    val paymentType: String = "Payment type"
    val dueDate: String = "Due date"
    val status: String = "Status"
    val amount: String = "Amount"
    val paymentOnAccount1: String = "Payment on account 1 of 2"
    val paymentOnAccount2: String = "Payment on account 2 of 2"
    val unpaid: String = "Unpaid"
    val paid: String = "Paid"
    val partPaid: String = "Part paid"
    val noPaymentsDue: String = "No payments currently due."
    val updateType: String = "Update type"
    val updateIncomeSource: String = "Income source"
    val updateDateSubmitted: String = "Date submitted"
    val lpiPaymentOnAccount1: String = "Late payment interest for payment on account 1 of 2"
    val lpiPaymentOnAccount2: String = "Late payment interest for payment on account 2 of 2"
    val lpiRemainingBalance: String = "Late payment interest for Balancing payment"
    val paymentUnderReview: String = "Payment under review"
    val class2Nic: String = "Class 2 National Insurance"
    val remainingBalance: String = "Balancing payment"
    val payeSA: String = "Balancing payment collected through PAYE tax code"

    val cancelledPaye: String = "Cancelled Self Assessment payment (through your PAYE tax code)"
    val na: String = "N/A"
    val payeTaxCode: String = "PAYE tax code"

    def updateCaption(from: String, to: String): String = s"$from to $to"

    def dueMessage(due: String): String = s"Due $due"

    def incomeType(incomeType: String): String = {
      incomeType match {
        case "Property" => "Property income"
        case "Business" => "Business"
        case "Crystallised" => "All income sources"
        case other => other
      }
    }

    def updateType(updateType: String): String = {
      updateType match {
        case "Quarterly" => "Quarterly Update"
        case "EOPS" => "Annual Update"
        case "Crystallised" => "Final Declaration"
        case _ => updateType
      }
    }
  }

  "taxYearOverview" when {
    "the user is an individual" should {
      "have the correct title" in new Setup(estimateView()) {
        document.title shouldBe taxYearOverviewMessages.title
      }

      "have the correct heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(taxYearOverviewMessages.heading)
      }

      "have the correct secondary heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(taxYearOverviewMessages.secondaryHeading)
        layoutContent.selectHead("span").text.contains(taxYearOverviewMessages.secondaryHeading)
      }

      "display the calculation date" in new Setup(estimateView()) {
        layoutContent.selectHead("dl > div:nth-child(1) > dt:nth-child(1)").text shouldBe taxYearOverviewMessages.calculationDate
        layoutContent.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe taxYearOverviewMessages.calcDate
      }

      "display the estimate due for an ongoing tax year" in new Setup(estimateView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxYearOverviewMessages.taxCalculation
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe completeOverview(Some(false)).taxDue.toCurrencyString
      }

      "display the total due for a crystallised year" in new Setup(crystallisedView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxYearOverviewMessages.totalDue
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe completeOverview(Some(true)).taxDue.toCurrencyString
      }

      "have a paragraph explaining the calc date for an ongoing year" in new Setup(estimateView()) {
        layoutContent.selectHead("p#calc-date-info").text shouldBe taxYearOverviewMessages.calcDateInfo
      }

      "not have a paragraph explaining the calc date for a crystallised year" in new Setup(crystallisedView()) {
        layoutContent.getOptionalSelector("p.govuk-body") shouldBe None
      }

      "show three tabs with the correct tab headings" in new Setup(estimateView()) {
        layoutContent.selectHead("#tab_taxCalculation").text shouldBe taxYearOverviewMessages.taxCalculationTab
        layoutContent.selectHead("#tab_payments").text shouldBe taxYearOverviewMessages.payments
        layoutContent.selectHead("#tab_updates").text shouldBe taxYearOverviewMessages.updates
      }

      "when in an ongoing year should display the correct heading in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead(" #income-deductions-table caption").text shouldBe taxYearOverviewMessages.taxCalculationHeading
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxYearOverviewMessages.taxCalculation
      }

      "display the section header in the Tax Calculation tab" in new Setup(estimateView()) {
        val sectionHeader: Element = layoutContent.selectHead(" #income-deductions-table tr:nth-child(1) th:nth-child(1)")
        sectionHeader.text shouldBe taxYearOverviewMessages.section
      }

      "display the amount header in the Tax Calculation tab" in new Setup(estimateView()) {
        val amountHeader: Element = layoutContent.selectHead(" #income-deductions-table tr:nth-child(1) th:nth-child(2)")
        amountHeader.text shouldBe taxYearOverviewMessages.amount
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView()) {
        val incomeLink: Element = layoutContent.selectHead(" #income-deductions-table tr:nth-child(1) td:nth-child(1) a")
        incomeLink.text shouldBe taxYearOverviewMessages.income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url
        layoutContent.selectHead("#income-deductions-table tr:nth-child(1) td:nth-child(2)").text shouldBe completeOverview(Some(false)).income.toCurrencyString
      }

      "when there is no calc data should display the correct heading in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > h2").text shouldBe taxYearOverviewMessages.taxCalculationNoData
      }

      "when there is no calc data should display the correct notes in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > p").text shouldBe taxYearOverviewMessages.taxCalculationNoDataNote
      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView()) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-table tr:nth-child(2) td:nth-child(1) a")
        allowancesLink.text shouldBe taxYearOverviewMessages.allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
        layoutContent.selectHead("#income-deductions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "−£2.02"
      }

      "display the Total income on which tax is due row in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#income-deductions-table tr:nth-child(3) td:nth-child(1)").text shouldBe taxYearOverviewMessages.totalIncomeDue
        layoutContent.selectHead("#income-deductions-table tr:nth-child(3) td:nth-child(2)").text shouldBe completeOverview(Some(false)).totalTaxableIncome.toCurrencyString
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView()) {
        val totalTaxDueLink: Element = layoutContent.selectHead("#taxdue-payments-table td:nth-child(1) a")
        totalTaxDueLink.text shouldBe taxYearOverviewMessages.incomeTaxNationalInsuranceDue
        totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummary(testYear).url
        layoutContent.selectHead("#taxdue-payments-table td:nth-child(2)").text shouldBe completeOverview(Some(false)).taxDue.toCurrencyString
      }

      "display the table headings in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#paymentTypeHeading").text shouldBe taxYearOverviewMessages.paymentType
        layoutContent.selectHead("#paymentDueDateHeading").text shouldBe taxYearOverviewMessages.dueDate
        layoutContent.selectHead("#paymentStatusHeading").text shouldBe taxYearOverviewMessages.status
        layoutContent.selectHead("#paymentAmountHeading").text shouldBe taxYearOverviewMessages.amount
      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Due date in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 May 2019"
      }

      "display the Status in the payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe taxYearOverviewMessages.unpaid
      }

      "display the Amount in the payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(4)").text shouldBe "£1,400.00"
      }

      "display no payments due when there are no charges in the payments tab" in new Setup(estimateView(emptyChargeList)) {
        layoutContent.selectHead("#payments p").text shouldBe taxYearOverviewMessages.noPaymentsDue
        layoutContent.h2.selectFirst("h2").text().contains(taxYearOverviewMessages.payments)
        layoutContent.selectHead("#payments").doesNotHave("table")
      }

      "display the late payment interest POA1 with a dunning lock applied" in new Setup(estimateView()) {
        val paymentType: Element = layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(1) div:nth-child(3)")
        paymentType.text shouldBe taxYearOverviewMessages.paymentUnderReview
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(2)").text shouldBe "15 Jun 2019"
      }

      "display the Status in the payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(3)").text shouldBe taxYearOverviewMessages.partPaid
      }

      "display the Amount in the payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(4)").text shouldBe "£100.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.lpiPaymentOnAccount2
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(2)").text shouldBe "15 Jul 2019"
      }

      "display the Status in the payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(3)").text shouldBe taxYearOverviewMessages.unpaid
      }

      "display the Amount in the payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(4)").text shouldBe "£80.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.lpiRemainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(2)").text shouldBe "15 Aug 2019"
      }

      "display the Status in the payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(3)").text shouldBe taxYearOverviewMessages.paid
      }

      "display the Amount in the payments tab for late payment interest p" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(4)").text shouldBe "£100.00"
      }

      "display the Dunning lock subheading in the payments tab for multiple lines POA1 and Balancing payment" in new Setup(multipleDunningLockView()) {
        layoutContent.selectHead("#payments-table tbody tr:nth-child(1) td:nth-child(1) div:nth-child(3)").text shouldBe taxYearOverviewMessages.paymentUnderReview
        layoutContent.selectHead("#payments-table tbody tr:nth-child(3) td:nth-child(1) div:nth-child(3)").text shouldBe taxYearOverviewMessages.paymentUnderReview
        layoutContent.doesNotHave("#payments-table tbody tr:nth-child(4) td:nth-child(1) div:nth-child(3)")
      }

      "display the Class 2 National Insurance payment link on the payments table when coding out is enabled" in new Setup(class2NicsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Balancing payment payment link on the payments table when coding out is disabled" in new Setup(class2NicsView(codingOutEnabled = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.payeSA
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      s"display the Due date in the Payments tab for PAYE Self Assessment as ${taxYearOverviewMessages.na}" in new Setup(payeView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe taxYearOverviewMessages.na
      }

      s"display the Status in the payments tab for PAYE Self Assessment as ${taxYearOverviewMessages.payeTaxCode}" in new Setup(payeView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe taxYearOverviewMessages.payeTaxCode
      }

      "display the Amount in the payments tab for PAYE Self Assessment" in new Setup(payeView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(4)").text shouldBe "£1,400.00"
      }

      "display the Due date in the Payments tab for Cancelled" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 May 2019"
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Balancing payment on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display updates by due-date" in new Setup(estimateView()) {

        testObligationsModel.allDeadlinesWithSource(previous = true).groupBy[LocalDate] { nextUpdateWithIncomeType =>
          nextUpdateWithIncomeType.obligation.due
        }.toList.sortBy(_._1)(localDateOrdering).reverse.foreach { case (due: LocalDate, obligations: Seq[NextUpdateModelWithIncomeType]) =>
          layoutContent.selectHead(s"#table-default-content-$due").text shouldBe taxYearOverviewMessages.dueMessage(due.toLongDate)
          val sectionContent = layoutContent.selectHead(s"#updates")
          obligations.zip(1 to obligations.length).foreach {
            case (testObligation, index) =>
              val divAccordion = sectionContent.selectHead(s"div:nth-of-type($index)")

              divAccordion.selectHead("caption").text shouldBe
                taxYearOverviewMessages.updateCaption(testObligation.obligation.start.toLongDate, testObligation.obligation.end.toLongDate)
              divAccordion.selectHead("thead").selectNth("th", 1).text shouldBe taxYearOverviewMessages.updateType
              divAccordion.selectHead("thead").selectNth("th", 2).text shouldBe taxYearOverviewMessages.updateIncomeSource
              divAccordion.selectHead("thead").selectNth("th", 3).text shouldBe taxYearOverviewMessages.updateDateSubmitted
              val row = divAccordion.selectHead("tbody").selectHead("tr")
              row.selectNth("td", 1).text shouldBe taxYearOverviewMessages.updateType(testObligation.obligation.obligationType)
              row.selectNth("td", 2).text shouldBe taxYearOverviewMessages.incomeType(testObligation.incomeType)
              row.selectNth("td", 3).text shouldBe testObligation.obligation.dateReceived.map(_.toLongDateShort).getOrElse("")
          }
        }
      }

      "throw exception when Due Date is missing as Agent" in {
        val expectedException = intercept[MissingFieldException] {
          new Setup(estimateView(testWithOneMissingDueDateChargesList, isAgent = true))
        }

        expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
      }

      "throw exception when Due Date is missing as Individual" in {
        val expectedException = intercept[MissingFieldException] {
          new Setup(estimateView(testWithOneMissingDueDateChargesList))
        }

        expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
      }

      "throw exception when Original Amount is missing as Agent" in {
        val expectedException = intercept[MissingFieldException] {
          new Setup(estimateView(testWithMissingOriginalAmountChargesList, isAgent = true))
        }

        expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Original Amount"
      }

      "throw exception when Original Amount is missing as Individual" in {
        val expectedException = intercept[MissingFieldException] {
          new Setup(estimateView(testWithMissingOriginalAmountChargesList))
        }

        expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Original Amount"
      }
    }
    "the user is an agent" should {

      "have the correct title" in new Setup(estimateView(isAgent = true)) {
        document.title shouldBe taxYearOverviewMessages.agentTitle
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val incomeLink: Element = layoutContent.selectHead("#income-deductions-table tr:nth-child(1) td:nth-child(1) a")
        incomeLink.text shouldBe taxYearOverviewMessages.income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-table tr:nth-child(1) td:nth-child(2)").text shouldBe completeOverview(Some(false)).income.toCurrencyString
      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-table tr:nth-child(2) td:nth-child(1) a")
        allowancesLink.text shouldBe taxYearOverviewMessages.allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.agent.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
        layoutContent.selectHead("#income-deductions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "−£2.02"
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val totalTaxDueLink: Element = layoutContent.selectHead("#taxdue-payments-table td:nth-child(1) a")
        totalTaxDueLink.text shouldBe taxYearOverviewMessages.incomeTaxNationalInsuranceDue
        totalTaxDueLink.attr("href") shouldBe controllers.agent.routes.TaxDueSummaryController.showTaxDueSummary(testYear).url
        layoutContent.selectHead("#taxdue-payments-table td:nth-child(2)").text shouldBe completeOverview(Some(false)).taxDue.toCurrencyString
      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(1) a")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Class 2 National Insurance payment link on the payments table when coding out is enabled" in new Setup(
        class2NicsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.payeSA
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.class2Nic
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe taxYearOverviewMessages.cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
          testYear, fullDocumentDetailModel.transactionId).url
      }

    }
  }

  "The TaxYearOverview view when missing mandatory fields" should {
    "throw a MissingFieldException" in {
      val thrownException = intercept[MissingFieldException] {
        taxYearOverviewView(
          taxYear = testYear,
          overviewOpt = Some(completeOverview(Some(false))),
          charges = documentDetailWithDueDateMissingDueDate,
          obligations = testObligationsModel,
          backUrl = "testBackURL",
          isAgent = false,
          codingOutEnabled = false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
    }
  }
}
