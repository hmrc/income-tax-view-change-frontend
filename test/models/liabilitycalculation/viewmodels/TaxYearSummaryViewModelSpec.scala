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

package models.liabilitycalculation.viewmodels

import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

class TaxYearSummaryViewModelSpec extends UnitSpec {

  "TaxYearSummaryViewModel model" when {
    "create a minimal TaxYearSummaryViewModel when there is a minimal Calculation response" in {
      TaxYearSummaryViewModel(liabilityCalculationModelDeductionsMinimal(),2023) shouldBe
        TaxYearSummaryViewModel(
          timestamp = None,
          crystallised = None,
          unattendedCalc = false,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0,
          forecastIncome = None,
          forecastIncomeTaxAndNics = None
        )
    }

    "successful successModelFull" should {

      "create a full TaxYearSummaryViewModel when there is a full Calculation" in {
        val expectedTaxYearSummaryViewModel = TaxYearSummaryViewModel(
          timestamp = Some("2019-02-15T09:35:15.094Z"),
          crystallised = Some(true),
          unattendedCalc = false,
          taxDue = 5000.99,
          income = 12500,
          deductions = 17500.99,
          totalTaxableIncome = 12500,
          forecastIncome = Some(12500),
          forecastIncomeTaxAndNics = Some(5000.99)
        )

        TaxYearSummaryViewModel(liabilityCalculationModelSuccessful,taxYear = 2022) shouldBe expectedTaxYearSummaryViewModel
      }
    }

    "return unattendedCalc as true when calculationReason is 'unattendedCalculation'" in {
      TaxYearSummaryViewModel(liabilityCalculationModelDeductionsMinimal(calculationReason = Some("unattendedCalculation")),2023) shouldBe
        TaxYearSummaryViewModel(
          timestamp = None,
          crystallised = None,
          unattendedCalc = true,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0
        )
    }
    "return in year calculation quarter based on the valid calculation date and tax year" in {
      val calculationDate = "2023-02-15T09:35:15.094Z"
      val taxYear = 2023
      val result = TaxYearSummaryViewModel.getInYearCalculationRunDateQuater(calculationDate, taxYear)
      result shouldBe Some("third")
    }

    "return in None based on the invalid calculation date and tax year" in {
      val calculationDate = "2019-02-15T09:35:15.094Z"
      val taxYear = 2023
      val result = TaxYearSummaryViewModel.getInYearCalculationRunDateQuater(calculationDate, taxYear)
      result shouldBe None
    }
  }
}
