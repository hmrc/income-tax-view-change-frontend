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

import assets.MessagesLookUp.DeductionBreakdown
import assets.CalcBreakdownTestConstants
import enums.Estimate
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.DeductionBreakdown

class DeductionBreakdownViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"
  val deductions = "Allowances and deductions"


  def deductionBreakdownView: DeductionBreakdown = app.injector.instanceOf[DeductionBreakdown]

  "The deduction breakdown view" when {

    "provided with a calculation without tax deductions for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = deductionBreakdownView(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationNoBillModel,
        Estimate), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe DeductionBreakdown.title
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading DeductionBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        content.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance
      }

      "have an deduction table" which {

        "has only two table row" in new Setup(view) {
          content hasTableWithCorrectSize (1, 2)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe DeductionBreakdown.deductionBreakdownHeader
          row.select("th").last().text() shouldBe DeductionBreakdown.deductionBreakdownHeaderAmount
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£0.00"
        }
      }
    }

    "provided with a calculation with all tax deductions for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = deductionBreakdownView(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationAllDeductionSources,
        Estimate), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe DeductionBreakdown.title
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading DeductionBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        content.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance
      }

      "have an deduction table" which {

        "has all ten table rows" in new Setup(view) {
          content hasTableWithCorrectSize (1,10)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe DeductionBreakdown.deductionBreakdownHeader
          row.select("th").last().text() shouldBe DeductionBreakdown.deductionBreakdownHeaderAmount
        }

        "has a personal allowance line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
          row.select("td").last().text() shouldBe "£11,500.00"
        }

        "has a pensions contributions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(2)
          row.select("td").first().text() shouldBe DeductionBreakdown.totalPensionContributions
          row.select("td").last().text() shouldBe "£12,500.00"
        }

        "has a loss relief line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(3)
          row.select("td").first().text() shouldBe DeductionBreakdown.lossesAppliedToGeneralIncome
          row.select("td").last().text() shouldBe "£12,500.00"

        }

        "has a gift of investments and property to charity line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(4)
          row.select("td").first().text() shouldBe DeductionBreakdown.giftOfInvestmentsAndPropertyToCharity
          row.select("td").last().text() shouldBe "£10,000.00"
        }

        "has a annual payments line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(5)
          row.select("td").first().text() shouldBe DeductionBreakdown.annualPayments
          row.select("td").last().text() shouldBe "£1,000.00"
        }

        "has a qualifying loan interest line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(6)
          row.select("td").first().text() shouldBe DeductionBreakdown.loanInterest
          row.select("td").last().text() shouldBe "£1,001.00"
        }

        "has a post cessasation trade receipts line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(7)
          row.select("td").first().text() shouldBe DeductionBreakdown.postCessasationTradeReceipts
          row.select("td").last().text() shouldBe "£1,002.00"
        }

        "has a trade union payments line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(8)
          row.select("td").first().text() shouldBe DeductionBreakdown.tradeUnionPayments
          row.select("td").last().text() shouldBe "£1,003.00"
        }

        "has a total deductions line with the correct value" in new Setup(view) {
          val row: Element = content.table().select("tr").get(9)
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£47,500.00"
        }
      }
    }
  }
}
