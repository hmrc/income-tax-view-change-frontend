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

package views.components

import config.featureswitch.FeatureSwitching
import models.liabilitycalculation.viewmodels.CalculationSummary
import models.liabilitycalculation.viewmodels.CalculationSummary.localDate
import models.taxyearsummary._
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import testConstants.ChargeConstants
import testUtils.ViewSpec
import views.html.partials.taxYearSummary.TaxCalculationOverview

import java.time.LocalDate

class TaxCalculationOverviewSpec extends ViewSpec with FeatureSwitching with ChargeConstants {

  val taxCalculationOverview: TaxCalculationOverview = app.injector.instanceOf[TaxCalculationOverview]

  val mockTestYear = 2025

  def buildCalculationSummary(crystallised: Boolean, unattendedCalc: Boolean = false, isAmended: Boolean = false): CalculationSummary =
    CalculationSummary(
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
      periodFrom = Some(LocalDate.of(mockTestYear - 1, 1, 1)),
      periodTo = Some(LocalDate.of(mockTestYear, 1, 1)),
      isAmended = isAmended
    )

  val taxCalcCannotBeDisplayedH3Id = "your-tax-calculation-cannot-be-displayed-heading"
  val notFiledCompatibleParagraph = "not-filed-compatible-software-paragraph"
  val notCompatibleSoftwareParagraph = "not-compatible-software"
  val viewTaxCalcLinkId = "view-your-tax-calculations-link"
  val filedByPostParagraphId = "filed-by-post-contact-hmrc"

  val viewTaxCalcLink = "https://www.tax.service.gov.uk/self-assessment/ind/UTR/account/taxyear/2025"
  val saTaxReturnsLink = "https://www.gov.uk/self-assessment-tax-returns"
  val contactHmrcLink = "https://www.gov.uk/find-hmrc-contacts/self-assessment-general-enquiries"

  "TaxCalculationOverview" when {

    "scenario is LegacyAndCesa" should {

      "return the correct content" in {

        val calculationSummary: CalculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            calculationSummaryModel = Some(calculationSummary),
            isAgent = false,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            taxYearViewScenarios = LegacyAndCesa,
            showNoTaxCalc = false,
            viewTaxCalcLink = Some(viewTaxCalcLink),
            selfAssessmentLink = saTaxReturnsLink,
            contactHmrcLink = contactHmrcLink
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById(taxCalcCannotBeDisplayedH3Id).text() shouldBe "Your tax calculation cannot be displayed"
        doc.getElementById(notFiledCompatibleParagraph).text() shouldBe "This is because your tax return was not filed using software compatible with Making Tax Digital for Income Tax."
        doc.getElementById(viewTaxCalcLinkId).text() shouldBe "View your tax calculation if you filed your tax return using the HMRC online service (opens in new tab)"
        doc.getElementById(notCompatibleSoftwareParagraph).text() shouldBe "If your return was submitted using software not compatible with Making Tax Digital for Income Tax, your tax calculation will be available in this software."
        doc.getElementById(filedByPostParagraphId).text() shouldBe "To view your tax calculation if you filed your return by post, contact HMRC (opens in new tab)."

        doc.getElementById("view-your-tax-calculations-link").attr("href") shouldBe viewTaxCalcLink
        doc.getElementById("contact-hmrc-link").attr("href") shouldBe contactHmrcLink
      }
    }

    "scenario is IrsaEnrolementHandedOff" should {

      "return the correct content" in {

        val calculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            calculationSummaryModel = Some(calculationSummary),
            isAgent = false,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            taxYearViewScenarios = IrsaEnrolementHandedOff,
            showNoTaxCalc = false,
            viewTaxCalcLink = Some(viewTaxCalcLink),
            selfAssessmentLink = saTaxReturnsLink,
            contactHmrcLink = contactHmrcLink
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById(taxCalcCannotBeDisplayedH3Id).text() shouldBe "Your tax calculation cannot be displayed"
        doc.getElementById(notFiledCompatibleParagraph).text() shouldBe "This is because your tax return was not filed using software compatible with Making Tax Digital for Income Tax."
        doc.getElementById(viewTaxCalcLinkId).text() shouldBe "View your tax calculation if you filed your tax return using the HMRC online service (opens in new tab)"
        doc.getElementById(notCompatibleSoftwareParagraph).text() shouldBe "If your return was submitted using software not compatible with Making Tax Digital for Income Tax, your tax calculation will be available in this software."
        doc.getElementById(filedByPostParagraphId).text() shouldBe "To view your tax calculation if you filed your return by post, contact HMRC (opens in new tab)."

        doc.getElementById("view-your-tax-calculations-link").attr("href") shouldBe viewTaxCalcLink
        doc.getElementById("contact-hmrc-link").attr("href") shouldBe contactHmrcLink
      }
    }

    "scenario is NoIrsaAEnrolement" should {

      "return the correct content" in {

        val calculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            calculationSummaryModel = Some(calculationSummary),
            isAgent = false,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            taxYearViewScenarios = NoIrsaAEnrolement,
            showNoTaxCalc = false,
            viewTaxCalcLink = Some(viewTaxCalcLink),
            selfAssessmentLink = saTaxReturnsLink,
            contactHmrcLink = contactHmrcLink
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById(taxCalcCannotBeDisplayedH3Id).text() shouldBe "Your tax calculation cannot be displayed"
        doc.getElementById(notFiledCompatibleParagraph).text() shouldBe "This is because your tax return was not filed using software compatible with Making Tax Digital for Income Tax."

        doc.getElementById("self-assessment-link").text() shouldBe "Self Assessment tax returns (opens in new tab)"
        doc.getElementById(notCompatibleSoftwareParagraph).text() shouldBe "If your return was submitted using software not compatible with Making Tax Digital for Income Tax, your tax calculation will be available in this software."

        doc.getElementById(filedByPostParagraphId).text() shouldBe "To view your tax calculation if you filed your return by post, contact HMRC (opens in new tab)."
        doc.getElementById("self-assessment-link").attr("href") shouldBe saTaxReturnsLink
        doc.getElementById("contact-hmrc-link").attr("href") shouldBe contactHmrcLink
      }
    }

    "scenario is AgentBefore2023TaxYear" should {

      "return the correct content" in {

        val calculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            calculationSummaryModel = Some(calculationSummary),
            isAgent = true,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            taxYearViewScenarios = AgentBefore2023TaxYear,
            showNoTaxCalc = false,
            viewTaxCalcLink = Some(viewTaxCalcLink),
            selfAssessmentLink = saTaxReturnsLink,
            contactHmrcLink = contactHmrcLink
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById(taxCalcCannotBeDisplayedH3Id).text() shouldBe "Your tax calculation cannot be displayed"
        doc.getElementById(notFiledCompatibleParagraph).text() shouldBe "This is because the tax return was not filed using software compatible with Making Tax Digital for Income Tax."
        doc.getElementById("filed-via-sa").text() shouldBe "You may have filed the tax return using your previous Self Assessment for Agents account (opens in new tab). If so, the calculation will be available there. The account will also use a different Government Gateway ID and password to your agent services account."
        doc.getElementById("not-compatible-software").text() shouldBe "If the return was submitted using software not compatible with Making Tax Digital for Income Tax, your tax calculation will be available in this software."
        doc.getElementById("filed-by-post-contact-hmrc-agent").text() shouldBe "To view your tax calculation if you filed the return by post, contact HMRC (opens in new tab)."

        doc.getElementById("self-assessment-for-agents-account").attr("href") shouldBe saTaxReturnsLink
        doc.getElementById("contact-hmrc-agent-link").attr("href") shouldBe contactHmrcLink
      }
    }

    "scenario is MtdSoftwareShowCalc" should {

      "return the correct content" in {

        val calculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            calculationSummaryModel = Some(calculationSummary),
            isAgent = false,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            taxYearViewScenarios = MtdSoftwareShowCalc,
            showNoTaxCalc = false,
            viewTaxCalcLink = Some(viewTaxCalcLink),
            selfAssessmentLink = saTaxReturnsLink,
            contactHmrcLink = contactHmrcLink
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById("calculation-panel-heading").text() shouldBe "Previous calculation"

        // to show the calculations table is present when the scenario is a successful one
        doc.getElementById("calculation-income-deductions-contributions-table").text() shouldBe "Calculation Section Amount Income £1.00 Allowances and deductions £2.02 Total income on which tax is due £3.00 Self Assessment tax amount £4.04"
      }
    }
  }
}
