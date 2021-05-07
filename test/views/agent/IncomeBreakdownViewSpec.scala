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

package views.agent

import assets.CalcBreakdownTestConstants
import assets.MessagesLookUp.IncomeBreakdown
import enums.Estimate
import models.calculation.{AllowancesAndDeductions, CalcDisplayModel, CalcOverview, Calculation}
import models.financialTransactions.TransactionModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.agent.IncomeBreakdown

class IncomeBreakdownViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/agent/calculation/2021"

  val incomeBreakdown: IncomeBreakdown = app.injector.instanceOf[IncomeBreakdown]
  val testYear: Int = 2020
  val testCalcOverview: CalcOverview = CalcOverview(
    calculation = Calculation(
      crystallised = true,
      timestamp = Some("2020-04-06T12:34:56.789Z"),
      totalIncomeTaxAndNicsDue = Some(100.00),
      totalIncomeReceived = Some(150.00),
      allowancesAndDeductions = AllowancesAndDeductions(totalAllowancesAndDeductions = Some(25.00), totalReliefs = Some(25.00)),
      totalTaxableIncome = Some(30.00)
    ),
    transaction = Some(TransactionModel())
  )

  "The income breakdown view" when {

    "provided with a calculation without taxable incomes for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = incomeBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationNoBillModel,
        Estimate), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe IncomeBreakdown.agentTitle
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading IncomeBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe IncomeBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe IncomeBreakdown.guidance(taxYear)
      }

      "have an income table" which {

        "has only one table row" in new Setup(view) {
          content hasTableWithCorrectSize(1, 1)
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = content.table().select("tr").first()
          row.select("td").first().text() shouldBe IncomeBreakdown.total
          row.select("td").last().text() shouldBe "£0.00"
        }
      }
    }

    "provided with a calculation with all taxable incomes for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = incomeBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationAllIncomeSources,
        Estimate), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe IncomeBreakdown.agentTitle
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading IncomeBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe IncomeBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe IncomeBreakdown.guidance(taxYear)
      }

      "have an income table" which {

        "has all twenty one table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 21)
        }

        "has a employment income line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("td").first().text() shouldBe IncomeBreakdown.employments
          row.select("td").last().text() shouldBe "£5,005.05"
        }

        "has a benefits and expenses income line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(1)
          row.select("td").first().text() shouldBe IncomeBreakdown.benefitsAndExpenses
          row.select("td").last().text() shouldBe "£6,006.06"
        }

        "has an allowable expenses line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(2)
          row.select("td").first().text() shouldBe IncomeBreakdown.allowableExpenses
          row.select("td").last().text() shouldBe "-£7,007.07"
        }

        "has a total self-employment profit line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(3)
          row.select("td").first().text() shouldBe IncomeBreakdown.selfEmployments
          row.select("td").last().text() shouldBe "£1,001.01"
        }

        "has a total property profit line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(4)
          row.select("td").first().text() shouldBe IncomeBreakdown.property
          row.select("td").last().text() shouldBe "£2,002.02"
        }

        "has an Profit from UK furnished line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(5)
          row.select("td").first().text() shouldBe IncomeBreakdown.profitUkFurnished
          row.select("td").last().text() shouldBe "£6,003.00"
        }

        "has an Profit from foreign properties line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(6)
          row.select("td").first().text() shouldBe IncomeBreakdown.profitFromForeignProperties
          row.select("td").last().text() shouldBe "£6,004.00"
        }

        "has an Profit from Eea holiday line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(7)
          row.select("td").first().text() shouldBe IncomeBreakdown.profitFromEeaHoliday
          row.select("td").last().text() shouldBe "£6,005.00"
        }

        "has an foreign dividends income line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(8)
          row.select("td").first().text() shouldBe IncomeBreakdown.foreignDividendsIncome
          row.select("td").last().text() shouldBe "£7,026.00"
        }

        "has an foreign savings income line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(9)
          row.select("td").first().text() shouldBe IncomeBreakdown.foreignSavingsIncome
          row.select("td").last().text() shouldBe "£7,019.00"
        }

        "has an foreign pensions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(10)
          row.select("td").first().text() shouldBe IncomeBreakdown.foreignPensions
          row.select("td").last().text() shouldBe "£6,006.00"
        }

        "has an foreign income received whilst abroad line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(11)
          row.select("td").first().text() shouldBe IncomeBreakdown.incomeReceivedAbroad
          row.select("td").last().text() shouldBe "£6,007.00"
        }

        "has an foreign income and gains line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(12)
          row.select("td").first().text() shouldBe IncomeBreakdown.foreignincomeAndGains
          row.select("td").last().text() shouldBe "£6,008.00"
        }

        "has an foreign benefits and gifts line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(13)
          row.select("td").first().text() shouldBe IncomeBreakdown.foreignBenefitsAndGifts
          row.select("td").last().text() shouldBe "£6,009.00"
        }

        "has a total savings profit line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(14)
          row.select("td").first().text() shouldBe IncomeBreakdown.bbsi
          row.select("td").last().text() shouldBe "£3,003.03"
        }

        "has an gains on life insurance policies line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(15)
          row.select("td").first().text() shouldBe IncomeBreakdown.gainsOnInsurancePolicy
          row.select("td").last().text() shouldBe "£7,015.00"
        }

        "has a total dividends profit line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(16)
          row.select("td").first().text() shouldBe IncomeBreakdown.dividends
          row.select("td").last().text() shouldBe "£4,004.04"
        }

        "has an occupational pensions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(17)
          row.select("td").first().text() shouldBe IncomeBreakdown.occupationalPensions
          row.select("td").last().text() shouldBe "£8,008.08"
        }

        "has an state benefits line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(18)
          row.select("td").first().text() shouldBe IncomeBreakdown.stateBenefits
          row.select("td").last().text() shouldBe "£9,009.09"
        }

        "has an share scheme line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(19)
          row.select("td").first().text() shouldBe IncomeBreakdown.shareSchemes
          row.select("td").last().text() shouldBe "£6,010.00"
        }

        "has a total line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(20)
          row.select("td").first().text() shouldBe IncomeBreakdown.total
          row.select("td").last().text() shouldBe "£10,010.10"
        }
      }
    }
  }
}
