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

import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.IncomeBreakdown

class IncomeBreakdownViewSpec extends ViewSpec {

  val backUrl = "testUrl"

  val incomeBreakdown: IncomeBreakdown = app.injector.instanceOf[IncomeBreakdown]

  val income = "Income"

  val emptyIncomeBreakdownViewModel: IncomeBreakdownViewModel = IncomeBreakdownViewModel(
    totalPayeEmploymentAndLumpSumIncome = None,
    totalBenefitsInKind = None,
    totalEmploymentExpenses = None,
    totalSelfEmploymentProfit = None,
    totalPropertyProfit = None,
    totalFHLPropertyProfit = None,
    totalForeignPropertyProfit = None,
    totalEeaFhlProfit = None,
    chargeableForeignDividends = None,
    chargeableForeignSavingsAndGains = None,
    chargeableOverseasPensionsStateBenefitsRoyalties = None,
    chargeableAllOtherIncomeReceivedWhilstAbroad = None,
    totalOverseasIncomeAndGains = None,
    totalForeignBenefitsAndGifts = None,
    savingsAndGainsTaxableIncome = None,
    totalOfAllGains = None,
    dividendsTaxableIncome = None,
    totalOccupationalPensionIncome = None,
    totalStateBenefitsIncome = None,
    totalShareSchemesIncome = None,
    totalIncomeReceived = None
  )

  val fullIncomeBreakdownViewModel: IncomeBreakdownViewModel = IncomeBreakdownViewModel(
    totalPayeEmploymentAndLumpSumIncome = Some(5005.05),
    totalBenefitsInKind = Some(6006.06),
    totalEmploymentExpenses = Some(7007.07),
    totalSelfEmploymentProfit = Some(1001.01),
    totalPropertyProfit = Some(2002.02),
    totalFHLPropertyProfit = Some(6003.00),
    totalForeignPropertyProfit = Some(6004.00),
    totalEeaFhlProfit = Some(6005.00),
    chargeableForeignDividends = Some(7026.00),
    chargeableForeignSavingsAndGains = Some(7019.00),
    chargeableOverseasPensionsStateBenefitsRoyalties = Some(6006.00),
    chargeableAllOtherIncomeReceivedWhilstAbroad = Some(6007.00),
    totalOverseasIncomeAndGains = Some(6008.00),
    totalForeignBenefitsAndGifts = Some(6009.00),
    savingsAndGainsTaxableIncome = Some(3003.03),
    totalOfAllGains = Some(7015.00),
    dividendsTaxableIncome = Some(4004.04),
    totalOccupationalPensionIncome = Some(8008.08),
    totalStateBenefitsIncome = Some(9009.09),
    totalShareSchemesIncome = Some(6010.00),
    totalIncomeReceived = Some(10010.10)
  )

  def subHeading(taxYear: Int): String = messages("income_breakdown.dates", s"${taxYear - 1}", s"$taxYear")

  def heading(taxYear: Int): String = s"${subHeading(taxYear)} ${messages("income_breakdown.heading")}"


  "The income breakdown view" when {

    "provided with a calculation without taxable incomes for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = incomeBreakdown(emptyIncomeBreakdownViewModel, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe messages("titlePattern.serviceName.govUk", messages("income_breakdown.heading"))
      }

      "have a fallback backlink" in new Setup(view) {
        document hasFallbackBacklink()
      }

      "have the correct heading" in new Setup(view) {
        layoutContent hasPageHeading heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(income)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe messages("income_breakdown.guidance_software")
      }

      "have an income table" which {

        "has two table rows" in new Setup(view) {
          layoutContent hasTableWithCorrectSize(1, 2)
        }
        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe messages("income_breakdown.table.header")
          row.select("th").last().text() shouldBe messages("income_breakdown.table.header.amount")
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("income_breakdown.total")
          row.select("td").last().text() shouldBe "£0.00"
        }
      }
    }

    "provided with a calculation with all taxable incomes for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = incomeBreakdown(fullIncomeBreakdownViewModel, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe messages("titlePattern.serviceName.govUk", messages("income_breakdown.heading"))
      }

      "have the correct heading" in new Setup(view) {
        layoutContent hasPageHeading heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(income)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe messages("income_breakdown.guidance_software")
      }

      "have an income table" which {

        "has all twenty two table rows" in new Setup(view) {
          layoutContent hasTableWithCorrectSize(1, 22)
        }
        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe messages("income_breakdown.table.header")
          row.select("th").last().text() shouldBe messages("income_breakdown.table.header.amount")
        }
        "has a employment income line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.employment")
          row.select("td").last().text() shouldBe "£5,005.05"
        }

        "has a benefits and expenses income line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(2)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.benefits_received")
          row.select("td").last().text() shouldBe "£6,006.06"
        }

        "has an allowable expenses line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(3)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.allowable_expenses")
          row.select("td").last().text() shouldBe "−£7,007.07"
        }

        "has a total self-employment profit line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(4)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.self_employment")
          row.select("td").last().text() shouldBe "£1,001.01"
        }

        "has a total property profit line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(5)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.property")
          row.select("td").last().text() shouldBe "£2,002.02"
        }

        "has an Profit from UK furnished line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(6)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.profit_uk_furnished_holiday")
          row.select("td").last().text() shouldBe "£6,003.00"
        }

        "has an Profit from foreign properties line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(7)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.profit_foreign_property")
          row.select("td").last().text() shouldBe "£6,004.00"
        }

        "has an Profit from Eea holiday line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(8)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.profit_eea_holiday")
          row.select("td").last().text() shouldBe "£6,005.00"
        }

        "has an foreign dividends income line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(9)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_dividends_income")
          row.select("td").last().text() shouldBe "£7,026.00"
        }

        "has an foreign savings income line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(10)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_saving_income")
          row.select("td").last().text() shouldBe "£7,019.00"
        }

        "has an foreign pensions line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(11)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_pensions")
          row.select("td").last().text() shouldBe "£6,006.00"
        }

        "has an foreign income received whilst abroad line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(12)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_income_abroad")
          row.select("td").last().text() shouldBe "£6,007.00"
        }

        "has an foreign income and gains line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(13)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_income_gains")
          row.select("td").last().text() shouldBe "£6,008.00"
        }

        "has an foreign benefits and gifts line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(14)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.foreign_benefits_gifts")
          row.select("td").last().text() shouldBe "£6,009.00"
        }

        "has a total savings profit line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(15)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.bbsi")
          row.select("td").last().text() shouldBe "£3,003.03"
        }

        "has an gains on life insurance policies line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(16)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.gains_insurance")
          row.select("td").last().text() shouldBe "£7,015.00"
        }

        "has a total dividends profit line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(17)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.dividends")
          row.select("td").last().text() shouldBe "£4,004.04"
        }

        "has an occupational pensions line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(18)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.occupational_pensions")
          row.select("td").last().text() shouldBe "£8,008.08"
        }

        "has an state benefits line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(19)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.state_benefit")
          row.select("td").last().text() shouldBe "£9,009.09"
        }

        "has an share scheme line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(20)
          row.select("td").first().text() shouldBe messages("income_breakdown.table.share_schemes")
          row.select("td").last().text() shouldBe "£6,010.00"
        }

        "has a total line with the correct value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(21)
          row.select("td").first().text() shouldBe messages("income_breakdown.total")
          row.select("td").last().text() shouldBe "£10,010.10"
        }
      }
    }
  }
}
