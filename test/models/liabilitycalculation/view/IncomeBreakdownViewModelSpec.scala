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

import models.liabilitycalculation.Calculation
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessFull

import testUtils.UnitSpec

class IncomeBreakdownViewModelSpec extends UnitSpec {

  "IncomeBreakdownViewModel model" when {
    "create a minimal IncomeBreakdownViewModel when there is a minimal Calculation" in {
      IncomeBreakdownViewModel(Some(Calculation())) shouldBe Some(
        IncomeBreakdownViewModel(None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None))
    }

    "create a empty IncomeBreakdownViewModel when there is a no Calculation" in {
      IncomeBreakdownViewModel(None) shouldBe None
    }

    "successful successModelFull" should {

      "create a full IncomeBreakdownViewModel when there is a full Calculation" in {
        val expectedIncomeBreakdownViewModel = IncomeBreakdownViewModel(
          totalPayeEmploymentAndLumpSumIncome = Some(5000.99),
          totalBenefitsInKind = Some(5000.99),
          totalEmploymentExpenses = Some(5000.99),
          totalSelfEmploymentProfit = Some(12500),
          totalPropertyProfit = Some(12500),
          totalFHLPropertyProfit = Some(12500),
          totalForeignPropertyProfit = Some(12500),
          totalEeaFhlProfit = Some(12500),
          chargeableForeignDividends = Some(12500),
          chargeableForeignSavingsAndGains = Some(12500),
          chargeableOverseasPensionsStateBenefitsRoyalties = Some(5000.99),
          chargeableAllOtherIncomeReceivedWhilstAbroad = Some(5000.99),
          totalOverseasIncomeAndGains = Some(5000.99),
          totalForeignBenefitsAndGifts = Some(5000.99),
          savingsAndGainsTaxableIncome = Some(12500),
          totalOfAllGains = Some(12500),
          dividendsTaxableIncome = Some(12500),
          totalOccupationalPensionIncome = Some(5000.99),
          totalStateBenefitsIncome = Some(5000.99),
          totalShareSchemesIncome = Some(5000.99),
          totalIncomeReceived = Some(12500)
        )

        IncomeBreakdownViewModel(Some(liabilityCalculationModelSuccessFull.calculation.get)) shouldBe Some(expectedIncomeBreakdownViewModel)
      }
    }
  }
}
