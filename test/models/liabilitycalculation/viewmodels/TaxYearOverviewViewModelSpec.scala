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

import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelDeductionsMinimal, liabilityCalculationModelSuccessFull}
import testUtils.UnitSpec

class TaxYearOverviewViewModelSpec extends UnitSpec {

  "TaxYearOverviewViewModel" should {
    "create a minimal TaxYearOverviewViewModel when there is a minimal calculation response" in {
      TaxYearOverviewViewModel(liabilityCalculationModelDeductionsMinimal()) shouldBe
        TaxYearOverviewViewModel(
          timestamp = None,
          crystallised = None,
          unattendedCalc = false,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0
        )
    }

    "create a full TaxYearOverviewViewModel when there is a full calculation response" in {
      TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull) shouldBe
        TaxYearOverviewViewModel(
          timestamp = Some("2019-02-15T09:35:15.094Z"),
          crystallised = Some(true),
          unattendedCalc = false,
          taxDue = 5000.99,
          income = 12500,
          deductions = 17500.99,
          totalTaxableIncome = 12500
        )
    }

    "return unattendedCalc as true when calculationReason is 'unattendedCalculation'" in {
      TaxYearOverviewViewModel(liabilityCalculationModelDeductionsMinimal(calculationReason = Some("unattendedCalculation"))) shouldBe
        TaxYearOverviewViewModel(
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
