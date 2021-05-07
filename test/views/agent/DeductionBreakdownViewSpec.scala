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

import assets.MessagesLookUp.DeductionBreakdown
import assets.CalcBreakdownTestConstants
import config.FrontendAppConfig
import enums.Estimate
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.agent.DeductionBreakdown

class DeductionBreakdownViewSpec extends ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val deductionBreakdown: DeductionBreakdown = app.injector.instanceOf[DeductionBreakdown]
  val taxYear2017 = 2017

  class DeductionBreakdownSetup() extends Setup(
    deductionBreakdown( CalcDisplayModel("", 1,
      CalcBreakdownTestConstants.calculationNoBillModel,
      Estimate),taxYear2017,"testBackURL")
  )

  "The deduction breakdown view" when {

    "provided with a calculation without tax deductions for the 2017 tax year" should {
      val taxYear2017 = 2017



      "have the correct title" in new DeductionBreakdownSetup {
        document title() shouldBe DeductionBreakdown.agentTitle
      }

      "have the correct heading" in new DeductionBreakdownSetup {
        content hasPageHeading DeductionBreakdown.heading(taxYear2017)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear2017)
      }

      "have the correct guidance" in new DeductionBreakdownSetup {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance(taxYear2017)
      }

      "have an deduction table" which {

        "has only one table row" in new DeductionBreakdownSetup {
          content hasTableWithCorrectSize (1, 1)
        }

        "has a total line with a zero value" in new DeductionBreakdownSetup {
          val row: Element = content.table().select("tr").first()
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£0.00"
        }
      }
    }

    "provided with a calculation with all tax deductions for the 2018 tax year" should {
      val taxYear2018 = 2018

      class DeductionBreakdownSetup2018() extends Setup(
        deductionBreakdown(CalcDisplayModel("", 1,
          CalcBreakdownTestConstants.calculationAllDeductionSources,
          Estimate),taxYear2018,"testBackURL")
      )

      "have the correct title" in new DeductionBreakdownSetup2018 {
        document title() shouldBe DeductionBreakdown.agentTitle
      }

      "have the correct agent heading" in new DeductionBreakdownSetup2018 {
        content hasPageHeading DeductionBreakdown.heading(taxYear2018)
        content.h1.select(".heading-secondary").text() shouldBe DeductionBreakdown.subHeading(taxYear2018)
      }

      "have the correct guidance" in new DeductionBreakdownSetup2018 {
        val guidance: Element = content.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance(taxYear2018)
      }

      "have an deduction table" which {

        "has all nine table rows" in new DeductionBreakdownSetup2018 {
          content hasTableWithCorrectSize (1,9)
        }

        "has a personal allowance line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(0)
          row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
          row.select("td").last().text() shouldBe "£11,500.00"
        }

        "has a pensions contributions line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.totalPensionContributions
          row.select("td").last().text() shouldBe "£12,500.00"
        }

        "has a loss relief line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(2)
          row.select("td").first().text() shouldBe DeductionBreakdown.lossesAppliedToGeneralIncome
          row.select("td").last().text() shouldBe "£12,500.00"

        }

        "has a gift of investments and property to charity line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(3)
          row.select("td").first().text() shouldBe DeductionBreakdown.giftOfInvestmentsAndPropertyToCharity
          row.select("td").last().text() shouldBe "£10,000.00"
        }

        "has a annual payments line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(4)
          row.select("td").first().text() shouldBe DeductionBreakdown.annualPayments
          row.select("td").last().text() shouldBe "£1,000.00"
        }

        "has a qualifying loan interest line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(5)
          row.select("td").first().text() shouldBe DeductionBreakdown.loanInterest
          row.select("td").last().text() shouldBe "£1,001.00"
        }

        "has a post cessasation trade receipts line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(6)
          row.select("td").first().text() shouldBe DeductionBreakdown.postCessasationTradeReceipts
          row.select("td").last().text() shouldBe "£1,002.00"
        }

        "has a trade union payments line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(7)
          row.select("td").first().text() shouldBe DeductionBreakdown.tradeUnionPayments
          row.select("td").last().text() shouldBe "£1,003.00"
        }

        "has a total deductions line with the correct value" in new DeductionBreakdownSetup2018 {
          val row: Element = content.table().select("tr").get(8)
          row.select("td").first().text() shouldBe DeductionBreakdown.total
          row.select("td").last().text() shouldBe "£47,500.00"
        }
      }
    }
  }
}
