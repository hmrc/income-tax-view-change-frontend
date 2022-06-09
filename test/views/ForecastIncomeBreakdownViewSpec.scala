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

import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import views.html.ForecastIncomeSummary

class ForecastIncomeBreakdownViewSpec extends ViewSpec {

  val backUrl = "testUrl"

  val forecastIncomeTemplate: ForecastIncomeSummary = app.injector.instanceOf[ForecastIncomeSummary]

  val viewModelFull = EndOfYearEstimate(
    incomeSource = Some(List(
      IncomeSource(
        incomeSourceType = "01",
        incomeSourceName = Some("self-employment1"),
        taxableIncome = 2500
      ),
      IncomeSource(
        incomeSourceType = "01",
        incomeSourceName = Some("self-employment2"),
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "02",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "03",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "04",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "05",
        incomeSourceName = Some("employment1"),
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "05",
        incomeSourceName = Some("employment2"),
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "06",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "07",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "09",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "10",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "11",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "12",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "13",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "15",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "16",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "17",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "18",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "19",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "20",
        incomeSourceName = None,
        taxableIncome = 12500
      ),
      IncomeSource(
        incomeSourceType = "98",
        incomeSourceName = None,
        taxableIncome = 12500
      )
    )),
    totalEstimatedIncome = Some(12500),
    totalTaxableIncome = Some(12500),
    incomeTaxAmount = Some(5000.99),
    nic2 = Some(5000.99),
    nic4 = Some(5000.99),
    totalNicAmount = Some(5000.99),
    totalTaxDeductedBeforeCodingOut = Some(5000.99),
    saUnderpaymentsCodedOut = Some(5000.99),
    totalStudentLoansRepaymentAmount = Some(5000.99),
    totalAnnuityPaymentsTaxCharged = Some(5000.99),
    totalRoyaltyPaymentsTaxCharged = Some(5000.99),
    totalTaxDeducted = Some(-99999999999.99),
    incomeTaxNicAmount = Some(-99999999999.99),
    cgtAmount = Some(5000.99),
    incomeTaxNicAndCgtAmount = Some(5000.99)
  )


  "The forecast income summary view" when {

    "provided with a calculation without taxable incomes for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = forecastIncomeTemplate(viewModelFull, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe  messages("titlePattern.serviceName.govUk", messages("forecast_income.heading"))
      }

      "have the correct heading" in new Setup(view) {
        layoutContent hasPageHeading s"${messages("forecast_income.dates", "2016", "2017")} ${messages("forecast_income.heading")}"
      }

      "have the correct caption" in new Setup(view) {
        layoutContent.selectHead(" caption").text.contains(messages("forecast_income.heading"))
      }

      "have a forecasted income table" which {

        val expectedTableDataRows = Table(
          ("row index", "Income type", "Amount"),
          (1, messages("forecast_income.source_types.01", "self-employment1"), "£2,500.00"),
          (2, messages("forecast_income.source_types.01", "self-employment2"), "£12,500.00"),
          (3, messages("forecast_income.source_types.02"), "£12,500.00"),
          (4, messages("forecast_income.source_types.03"), "£12,500.00"),
          (5, messages("forecast_income.source_types.04"), "£12,500.00"),
          (6, messages("forecast_income.source_types.05", "employment1"), "£12,500.00"),
          (7, messages("forecast_income.source_types.05", "employment2"), "£12,500.00"),
          (8, messages("forecast_income.source_types.06"), "£12,500.00"),
          (9, messages("forecast_income.source_types.07"), "£12,500.00"),
          (10, messages("forecast_income.source_types.09"), "£12,500.00"),
          (11, messages("forecast_income.source_types.10"), "£12,500.00"),
          (12, messages("forecast_income.source_types.11"), "£12,500.00"),
          (13, messages("forecast_income.source_types.12"), "£12,500.00"),
          (14, messages("forecast_income.source_types.13"), "£12,500.00"),
          (15, messages("forecast_income.source_types.15"), "£12,500.00"),
          (16, messages("forecast_income.source_types.16"), "£12,500.00"),
          (17, messages("forecast_income.source_types.17"), "£12,500.00"),
          (18, messages("forecast_income.source_types.18"), "£12,500.00"),
          (19, messages("forecast_income.source_types.19"), "£12,500.00"),
          (20, messages("forecast_income.source_types.20"), "£12,500.00"),
          (21, messages("forecast_income.source_types.98"), "£12,500.00"),
          (22, messages("forecast_income.total"), "£12,500.00")
        )
        forAll(expectedTableDataRows) { (rowIndex: Int, deductionType: String, formattedAmount: String) =>

          s"has the row $rowIndex for $deductionType line with the correct amount value" in new Setup(view) {
            val row: Element = layoutContent.table().select("tr").get(rowIndex)
            row.select("td").first().text() shouldBe deductionType
            row.select("td").last().text() shouldBe formattedAmount
          }
        }
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = layoutContent.select("p").get(0)
        guidance.text() shouldBe messages("income_breakdown.guidance_software")
      }
    }
  }
}
