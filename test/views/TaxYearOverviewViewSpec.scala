/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.MessagesLookUp.Breadcrumbs
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.calculation.CalcOverview
import models.financialDetails.Charge
import models.financialTransactions.TransactionModel
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.taxYearOverview

class TaxYearOverviewViewSpec extends ViewSpec {

  val testYear: Int = 2020

  def completeOverview(crystallised: Boolean): CalcOverview = CalcOverview(
    timestamp = Some("2020-01-01T00:35:34.185Z"),
    income = 1.01,
    deductions = 2.02,
    totalTaxableIncome = 3.03,
    taxDue = 4.04,
    payment = 5.05,
    totalRemainingDue = 6.06,
    crystallised = crystallised
  )

  val transactionModel: TransactionModel = TransactionModel(
    clearedAmount = Some(7.07),
    outstandingAmount = Some(8.08)
  )

  val chargeModel: Charge = Charge(
		taxYear = "2018",
		transactionId = "id",
    totalAmount = Some(7.07),
    outstandingAmount = Some(8.08)
  )

  def estimateView: Html = taxYearOverview(testYear, completeOverview(false), mockImplicitDateFormatter)

  def crystallisedView: Html = taxYearOverview(testYear, completeOverview(true), mockImplicitDateFormatter)

  object taxYearOverviewMessages{
    val title: String = "Tax year overview - Business Tax account - GOV.UK"
    val heading: String = "Tax year overview"
    val secondaryHeading: String = s"6 April ${testYear - 1} to 5 April $testYear"
    val calculationDate: String = "Calculation date"
    val calcDate: String = "1 January 2020"
    val estimate: String = s"6 April ${testYear - 1} to 1 January 2020 estimate"
    val totalDue: String = "Total Due"
    val taxDue: String = "£4.04"
    val calcDateInfo: String = "This calculation is from the last time you viewed your tax calculation in your own software. You will need to view it in your software for the most up to date version."
    val taxCalculation: String = "Tax Calculation"
    val payments: String = "Payments"
    val updates: String = "Updates"
    val income: String = "Income"
    val allowancesAndDeductions: String = "Allowances and deductions"
    val totalIncomeDue: String = "Total income on which tax is due"
    val incomeTaxNationalInsuranceDue: String = "Income Tax and National Insurance contributions due"
  }

  "taxYearOverview" should {
    "have the correct title" in new Setup(estimateView) {
      document.title shouldBe taxYearOverviewMessages.title
    }

    "have a breadcrumb trail" in new Setup(estimateView) {
      content.selectHead("#breadcrumb-bta").text shouldBe Breadcrumbs.bta
      content.selectHead("#breadcrumb-bta").attr("href") shouldBe appConfig.businessTaxAccount
      content.selectHead("#breadcrumb-it").text shouldBe Breadcrumbs.it
      content.selectHead("#breadcrumb-it").attr("href") shouldBe controllers.routes.HomeController.home().url
      content.selectHead("#breadcrumb-tax-years").text shouldBe Breadcrumbs.taxYears
      content.selectHead("#breadcrumb-tax-years").attr("href") shouldBe controllers.routes.TaxYearsController.viewTaxYears().url
      content.selectHead("#breadcrumb-tax-year-overview").text shouldBe Breadcrumbs.taxYearOverview(testYear - 1, testYear)
      content.selectHead("#breadcrumb-tax-year-overview").hasAttr("href") shouldBe false
    }

    "have the correct heading" in new Setup(estimateView) {
      content.selectHead("  h1").text shouldBe taxYearOverviewMessages.heading
    }

    "have the correct secondary heading" in new Setup(estimateView) {
      content.selectHead("header > p").text shouldBe taxYearOverviewMessages.secondaryHeading
    }

    "display the calculation date" in new Setup(estimateView) {
      content.selectHead("dl > div:nth-child(1) > dd:nth-child(1)").text shouldBe taxYearOverviewMessages.calculationDate
      content.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe taxYearOverviewMessages.calcDate
    }

    "display the estimate due for an ongoing tax year" in new Setup(estimateView) {
      content.selectHead("dl > div:nth-child(2) > dd:nth-child(1)").text shouldBe taxYearOverviewMessages.estimate
      content.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe completeOverview(false).taxDue.toCurrencyString
    }

    "display the total due for a crystallised year" in new Setup(crystallisedView) {
      content.selectHead("dl > div:nth-child(2) > dd:nth-child(1)").text shouldBe taxYearOverviewMessages.totalDue
      content.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe completeOverview(true).taxDue.toCurrencyString
    }

    "have a paragraph explaining the calc date for an ongoing year" in new Setup(estimateView) {
      content.selectHead(".panel").text shouldBe taxYearOverviewMessages.calcDateInfo
    }

    "not have a paragraph explaining the calc date for a crystallised year" in new Setup(crystallisedView) {
      content.getOptionalSelector(".panel") shouldBe None
    }

    "show three tabs with the correct tab headings" in new Setup(estimateView) {
      content.selectHead("#tab_taxCalculation").text shouldBe taxYearOverviewMessages.taxCalculation
      content.selectHead("#tab_payments").text shouldBe taxYearOverviewMessages.payments
      content.selectHead("#tab_updates").text shouldBe taxYearOverviewMessages.updates
    }

    "when in an ongoing year should display the correct heading in the Tax Calculation tab" in new Setup(estimateView) {
      content.selectHead("#taxCalculation > h2").text shouldBe taxYearOverviewMessages.estimate
    }

    "when in a crystallised year should display the correct heading in the Tax Calculation tab" in new Setup(crystallisedView) {
      content.selectHead("#taxCalculation > h2").text shouldBe taxYearOverviewMessages.taxCalculation
    }

    "display the income row" in new Setup(estimateView) {
      val incomeLink = content.selectHead(" #income-deductions-table tr:nth-child(1) td:nth-child(1) a")
      incomeLink.text shouldBe taxYearOverviewMessages.income
      incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url
      content.selectHead("#income-deductions-table tr:nth-child(1) .numeric").text shouldBe completeOverview(false).income.toCurrencyString
    }

    "display the Allowances and deductions row" in new Setup(estimateView) {
      val allowancesLink = content.selectHead(" #income-deductions-table tr:nth-child(2) td:nth-child(1) a")
      allowancesLink.text shouldBe taxYearOverviewMessages.allowancesAndDeductions
      allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
      content.selectHead("#income-deductions-table tr:nth-child(2) .numeric").text shouldBe completeOverview(false).deductions.toNegativeCurrencyString
    }

    "display the Total income on which tax is due row" in new Setup(estimateView) {
      content.selectHead(".total-section:nth-child(1)").text shouldBe taxYearOverviewMessages.totalIncomeDue
      content.selectHead(".total-section:nth-child(2)").text shouldBe completeOverview(false).totalTaxableIncome.toCurrencyString
    }

    "display the Income Tax and National Insurance Contributions Due row" in new Setup(estimateView) {
      val totalTaxDueLink = content.selectHead("#taxdue-payments-table td:nth-child(1) a")
      totalTaxDueLink.text shouldBe taxYearOverviewMessages.incomeTaxNationalInsuranceDue
      totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummary(testYear).url
      content.selectHead("#taxdue-payments-table td:nth-child(2)").text shouldBe completeOverview(false).taxDue.toCurrencyString
    }
  }
}
