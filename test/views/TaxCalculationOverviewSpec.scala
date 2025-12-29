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

package views

import config.featureswitch.FeatureSwitching
import models.liabilitycalculation.viewmodels.CalculationSummary
import models.liabilitycalculation.viewmodels.CalculationSummary.localDate
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

  "TaxCalculationOverview" when {

    "scenrio A" should {

      "return the correct content " in {

        val calculationSummary = buildCalculationSummary(crystallised = false)

        val partial: HtmlFormat.Appendable =
          taxCalculationOverview(
            model = calculationSummary,
            isAgent = false,
            taxYear = 2025,
            isLatest = true,
            isPrevious = true,
            pfaEnabled = true,
            isAmended = true,
            scenario = "scenarioA",
            "",
            ""
          )

        val doc = Jsoup.parse(partial.body)

        doc.getElementById("your-tax-calculation-cannot-be-displayed-heading") shouldBe ""
      }
    }
  }


}
