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

import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import enums.CodingOutType._
import exceptions.MissingFieldException
import implicits.ImplicitCurrencyFormatter.{CurrencyFormatter, CurrencyFormatterInt}
import implicits.ImplicitDateFormatterImpl
import models.financialDetails.DocumentDetailWithDueDate
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.liabilitycalculation.{Message, Messages}
import models.nextUpdates.{NextUpdateModelWithIncomeType, ObligationsModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.FinancialDetailsTestConstants.{MFADebitsDocumentDetailsWithDueDates, fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import testConstants.NextUpdatesTestConstants._
import testUtils.ViewSpec
import views.html.TaxYearSummary

import java.time.LocalDate

class TaxYearSummaryViewSpec extends ViewSpec with FeatureSwitching {

  val testYear: Int = 2018
  val hrefForecastSelector: String = """a[href$="#forecast"]"""

  val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  import TaxYearSummaryMessages._
  import implicitDateFormatter._

  def modelComplete(crystallised: Option[Boolean], unattendedCalc: Boolean = false): TaxYearSummaryViewModel = TaxYearSummaryViewModel(
    timestamp = Some("2020-01-01T00:35:34.185Z".toZonedDateTime.toLocalDate),
    income = 1,
    deductions = 2.02,
    totalTaxableIncome = 3,
    taxDue = 4.04,
    crystallised = crystallised,
    unattendedCalc = unattendedCalc,
    forecastIncome = Some(12500),
    forecastIncomeTaxAndNics = Some(5000.99),
    forecastAllowancesAndDeductions = Some(4200.00),
    forecastTotalTaxableIncome = Some(8300),
    periodFrom = Some(LocalDate.of(testYear - 1, 1, 1)),
    periodTo = Some(LocalDate.of(testYear, 1, 1))
  )

  val date: String = dateService.getCurrentDate.toLongDate

  val modelWithMultipleErrorMessages = modelComplete(Some(false)).copy(messages = Some(Messages(errors = Some(List(
    Message("C15014", date),
    Message("C55014", date),
    Message("C15015", ""),
    Message("C15016", ""),
    Message("C15102", ""),
    Message("C15103", ""),
    Message("C15104", ""),
    Message("C15105", ""),
    Message("C15322", ""),
    Message("C15323", ""),
    Message("C15325", ""),
    Message("C15523", ""),
    Message("C15524", ""),
    Message("C15506", ""),
    Message("C15507", "1000"),
    Message("C15510", "50"),
    Message("C15518", ""),
    Message("C15530", ""),
    Message("C15319", ""),
    Message("C15320", ""),
    Message("C15522", ""),
    Message("C15531", ""),
    Message("C15324", ""),
    Message("C55316", ""),
    Message("C55317", ""),
    Message("C55318", ""),
    Message("C55501", ""),
    Message("C55502", ""),
    Message("C55503", ""),
    Message("C55508", ""),
    Message("C55008", date),
    Message("C55011", date),
    Message("C55009", ""),
    Message("C55010", ""),
    Message("C55012", date),
    Message("C55013", date),
    Message("C55009", ""),
    Message("C55511", ""),
    Message("C55519", ""),
    Message("C55515", ""),
    Message("C55516", ""),
    Message("C55517", ""),
    Message("C55520", ""),
    Message("C95005", ""),
    Message("C159014", ""),
    Message("C159015", ""),
    Message("C159016", ""),
    Message("C159018", ""),
    Message("C159019", ""),
    Message("C159026", ""),
    Message("C159027", ""),
    Message("C159028", ""),
    Message("C159030", ""),
    Message("C159102", ""),
    Message("C159106", ""),
    Message("C159110", "50"),
    Message("C159115", ""),
    Message("C159500", ""),
    Message("C559099", ""),
    Message("C559100", ""),
    Message("C559101", ""),
    Message("C559103", ""),
    Message("C559107", ""),
    Message("C559104", ""),
    Message("C559105", ""),
    Message("C559113", ""),
    Message("C559114", "")
  ))
  )))

  val modelWithErrorMessages = modelComplete(Some(false))
    .copy(messages = Some(Messages(
      errors = Some(List(
        Message("C15015", messages("tax-year-summary.message.C15015"))
      ))
    )))

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
      documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED)), codingOutEnabled = true))

  val testBalancingPaymentChargeWithZeroValue: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    documentDetail = fullDocumentDetailModel.copy(
      documentDescription = Some("TRM New Charge"), documentText = Some("document Text"), originalAmount = Some(BigDecimal(0))), codingOutEnabled = true))


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
      (documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CANCELLED)), codingOutEnabled = true)
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

  def estimateView(documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = testChargesList, isAgent: Boolean = false, obligations: ObligationsModel = testObligationsModel): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), documentDetailsWithDueDates, obligations, "testBackURL", isAgent, codingOutEnabled = false)

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

  def testBalancingPaymentChargeWithZeroValueView(codingOutEnabled: Boolean, isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(false))), testBalancingPaymentChargeWithZeroValue, testObligationsModel, "testBackURL", isAgent, codingOutEnabled)

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

  def mfaDebitsView(codingOutEnabled: Boolean, isAgent: Boolean): Html = taxYearSummaryView(
    testYear, Some(modelComplete(Some(true))), MFADebitsDocumentDetailsWithDueDates, testObligationsModel, "testBackURL", isAgent, codingOutEnabled,
    showForecastData = false)

  def calculationMultipleErrorView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelWithMultipleErrorMessages), testChargesList, testObligationsModel, "testBackURL", isAgent, false)

  def calculationSingleErrorView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, Some(modelWithErrorMessages), testChargesList, testObligationsModel, "testBackURL", isAgent, false)

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  object TaxYearSummaryMessages {
    val heading: String = messages("tax-year-summary.heading")
    val title: String = messages("htmlTitle", heading)
    val agentTitle: String = messages("htmlTitle.agent", heading)
    val secondaryHeading: String = messages("tax-year-summary.heading-secondary", s"${testYear - 1}", s"$testYear")
    val calculationDate: String = messages("tax-year-summary.calculation-date")
    val calcDate: String = "1 January 2020"
    val estimate: String = s"6 April ${testYear - 1} to 1 January 2020 estimate"
    val totalDue: String = messages("tax-year-summary.total-due")
    val taxDue: String = "£4.04"
    val calDateFrom: String = implicitDateFormatter.longDate(LocalDate.of(testYear - 1, 1, 1)).toLongDate
    val calDateTo: String = implicitDateFormatter.longDate(LocalDate.of(testYear, 1, 1)).toLongDate
    val calcDateInfo: String = messages("tax-year-summary.calc-from-last-time")
    val calcEstimateInfo: String = messages("tax-year-summary.calc-estimate-info")
    val taxCalculation: String = messages("tax-year-summary.tax-calculation.date", calDateFrom, calDateTo)
    val taxCalculationHeading: String = messages("tax-year-summary.tax-calculation")
    val taxCalculationTab: String = messages("tax-year-summary.tax-calculation")
    val taxCalculationNoData: String = messages("tax-year-summary.tax-calculation.no-calc")
    val forecastNoData: String = messages("forecast_taxCalc.noForecast.heading")
    val forecastNoDataNote: String = messages("forecast_taxCalc.noForecast.text")
    val unattendedCalcPara: String = s"! Warning ${messages("tax-year-summary.tax-calculation.unattended-calc")}"
    val taxCalculationNoDataNote: String = messages("tax-year-summary.tax-calculation.no-calc.note")
    val payments: String = messages("tax-year-summary.payments")
    val updates: String = messages("tax-year-summary.updates")
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
    val hmrcAdjustment: String = messages("tax-year-summary.payments.hmrcAdjustment.text")
    val cancelledPaye: String = messages("tax-year-summary.payments.cancelledPayeSelfAssessment.text")
    val na: String = messages("tax-year-summary.na")
    val messageHeader: String = messages("tax-year-summary.message.header")
    val messageAction: String = "! Warning " + messages("tax-year-summary.message.action")
    val messageError1: String = messages("tax-year-summary.message.C15015")
    val messageError2: String = messages("tax-year-summary.message.C15016")

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

        document.select(".forecast_table tbody tr").size() shouldBe 4
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£4,200.00"
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£8,300.00"
        document.select(".forecast_table tbody tr:nth-child(4) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(4) td:nth-child(2)").text() shouldBe "£5,000.99"
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
        layoutContent.selectHead("""a[href$="#payments"]""").text shouldBe payments
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

      "when there is a error message from calculation" in new Setup(calculationSingleErrorView()) {
        val calculationContent = layoutContent.getElementById("taxCalculation")
        calculationContent.child(0).text shouldBe messageHeader
        calculationContent.child(1).text shouldBe messageError1
        calculationContent.child(2).text shouldBe messageAction
      }

      "when there are multiple error messages from calculation" in new Setup(calculationMultipleErrorView()) {
        val calculationContent = layoutContent.getElementById("taxCalculation")
        val errorMessageList = calculationContent.child(1)
        calculationContent.child(0).text shouldBe messageHeader
        calculationContent.child(2).text shouldBe messageAction

        errorMessageList.child(0).text shouldBe messages("tax-year-summary.message.C15014", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(1).text shouldBe messages("tax-year-summary.message.C55014", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(2).text shouldBe messages("tax-year-summary.message.C15015")
        errorMessageList.child(3).text shouldBe messages("tax-year-summary.message.C15016")
        errorMessageList.child(4).text shouldBe messages("tax-year-summary.message.C15102")
        errorMessageList.child(5).text shouldBe messages("tax-year-summary.message.C15103")
        errorMessageList.child(6).text shouldBe s"${messages("tax-year-summary.message.C15104.1")} ${messages("tax-year-summary.message.C15104.2")} ${messages("tax-year-summary.message.C15104.3")}"
        errorMessageList.child(7).text shouldBe messages("tax-year-summary.message.C15105")
        errorMessageList.child(8).text shouldBe s"${messages("tax-year-summary.message.C15322.1")} ${messages("tax-year-summary.message.C15322.2")} ${messages("tax-year-summary.message.C15322.3")} ${messages("tax-year-summary.message.C15322.4")}"
        errorMessageList.child(9).text shouldBe messages("tax-year-summary.message.C15323")
        errorMessageList.child(10).text shouldBe messages("tax-year-summary.message.C15325")
        errorMessageList.child(11).text shouldBe messages("tax-year-summary.message.C15523")
        errorMessageList.child(12).text shouldBe messages("tax-year-summary.message.C15524")
        errorMessageList.child(13).text shouldBe messages("tax-year-summary.message.C15506")
        errorMessageList.child(14).text shouldBe messages("tax-year-summary.message.C15507", "1000")
        errorMessageList.child(15).text shouldBe messages("tax-year-summary.message.C15510", "50")
        errorMessageList.child(16).text shouldBe messages("tax-year-summary.message.C15518")
        errorMessageList.child(17).text shouldBe messages("tax-year-summary.message.C15530")
        errorMessageList.child(18).text shouldBe messages("tax-year-summary.message.C15319")
        errorMessageList.child(19).text shouldBe messages("tax-year-summary.message.C15320")
        errorMessageList.child(20).text shouldBe messages("tax-year-summary.message.C15522")
        errorMessageList.child(21).text shouldBe messages("tax-year-summary.message.C15531")
        errorMessageList.child(22).text shouldBe messages("tax-year-summary.message.C15324")
        errorMessageList.child(23).text shouldBe messages("tax-year-summary.message.C55316")
        errorMessageList.child(24).text shouldBe messages("tax-year-summary.message.C55317")
        errorMessageList.child(25).text shouldBe messages("tax-year-summary.message.C55318")
        errorMessageList.child(26).text shouldBe messages("tax-year-summary.message.C55501")
        errorMessageList.child(27).text shouldBe messages("tax-year-summary.message.C55502")
        errorMessageList.child(28).text shouldBe messages("tax-year-summary.message.C55503")
        errorMessageList.child(29).text shouldBe messages("tax-year-summary.message.C55508")
        errorMessageList.child(30).text shouldBe messages("tax-year-summary.message.C55008", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(31).text shouldBe messages("tax-year-summary.message.C55011", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(32).text shouldBe messages("tax-year-summary.message.C55009")
        errorMessageList.child(33).text shouldBe messages("tax-year-summary.message.C55010")
        errorMessageList.child(34).text shouldBe messages("tax-year-summary.message.C55012", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(35).text shouldBe messages("tax-year-summary.message.C55013", dateService.getCurrentDate.toLongDate)
        errorMessageList.child(36).text shouldBe messages("tax-year-summary.message.C55009")
        errorMessageList.child(37).text shouldBe messages("tax-year-summary.message.C55511")
        errorMessageList.child(38).text shouldBe messages("tax-year-summary.message.C55519")
        errorMessageList.child(39).text shouldBe messages("tax-year-summary.message.C55515")
        errorMessageList.child(40).text shouldBe messages("tax-year-summary.message.C55516")
        errorMessageList.child(41).text shouldBe messages("tax-year-summary.message.C55517")
        errorMessageList.child(42).text shouldBe messages("tax-year-summary.message.C55520")
        errorMessageList.child(43).text shouldBe messages("tax-year-summary.message.C95005")
        errorMessageList.child(44).text shouldBe messages("tax-year-summary.message.C159014")
        errorMessageList.child(45).text shouldBe messages("tax-year-summary.message.C159015")
        errorMessageList.child(46).text shouldBe messages("tax-year-summary.message.C159016")
        errorMessageList.child(47).text shouldBe messages("tax-year-summary.message.C159018")
        errorMessageList.child(48).text shouldBe messages("tax-year-summary.message.C159019")
        errorMessageList.child(49).text shouldBe messages("tax-year-summary.message.C159026")
        errorMessageList.child(50).text shouldBe messages("tax-year-summary.message.C159027")
        errorMessageList.child(51).text shouldBe s"${messages("tax-year-summary.message.C159028.1")} ${messages("tax-year-summary.message.C159028.2")} ${messages("tax-year-summary.message.C159028.3")} ${messages("tax-year-summary.message.C159028.4")}"
        errorMessageList.child(52).text shouldBe messages("tax-year-summary.message.C159030")
        errorMessageList.child(53).text shouldBe messages("tax-year-summary.message.C159102")
        errorMessageList.child(54).text shouldBe messages("tax-year-summary.message.C159106")
        errorMessageList.child(55).text shouldBe messages("tax-year-summary.message.C159110", "50")
        errorMessageList.child(56).text shouldBe messages("tax-year-summary.message.C159115")
        errorMessageList.child(57).text shouldBe messages("tax-year-summary.message.C159500")
        errorMessageList.child(58).text shouldBe messages("tax-year-summary.message.C559099")
        errorMessageList.child(59).text shouldBe messages("tax-year-summary.message.C559100")
        errorMessageList.child(60).text shouldBe messages("tax-year-summary.message.C559101")
        errorMessageList.child(61).text shouldBe messages("tax-year-summary.message.C559103")
        errorMessageList.child(62).text shouldBe messages("tax-year-summary.message.C559107")
        errorMessageList.child(63).text shouldBe messages("tax-year-summary.message.C559104")
        errorMessageList.child(64).text shouldBe messages("tax-year-summary.message.C559105")
        errorMessageList.child(65).text shouldBe messages("tax-year-summary.message.C559113")
        errorMessageList.child(66).text shouldBe messages("tax-year-summary.message.C559114")

      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView()) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(2) th:nth-child(1) a")
        allowancesLink.text shouldBe allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "£2.02"
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

      "display the Balancing payment on the payments table when coding out is enabled and a zero amount" in new Setup(testBalancingPaymentChargeWithZeroValueView(codingOutEnabled = false)) {
        val paymentTypeText: Element = layoutContent.getElementById("paymentTypeText-0")
        val paymentTypeLinkOption: Option[Element] = Option(layoutContent.getElementById("paymentTypeLink-0"))
        val paymentTabRow: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(1)
        paymentTabRow.getElementsByClass("govuk-table__cell").first().text() shouldBe "N/A"
        paymentTabRow.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(0).toCurrencyString
        paymentTypeText.text shouldBe remainingBalance
        paymentTypeLinkOption.isEmpty shouldBe true
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
                "Quarterly period from " + messages("updateTab.dateToDate", testObligation.obligation.start.toLongDateShort, testObligation.obligation.end.toLongDateShort)
              divAccordion.selectHead("thead").selectNth("th", 1).text shouldBe updateType
              divAccordion.selectHead("thead").selectNth("th", 2).text shouldBe updateIncomeSource
              divAccordion.selectHead("thead").selectNth("th", 3).text shouldBe updateDateSubmitted
              val row = divAccordion.selectHead("tbody").selectHead("tr")
              row.selectNth("th", 1).text shouldBe updateType(testObligation.obligation.obligationType)
              row.selectNth("td", 1).text shouldBe incomeType(messages(testObligation.incomeType))
              row.selectNth("td", 2).text shouldBe testObligation.obligation.dateReceived.map(_.toLongDateShort).getOrElse("")
          }
        }
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

      "display empty updates table when no obligations are there" in new Setup(estimateView(obligations = ObligationsModel(Seq.empty))) {
        document.getElementById("updates").text() shouldBe ""
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

        document.select(".forecast_table tbody tr").size() shouldBe 4
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£4,200.00"
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£8,300.00"
        document.select(".forecast_table tbody tr:nth-child(4) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(4) td:nth-child(2)").text() shouldBe "£5,000.99"
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
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "£2.02"
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

      "display MFA Debits - Individual" in new Setup(mfaDebitsView(codingOutEnabled = false, isAgent = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe hmrcAdjustment
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, MFADebitsDocumentDetailsWithDueDates.head.documentDetail.transactionId).url
      }

      "display MFA Debits - Agent" in new Setup(mfaDebitsView(codingOutEnabled = false, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe hmrcAdjustment
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, MFADebitsDocumentDetailsWithDueDates.head.documentDetail.transactionId).url
      }

    }
  }

  "The TaxYearSummary view when missing mandatory fields" should {
    "throw a MissingFieldException" in {
      val thrownException = intercept[MissingFieldException] {
        taxYearSummaryView(
          taxYear = testYear,
          modelOpt = Some(modelComplete(Some(false))),
          charges = testWithMissingOriginalAmountChargesList,
          obligations = testObligationsModel,
          backUrl = "testBackURL",
          isAgent = false,
          codingOutEnabled = false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Original Amount"
    }
  }
}
