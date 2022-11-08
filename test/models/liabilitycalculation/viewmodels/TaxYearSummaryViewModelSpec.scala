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

import implicits.ImplicitDateParser
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate

class TaxYearSummaryViewModelSpec extends UnitSpec with ImplicitDateParser {

  "TaxYearSummaryViewModel model" when {
    "create a minimal TaxYearSummaryViewModel when there is a minimal Calculation response" in {
      TaxYearSummaryViewModel(liabilityCalculationModelDeductionsMinimal()) shouldBe
        TaxYearSummaryViewModel(
          timestamp = None,
          crystallised = None,
          unattendedCalc = false,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0,
          forecastIncome = None,
          forecastIncomeTaxAndNics = None,
          forecastAllowancesAndDeductions = None,
          periodFrom = None,
          periodTo = None
        )
    }

    "successful successModelFull" should {

      "create a full TaxYearSummaryViewModel when there is a full Calculation" in {
        val expectedTaxYearSummaryViewModel = TaxYearSummaryViewModel(
          timestamp = Some("2019-02-15T09:35:15.094Z".toZonedDateTime.toLocalDate),
          crystallised = Some(true),
          unattendedCalc = false,
          taxDue = 5000.99,
          income = 12500,
          deductions = 17500.99,
          totalTaxableIncome = 12500,
          forecastIncome = Some(12500),
          forecastIncomeTaxAndNics = Some(5000.99),
          forecastAllowancesAndDeductions = Some(4200.00),
          forecastTotalTaxableIncome = Some(8300),
          periodFrom = Some(LocalDate.of(2018,1,1)),
          periodTo = Some(LocalDate.of(2019,1,1))
        )

        TaxYearSummaryViewModel(liabilityCalculationModelSuccessful) shouldBe expectedTaxYearSummaryViewModel
      }

      "create a full TaxYearSummaryViewModel with taxDue as totalIncomeTaxAndNicsAndCgt" when {
          "the 'totalIncomeTaxAndNicsAndCgt' is available" in {
            val taxDue = 6000
            val expectedTaxYearSummaryViewModel = TaxYearSummaryViewModel(
              timestamp = Some("2019-02-15T09:35:15.094Z".toZonedDateTime.toLocalDate),
              crystallised = Some(true),
              unattendedCalc = false,
              taxDue = taxDue,
              income = 12500,
              deductions = 17500.99,
              totalTaxableIncome = 12500,
              forecastIncome = Some(12500),
              forecastIncomeTaxAndNics = Some(5000.99),
              forecastAllowancesAndDeductions = Some(4200.00),
              forecastTotalTaxableIncome = Some(8300),
              periodFrom = Some(LocalDate.of(2018, 1, 1)),
              periodTo = Some(LocalDate.of(2019, 1, 1))
            )

            val liabilityCalculationModel = liabilityCalculationModelSuccessful.copy(
              calculation = Some(liabilityCalculationModelSuccessful.calculation.get.copy(
                taxCalculation = Some(liabilityCalculationModelSuccessful.calculation.get.taxCalculation.get.copy(
                  totalIncomeTaxAndNicsAndCgt = Some(taxDue))))))

            TaxYearSummaryViewModel(liabilityCalculationModel) shouldBe expectedTaxYearSummaryViewModel
          }
        }
      }
    }

    "return unattendedCalc as true when calculationReason is 'unattendedCalculation'" in {
      TaxYearSummaryViewModel(liabilityCalculationModelDeductionsMinimal(calculationReason = Some("unattendedCalculation"))) shouldBe
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
  }
}
