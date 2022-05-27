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

import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks._
import testConstants.BaseTestConstants.testNavHtml
import testUtils.ViewSpec
import views.html.DeductionBreakdown

class DeductionBreakdownViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"
  val deductions = "Allowances and deductions"
  val personalAllowance: String = messages("deduction_breakdown.table.personal_allowance")

  def deductionBreakdownView: DeductionBreakdown = app.injector.instanceOf[DeductionBreakdown]

  def subHeading(taxYear: Int): String =  messages("deduction_breakdown.dates", s"${taxYear - 1}", s"$taxYear")

  def heading(taxYear: Int): String = s"${subHeading(taxYear)} ${messages("deduction_breakdown.heading")}"

  "The deduction breakdown view" when {

    "provided with a btaNavPartial" should {
      val taxYear = 2017
      lazy val view = deductionBreakdownView(AllowancesAndDeductionsViewModel(), taxYear, backUrl, btaNavPartial = testNavHtml)

      "render the btaNavPartial" in new Setup(view) {
        document.getElementById(s"nav-bar-link-testEnHome").text shouldBe "testEnHome"
      }
    }

    "provided with a calculation without tax deductions for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = deductionBreakdownView(AllowancesAndDeductionsViewModel(), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe messages("titlePattern.serviceName.govUk", messages("deduction_breakdown.heading"))
      }

      "have a fallback backlink" in new Setup(view) {
        document hasFallbackBacklink()
      }

      "have the correct heading" in new Setup(view) {
        layoutContent hasPageHeading heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe messages("deduction_breakdown.guidance_software")
      }

      "have an deduction table" which {

        "has only two table row" in new Setup(view) {
          layoutContent hasTableWithCorrectSize(1, 2)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe messages("deduction_breakdown.table.header")
          row.select("th").last().text() shouldBe messages("deduction_breakdown.table.header.amount")
        }

        "has a total line with a zero value" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("deduction_breakdown.total")
          row.select("td").last().text() shouldBe "£0.00"
        }
      }
    }

    "provided with a calculation with all tax deductions for the 2018 tax year" should {
      val taxYear = 2018

      val deductions = AllowancesAndDeductionsViewModel(
        personalAllowance = Some(11500.00),
        reducedPersonalAllowance = None,
        personalAllowanceBeforeTransferOut = None,
        transferredOutAmount = Some(7500),
        pensionContributions = Some(12500),
        lossesAppliedToGeneralIncome = Some(13500),
        giftOfInvestmentsAndPropertyToCharity = Some(10000),
        grossAnnuityPayments = Some(1000),
        qualifyingLoanInterestFromInvestments = Some(1001),
        postCessationTradeReceipts = Some(1002),
        paymentsToTradeUnionsForDeathBenefits = Some(1003),
        totalAllowancesAndDeductions = Some(47000),
        totalReliefs = Some(500)
      )
      lazy val view = deductionBreakdownView(deductions, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe messages("titlePattern.serviceName.govUk", messages("deduction_breakdown.heading"))
      }

      "have the correct heading" in new Setup(view) {
        layoutContent hasPageHeading heading(taxYear)
        layoutContent.h1.select(".govuk-caption-xl").text() shouldBe subHeading(taxYear)
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(deductions)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe messages("deduction_breakdown.guidance_software")
      }

      "have an deduction table" which {

        val expectedBreakdownTableDataRows = Table(
          ("row index", "deduction type", "formatted amount"),
          (1, personalAllowance, "£11,500.00"),
          (2, messages("deduction_breakdown.table.marriage_allowance_transfer"), "−£7,500.00"),
          (3, messages("deduction_breakdown.table.pension_contributions"), "£12,500.00"),
          (4, messages("deduction_breakdown.table.loss_relief"), "£13,500.00"),
          (5, messages("deduction_breakdown.table.gift_of_investments_and_property_to_charity"), "£10,000.00"),
          (6, messages("deduction_breakdown.table.annual_payments"), "£1,000.00"),
          (7, messages("deduction_breakdown.table.qualifying_loan_interest"), "£1,001.00"),
          (8, messages("deduction_breakdown.table.post_cessasation_trade_receipts"), "£1,002.00"),
          (9, messages("deduction_breakdown.table.trade_union_payments"), "£1,003.00"),
          (10, messages("deduction_breakdown.total"), "£47,500.00")
        )

        "has all eleven table rows, including a header row" in new Setup(view) {
          layoutContent hasTableWithCorrectSize(1, 11)
        }

        "has a table header and amount section" in new Setup(view) {
          val row: Element = layoutContent.table().select("tr").get(0)
          row.select("th").first().text() shouldBe messages("deduction_breakdown.table.header")
          row.select("th").last().text() shouldBe messages("deduction_breakdown.table.header.amount")
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
          AllowancesAndDeductionsViewModel(
            personalAllowance = personalAllowance,
            reducedPersonalAllowance = reducedPersonalAllowance,
            personalAllowanceBeforeTransferOut = personalAllowanceBeforeTransferOut,
            transferredOutAmount = Some(1234.56),
            pensionContributions = Some(1234.56),
            lossesAppliedToGeneralIncome = Some(1234.56),
            giftOfInvestmentsAndPropertyToCharity = Some(1234.56),
            grossAnnuityPayments = Some(1234.56),
            qualifyingLoanInterestFromInvestments = Some(1234.56),
            postCessationTradeReceipts = Some(1234.56),
            paymentsToTradeUnionsForDeathBenefits = Some(1234.56),
            totalAllowancesAndDeductions = Some(1234.56),
            totalReliefs = Some(1234.56)
          ),
          taxYear2018, "testBackURL")
      )

      "only show one of the following: personalAllowanceBeforeTransferOut reducedPersonalAllowance personalAllowance" when {

        "all fields are present" in new DeductionBreakdownSetupVariedAllowances(Some(1200.00), Some(600.00)) {
          val row: Element = layoutContent.select("tr").get(1)
          row.select("td").first().text() shouldBe personalAllowance
          row.select("td").last().text() shouldBe "£1,200.00"
        }

        "all fields except personalAllowanceBeforeTransferOut" in
          new DeductionBreakdownSetupVariedAllowances(reducedPersonalAllowance = Some(600.00)) {
            val row: Element = layoutContent.select("tr").get(1)
            row.select("td").first().text() shouldBe personalAllowance
            row.select("td").last().text() shouldBe "£600.00"

          }

        "only personal Allowance is present" in new DeductionBreakdownSetupVariedAllowances() {
          val row: Element = layoutContent.select("tr").get(1)
          row.select("td").first().text() shouldBe personalAllowance
          row.select("td").last().text() shouldBe "£11,500.00"
        }
      }
    }
  }
}
