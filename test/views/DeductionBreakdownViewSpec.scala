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

import testConstants.CalcBreakdownTestConstants
import testConstants.MessagesLookUp.DeductionBreakdown
import enums.Estimate
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks._
import testConstants.CalcBreakdownTestConstants.allowancesAndDeductions
import testConstants.EstimatesTestConstants.testYearPlusOne
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
        layoutContent hasPageHeading DeductionBreakdown.heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance
      }

      "have an deduction table" which {

        "has only two table row" in new Setup(view) {
          layoutContent hasTableWithCorrectSize (1, 2)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe DeductionBreakdown.deductionBreakdownHeader
          row.select("th").last().text() shouldBe DeductionBreakdown.deductionBreakdownHeaderAmount
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(1)
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
        layoutContent hasPageHeading DeductionBreakdown.heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe DeductionBreakdown.subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe DeductionBreakdown.guidance
      }

      "have an deduction table" which {

        val expectedBreakdownTableDataRows = Table(
          ("row index", "deduction type", "formatted amount"),
          (1, DeductionBreakdown.personalAllowance, "£11,500.00"),
          (2, DeductionBreakdown.marriageAllowanceTransfer, "−£7,500.00"),
          (3, DeductionBreakdown.totalPensionContributions, "£12,500.00"),
          (4, DeductionBreakdown.lossesAppliedToGeneralIncome, "£13,500.00"),
          (5, DeductionBreakdown.giftOfInvestmentsAndPropertyToCharity, "£10,000.00"),
          (6, DeductionBreakdown.annualPayments, "£1,000.00"),
          (7, DeductionBreakdown.loanInterest, "£1,001.00"),
          (8, DeductionBreakdown.postCessasationTradeReceipts, "£1,002.00"),
          (9, DeductionBreakdown.tradeUnionPayments, "£1,003.00"),
          (10, DeductionBreakdown.total, "£47,500.00")
        )

        "has all eleven table rows, including a header row" in new Setup(view) {
          layoutContent hasTableWithCorrectSize (1, 11)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe DeductionBreakdown.deductionBreakdownHeader
          row.select("th").last().text() shouldBe DeductionBreakdown.deductionBreakdownHeaderAmount
        }

        forAll(expectedBreakdownTableDataRows) { (rowIndex: Int, deductionType: String, formattedAmount: String) =>

          s"has the row $rowIndex for $deductionType line with the correct amount value" in new Setup(view) {
            val row: Element = layoutContent.table().select("tr").get(rowIndex)
            row.select("td").first().text() shouldBe deductionType
            row.select("td").last().text() shouldBe formattedAmount
          }

        }
      }
    }

    "presenting personal allowances" should {
      val taxYear2018 = 2018

      class DeductionBreakdownSetupVariedAllowances(personalAllowanceBeforeTransferOut: Option[BigDecimal] = None,
                                                    reducedPersonalAllowance: Option[BigDecimal] = None,
                                                    personalAllowance: Option[BigDecimal] = Some(11500.00)) extends Setup(
        deductionBreakdownView(
          CalcDisplayModel("", 1,
            CalcBreakdownTestConstants.calculationAllDeductionSources.copy(
              allowancesAndDeductions = allowancesAndDeductions.copy(
                personalAllowance = personalAllowance,
                reducedPersonalAllowance = reducedPersonalAllowance,
                personalAllowanceBeforeTransferOut = personalAllowanceBeforeTransferOut
              )
            ),
            Estimate
          ),
          taxYear2018, "testBackURL")
      )

      "only show one of the following: personalAllowanceBeforeTransferOut reducedPersonalAllowance personalAllowance" when {

        "all fields are present" in new DeductionBreakdownSetupVariedAllowances(Some(1200.00), Some(600.00)) {
          val row: Element = layoutContent.select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
          row.select("td").last().text() shouldBe "£1,200.00"
        }

        "all fields except personalAllowanceBeforeTransferOut" in
          new DeductionBreakdownSetupVariedAllowances(reducedPersonalAllowance = Some(600.00)){
            val row: Element = layoutContent.select("tr").get(1)
            row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
            row.select("td").last().text() shouldBe "£600.00"

          }

        "only personal Allowance is present" in new DeductionBreakdownSetupVariedAllowances(){
          val row: Element = layoutContent.select("tr").get(1)
          row.select("td").first().text() shouldBe DeductionBreakdown.personalAllowance
          row.select("td").last().text() shouldBe "£11,500.00"
        }
      }
    }
  }
}
