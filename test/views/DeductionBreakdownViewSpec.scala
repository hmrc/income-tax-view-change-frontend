/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.CalcBreakdownTestConstants
import assets.MessagesLookUp.{DeductionBreakdown, IncomeBreakdown}
import enums.Estimate
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.deductionBreakdown

class DeductionBreakdownViewSpec extends ViewSpec {

  object Breadcrumbs {
    val businessTaxAccount = "Business tax account"
    val home = "Income Tax account"
    val taxYears = "My tax years"
    def taxYear(start: Int, end: Int): String = s"6 April $start to 5 April $end"
    val deductions = "Allowances and deductions"
  }

  "The deduction breakdown view" when {

    "provided with a calculation without tax deductions for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = deductionBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationNoBillModel,
        Estimate), taxYear)

      "have the correct title" in new Setup(view) {
        document title() shouldBe DeductionBreakdown.title
      }

      "have the correct breadcrumbs" in new Setup(view) {
        val breadcrumbNav: Element = content.selectHead("#breadcrumbs")

        val first: Element = breadcrumbNav.selectHead("ol").selectNth("li", 1).selectHead("a")
        first.attr("href") shouldBe appConfig.businessTaxAccount
        first.text shouldBe Breadcrumbs.businessTaxAccount

        val second: Element = breadcrumbNav.selectHead("ol").selectNth("li", 2).selectHead("a")
        second.attr("href") shouldBe controllers.routes.HomeController.home().url
        second.text shouldBe Breadcrumbs.home

        val third: Element = breadcrumbNav.selectHead("ol").selectNth("li", 3).selectHead("a")
        third.attr("href") shouldBe controllers.routes.TaxYearsController.viewTaxYears().url
        third.text shouldBe Breadcrumbs.taxYears

        val forth: Element = breadcrumbNav.selectHead("ol").selectNth("li", 4).selectHead("a")
        forth.attr("href") shouldBe controllers.routes.CalculationController.renderCalculationPage(taxYear).url
        forth.text shouldBe Breadcrumbs.taxYear(taxYear - 1, taxYear)

        val fifth: Element = breadcrumbNav.selectHead("ol").selectNth("li", 5)
        fifth.text shouldBe Breadcrumbs.deductions
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading DeductionBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(1)
        guidance.text() shouldBe DeductionBreakdown.guidance(taxYear)
      }

      "have an deduction table" which {

        "has only one table row" in new Setup(view) {
          content hasTableWithCorrectSize (1, 1)
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = content.table().select("tr").first()
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£0"
        }
      }
    }

    "provided with a calculation with all tax deductions for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = deductionBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationAllDeductionSources,
        Estimate), taxYear)

      "have the correct title" in new Setup(view) {
        document title() shouldBe DeductionBreakdown.title
      }

      "have the correct breadcrumbs" in new Setup(view) {
        val breadcrumbNav: Element = content.selectHead("#breadcrumbs")

        val first: Element = breadcrumbNav.selectHead("ol").selectNth("li", 1).selectHead("a")
        first.attr("href") shouldBe appConfig.businessTaxAccount
        first.text shouldBe Breadcrumbs.businessTaxAccount

        val second: Element = breadcrumbNav.selectHead("ol").selectNth("li", 2).selectHead("a")
        second.attr("href") shouldBe controllers.routes.HomeController.home().url
        second.text shouldBe Breadcrumbs.home

        val third: Element = breadcrumbNav.selectHead("ol").selectNth("li", 3).selectHead("a")
        third.attr("href") shouldBe controllers.routes.TaxYearsController.viewTaxYears().url
        third.text shouldBe Breadcrumbs.taxYears

        val forth: Element = breadcrumbNav.selectHead("ol").selectNth("li", 4).selectHead("a")
        forth.attr("href") shouldBe controllers.routes.CalculationController.renderCalculationPage(taxYear).url
        forth.text shouldBe Breadcrumbs.taxYear(taxYear - 1, taxYear)

        val fifth: Element = breadcrumbNav.selectHead("ol").selectNth("li", 5)
        fifth.text shouldBe Breadcrumbs.deductions
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading DeductionBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(1)
        guidance.text() shouldBe DeductionBreakdown.guidance(taxYear)
      }

      "have an deduction table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize (1,5)
        }

        "has a personal allowance line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
          row.select("td").last().text() shouldBe "£11,500"
        }

        "has a pensions contributions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.totalPensionContributions
          row.select("td").last().text() shouldBe "£12,500"
        }

        "has a loss relief line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(2)
          row.select("td").first().text() shouldBe DeductionBreakdown.lossesAppliedToGeneralIncome
          row.select("td").last().text() shouldBe "£12,500"

        }

        "has a gift of investments and property to charity line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(3)
          row.select("td").first().text() shouldBe DeductionBreakdown.giftOfInvestmentsAndPropertyToCharity
          row.select("td").last().text() shouldBe "£10,000"
        }

        "has a total deductions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(4)
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£47,500"
        }
      }
    }
  }
}
