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
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.nextUpdates.{NextUpdateModelWithIncomeType, ObligationsModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.FinancialDetailsTestConstants.{fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import testConstants.NextUpdatesTestConstants._
import testUtils.ViewSpec
import views.html.TaxYearSummary

import java.time.LocalDate

class TaxYearSummaryViewSpec extends ViewSpec with FeatureSwitching {

  val testYear: Int = 2018
  val hrefForecastSelector: String = """a[href$="#forecast"]"""

  val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  import implicitDateFormatter._
  import TaxYearSummaryMessages._

  def modelComplete(crystallised: Option[Boolean], unattendedCalc: Boolean = false): TaxYearSummaryViewModel = TaxYearSummaryViewModel(
    timestamp = Some("2020-01-01T00:35:34.185Z"),
    income = 1,
    deductions = 2.02,
    totalTaxableIncome = 3,
    taxDue = 4.04,
    crystallised = crystallised,
    unattendedCalc = unattendedCalc,
    forecastIncome = Some(12500),
    forecastIncomeTaxAndNics = Some(5000.99)
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
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"), documentText = Some(taxYearSummaryClass2Nic))))

  val payeChargeList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("PAYE Self Assessment")), codingOutEnabled = true))


  val immediatelyRejectedByNps: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some(taxYearSummaryClass2Nic)), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy(
        documentDescription = Some("TRM New Charge"), interestOutstandingAmount = Some(0.0)))
  )

  val rejectedByNpsPartWay: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some(taxYearSummaryClass2Nic)), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy
      (documentDescription = Some("TRM New Charge"), documentText = Some("Cancelled PAYE Self Assessment")), codingOutEnabled = true)
  )

  val codingOutPartiallyCollected: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some(taxYearSummaryClass2Nic)), codingOutEnabled = true),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy(
        documentDescription = Some("TRM New Charge"), interestOutstandingAmount = Some(0.0))),
    fullDocumentDetailWithDueDateModel.copy(
      documentDetail = fullDocumentDetailModel.copy
      (documentDescription = Some("TRM New Charge"), documentText = Some(messages("whatYouOwe.cancelled-paye-sa.heading"))), codingOutEnabled = true)
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

  def estimateView(documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = testChargesList, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), documentDetailsWithDueDates, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def class2NicsView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), class2NicsChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = codingOutEnabled)

  def estimateViewWithNoCalcData(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, None, testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def unattendedCalcView(isAgent: Boolean = false, unattendedCalc: Boolean): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false), unattendedCalc)), testChargesList, testObligationsModel, "testBackUrl", isAgent, codingOutEnabled = false
  )

  def multipleDunningLockView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), testDunningLockChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def crystallisedView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(true))), testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled = false)

  def payeView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), payeChargeList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def immediatelyRejectedByNpsView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), immediatelyRejectedByNps, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def rejectedByNpsPartWayView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), rejectedByNpsPartWay, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def codingOutPartiallyCollectedView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), codingOutPartiallyCollected, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

  def forecastCalcView(codingOutEnabled: Boolean = false, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled,
    showForecastData = true)

  def forecastCalcViewCrystalised(codingOutEnabled: Boolean = false, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(true))), testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled,
    showForecastData = true)

  def noForecastDataView(codingOutEnabled: Boolean = false, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled,
    showForecastData = false)

  def forecastWithNoCalcData(codingOutEnabled: Boolean = false, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, None, testChargesList, testObligationsModel, "testBackURL", isAgent, codingOutEnabled, showForecastData = true)

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  object TaxYearSummaryMessages {
    val heading: String = messages("tax-year-summary.heading")
    val title: String = messages("titlePattern.serviceName.govUk", heading)
    val agentTitle: String = messages("agent.titlePattern.serviceName.govUk", heading)
    val secondaryHeading: String = messages("tax-year-summary.heading-secondary", s"${testYear - 1}", s"$testYear")
    val calculationDate: String = messages("tax-year-summary.calculation-date")
    val calcDate: String = "1 January 2020"
    val estimate: String = s"6 April ${testYear - 1} to 1 January 2020 estimate"
    val totalDue: String = messages("tax-year-summary.total-due")
    val taxDue: String = "£4.04"
    val calcDateInfo: String = messages("tax-year-summary.calc-from-last-time")
    val calcEstimateInfo: String = messages("tax-year-summary.calc-estimate-info")
    val taxCalculation: String = messages("tax-year-summary.tax-calculation.date", s"${testYear - 1}", s"$testYear")
    val taxCalculationHeading: String = messages("tax-year-summary.tax-calculation")
    val taxCalculationTab: String = messages("tax-year-summary.tax-calculation")
    val taxCalculationNoData: String = messages("tax-year-summary.tax-calculation.no-calc")
    val forecastNoData: String = messages("forecast_taxCalc.noForecast.heading")
    val forecastNoDataNote: String = messages("forecast_taxCalc.noForecast.text")
    val unattendedCalcPara: String = s"! ${messages("tax-year-summary.tax-calculation.unattended-calc")}"
    val taxCalculationNoDataNote: String = messages("tax-year-summary.tax-calculation.no-calc.note")
    val payments: String = messages("tax-year-summary.payments")
    val updates: String =  messages("tax-year-summary.updates")
    val income: String = messages("tax-year-summary.income")
    val section: String = messages("tax-year-summary.section")
    val allowancesAndDeductions: String = messages("tax-year-summary.deductions")
    val totalIncomeDue: String = messages("tax-year-summary.taxable-income")
    val incomeTaxNationalInsuranceDue: String = messages("tax-year-summary.tax-due")
    val paymentType: String = messages("tax-year-summary.payments.payment-type")
    val dueDate: String = messages("tax-year-summary.payments.due-date")
    val amount: String = messages("tax-year-summary.payments.amount")
    val paymentOnAccount1: String = messages("tax-year-summary.payments.paymentOnAccount1.text")
    val paymentOnAccount2: String = messages("tax-year-summary.payments.paymentOnAccount2.text")
    val unpaid: String = "Unpaid"
    val paid: String = "Paid"
    val partPaid: String = "Part paid"
    val noPaymentsDue: String = messages("tax-year-summary.payments.no-payments")
    val updateType: String = messages("updateTab.updateType")
    val updateIncomeSource: String = messages("updateTab.incomeSource")
    val updateDateSubmitted: String = messages("updateTab.dateSubmitted")
    val lpiPaymentOnAccount1: String = messages("tax-year-summary.payments.lpi.paymentOnAccount1.text")
    val lpiPaymentOnAccount2: String = messages("tax-year-summary.payments.lpi.paymentOnAccount2.text")
    val lpiRemainingBalance: String = messages("tax-year-summary.payments.lpi.balancingCharge.text")
    val paymentUnderReview: String = messages("tax-year-summary.payments.paymentUnderReview")
    val taxYearSummaryClass2Nic: String = messages("tax-year-summary.payments.class2Nic.text")
    val remainingBalance: String = messages("tax-year-summary.payments.balancingCharge.text")
    val payeSA: String = messages("tax-year-summary.payments.codingOut.text")

    val cancelledPaye: String = messages("tax-year-summary.payments.cancelledPayeSelfAssessment.text")
    val na: String = messages("tax-year-summary.na")
    val payeTaxCode: String = messages("tax-year-summary.paye-tax-code")

    def updateCaption(from: String, to: String): String = s"$from to $to"

    def incomeType(incomeType: String): String = {
      incomeType match {
        case "Property" => messages("updateTab.obligationType.property")
        case "Business" => messages("updateTab.obligationType.business")
        case "Crystallised" => messages("updateTab.obligationType.crystallised")
        case other => other
      }
    }

    def updateType(updateType: String): String = {
      updateType match {
        case "Quarterly" => messages("updateTab.updateType.quarterly")
        case "EOPS" => messages("updateTab.updateType.eops")
        case "Crystallised" => messages("updateTab.updateType.crystallised")
        case _ => updateType
      }
    }
  }

  "taxYearSummary" when {
    "the user is an individual" should {
      "display forecast data when forecast data present" in new Setup(forecastCalcView()) {
        document.title shouldBe title
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.getOptionalSelector("#forecast").isDefined shouldBe true
        document.getOptionalSelector(".forecast_table").isDefined shouldBe true

        val incomeForecastUrl = "/report-quarterly/income-and-expenses/view/2018/forecast-income"
        val taxDueForecastUrl = "/report-quarterly/income-and-expenses/view/2018/forecast-tax-calculation"

        document.select(".forecast_table tbody tr").size() shouldBe 3
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(3) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£5,000.99"
        document.select("#inset_forecast").text() shouldBe messagesLookUp("tax-year-summary.forecast_tab.insetText", testYear.toString)
      }

      "NOT display forecast data when showForecastData param is false" in new Setup(noForecastDataView()) {
        document.title shouldBe title
        document.getOptionalSelector("#tab_forecast").isDefined shouldBe false
      }

      "have the correct title" in new Setup(estimateView()) {
        document.title shouldBe title
      }

      "have the correct heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(heading)
      }

      "have the correct secondary heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(secondaryHeading)
        layoutContent.selectHead("span").text.contains(secondaryHeading)
      }

      "display the calculation date and title" in new Setup(estimateView()) {
        layoutContent.h2.selectFirst("h2").text().contains(taxCalculationHeading)
        layoutContent.selectHead("dl > div:nth-child(1) > dt:nth-child(1)").text shouldBe calculationDate
        layoutContent.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe calcDate
      }

      "display the estimate due for an ongoing tax year" in new Setup(estimateView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxCalculation
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe modelComplete(Some(false)).taxDue.toCurrencyString
      }

      "display the total due for a crystallised year" in new Setup(crystallisedView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe totalDue
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe modelComplete(Some(true)).taxDue.toCurrencyString
      }

      "have a paragraph explaining the calc date for an ongoing year" in new Setup(estimateView()) {
        layoutContent.selectHead("p#calc-date-info").text shouldBe calcDateInfo
      }

      "have a paragraph explaining that the calc date is an estimate" in new Setup(estimateView()) {
        layoutContent.selectHead("p#calc-estimate-info").text shouldBe calcEstimateInfo
      }

      "not have a paragraph explaining the calc date for a crystallised year" in new Setup(crystallisedView()) {
        layoutContent.getOptionalSelector("p.govuk-body") shouldBe None
      }

      "show three tabs with the correct tab headings" in new Setup(estimateView()) {
        layoutContent.selectHead("""a[href$="#taxCalculation"]""").text shouldBe taxCalculationTab
        layoutContent.selectHead( """a[href$="#payments"]""").text shouldBe payments
        layoutContent.selectHead("""a[href$="#updates"]""").text shouldBe updates
      }

      "when in an ongoing year should display the correct heading in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead(" #income-deductions-contributions-table caption").text shouldBe taxCalculationHeading
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxCalculation
      }

      "show the unattended calculation info when an unattended calc is returned" in new Setup(unattendedCalcView(unattendedCalc = true)) {
        layoutContent.selectHead(".govuk-warning-text").text shouldBe unattendedCalcPara
      }

      "not show the unattended calculation info when the calc returned isn't unattended" in new Setup(unattendedCalcView(unattendedCalc = false)) {
        layoutContent.getOptionalSelector(".govuk-warning-text") shouldBe None
      }

      "display the section header in the Tax Calculation tab" in new Setup(estimateView()) {
        val sectionHeader: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(1)")
        sectionHeader.text shouldBe section
      }

      "display the amount header in the Tax Calculation tab" in new Setup(estimateView()) {
        val amountHeader: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(2)")
        amountHeader.text shouldBe amount
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView()) {
        val incomeLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(1) a")
        incomeLink.text shouldBe income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) td:nth-child(2)").text shouldBe modelComplete(Some(false)).income.toCurrencyString
      }

      "when there is no calc data should display the correct heading in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > h2").text shouldBe taxCalculationNoData
      }

      "when there is no calc data should display the correct notes in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > p").text shouldBe taxCalculationNoDataNote
      }

      "when there is no calc data should display the correct heading in the Forecast tab" in new Setup(forecastWithNoCalcData()) {
        layoutContent.getElementById("no-forecast-data-header").text shouldBe forecastNoData
      }

      "when there is no calc data should display the correct notes in the Forecast tab" in new Setup(forecastWithNoCalcData()) {
        layoutContent.getElementById("no-forecast-data-note").text shouldBe forecastNoDataNote
      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView()) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(2) th:nth-child(1) a")
        allowancesLink.text shouldBe allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "−£2.02"
      }

      "display the Total income on which tax is due row in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(3) th:nth-child(1)").text shouldBe totalIncomeDue
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(3) td:nth-child(2)").text shouldBe modelComplete(Some(false)).totalTaxableIncome.toCurrencyString
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView()) {
        val totalTaxDueLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(4) th:nth-child(1) a")
        totalTaxDueLink.text shouldBe incomeTaxNationalInsuranceDue
        totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) td:nth-child(2)").text shouldBe modelComplete(Some(false)).taxDue.toCurrencyString
      }

      "display the table headings in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#paymentTypeHeading").text shouldBe paymentType
        layoutContent.selectHead("#paymentDueDateHeading").text shouldBe dueDate
        layoutContent.selectHead("#paymentAmountHeading").text shouldBe amount
      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Due date in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 May 2019"
      }


      "display the Amount in the payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe "£1,400.00"
      }

      "display no payments due when there are no charges in the payments tab" in new Setup(estimateView(emptyChargeList)) {
        layoutContent.selectHead("#payments p").text shouldBe noPaymentsDue
        layoutContent.h2.selectFirst("h2").text().contains(payments)
        layoutContent.selectHead("#payments").doesNotHave("table")
      }

      "display the late payment interest POA1 with a dunning lock applied" in new Setup(estimateView()) {
        val paymentType: Element = layoutContent.selectHead("#payments-table tr:nth-child(3) div:nth-child(3)")
        paymentType.text shouldBe paymentUnderReview
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(2) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(2)").text shouldBe "15 Jun 2019"
      }


      "display the Amount in the payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(3)").text shouldBe "£100.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(3) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount2
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(2)").text shouldBe "15 Jul 2019"
      }


      "display the Amount in the payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(3)").text shouldBe "£80.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(4) a")
        paymentTypeLink.text shouldBe lpiRemainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(2)").text shouldBe "15 Aug 2019"
      }


      "display the Amount in the payments tab for late payment interest p" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(4) td:nth-child(3)").text shouldBe "£100.00"
      }

      "display the Dunning lock subheading in the payments tab for multiple lines POA1 and Balancing payment" in new Setup(multipleDunningLockView()) {
        layoutContent.selectHead("#payments-table tbody tr:nth-child(1) div:nth-child(3)").text shouldBe paymentUnderReview
        layoutContent.selectHead("#payments-table tbody tr:nth-child(3) div:nth-child(3)").text shouldBe paymentUnderReview
        layoutContent.doesNotHave("#payments-table tbody tr:nth-child(4) th:nth-child(1) div:nth-child(3)")
      }

      "display the Class 2 National Insurance payment link on the payments table when coding out is enabled" in new Setup(class2NicsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Balancing payment payment link on the payments table when coding out is disabled" in new Setup(class2NicsView(codingOutEnabled = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe payeSA
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      s"display the Due date in the Payments tab for PAYE Self Assessment as ${na}" in new Setup(payeView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe na
      }

      "display the Amount in the payments tab for PAYE Self Assessment" in new Setup(payeView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe "£1,400.00"
      }

      "display the Due date in the Payments tab for Cancelled" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 May 2019"
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Balancing payment on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display updates by due-date" in new Setup(estimateView()) {

        testObligationsModel.allDeadlinesWithSource(previous = true).groupBy[LocalDate] { nextUpdateWithIncomeType =>
          nextUpdateWithIncomeType.obligation.due
        }.toList.sortBy(_._1)(localDateOrdering).reverse.foreach { case (due: LocalDate, obligations: Seq[NextUpdateModelWithIncomeType]) =>
          layoutContent.selectHead(s"#table-default-content-$due").text shouldBe messages("updateTab.due", due.toLongDate)
          val sectionContent = layoutContent.selectHead(s"#updates")
          obligations.zip(1 to obligations.length).foreach {
            case (testObligation, index) =>
              val divAccordion = sectionContent.selectHead(s"div:nth-of-type($index)")

              divAccordion.selectHead("caption").text shouldBe
                messages("updateTab.dateToDate", testObligation.obligation.start.toLongDate, testObligation.obligation.end.toLongDate)
              divAccordion.selectHead("thead").selectNth("th", 1).text shouldBe updateType
              divAccordion.selectHead("thead").selectNth("th", 2).text shouldBe updateIncomeSource
              divAccordion.selectHead("thead").selectNth("th", 3).text shouldBe updateDateSubmitted
              val row = divAccordion.selectHead("tbody").selectHead("tr")
              row.selectNth("th", 1).text shouldBe updateType(testObligation.obligation.obligationType)
              row.selectNth("td", 1).text shouldBe incomeType(testObligation.incomeType)
              row.selectNth("td", 2).text shouldBe testObligation.obligation.dateReceived.map(_.toLongDateShort).getOrElse("")
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

      "display forecast data when forecast data present" in new Setup(forecastCalcView(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.getOptionalSelector("#forecast").isDefined shouldBe true
        document.getOptionalSelector(".forecast_table").isDefined shouldBe true

        val incomeForecastUrl = "/report-quarterly/income-and-expenses/view/agents/2018/forecast-income"
        val taxDueForecastUrl = "/report-quarterly/income-and-expenses/view/agents/2018/forecast-tax-calculation"

        document.select(".forecast_table tbody tr").size() shouldBe 3
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(3) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£5,000.99"
        document.select("#inset_forecast").text() shouldBe messagesLookUp("tax-year-summary.forecast_tab.insetText", testYear.toString)
      }

      "NOT display forecastdata when showForecastData param is false" in new Setup(noForecastDataView(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#tab_forecast").isDefined shouldBe false
      }

      "display No forecast data yet when calcData returns NOT_FOUND" in new Setup(forecastWithNoCalcData(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.select(".forecast_table h2").text.contains(messagesLookUp("forecast_taxCalc.noForecast.heading"))
        document.select(".forecast_table p").text.contains(messagesLookUp("forecast_taxCalc.noForecast.text"))
      }

      "have the correct title" in new Setup(estimateView(isAgent = true)) {
        document.title shouldBe agentTitle
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val incomeLink: Element = layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) th:nth-child(1) a")
        incomeLink.text shouldBe income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) td:nth-child(2)").text shouldBe modelComplete(Some(false)).income.toCurrencyString
      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(2) th:nth-child(1) a")
        allowancesLink.text shouldBe allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "−£2.02"
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val totalTaxDueLink: Element = layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) th:nth-child(1) a")
        totalTaxDueLink.text shouldBe incomeTaxNationalInsuranceDue

        totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) td:nth-child(2)").text shouldBe modelComplete(Some(false)).taxDue.toCurrencyString

      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(2) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Class 2 National Insurance payment link on the payments table when coding out is enabled" in new Setup(
        class2NicsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table when coding out is enabled" in new Setup(payeView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe payeSA
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(codingOutEnabled = true, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

    }
  }

  "The TaxYearSummary view when missing mandatory fields" should {
    "throw a MissingFieldException" in {
      val thrownException = intercept[MissingFieldException] {
        taxYearSummaryView(
          taxYear = testYear,
          modelOpt = Some(modelComplete(Some(false))),
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
