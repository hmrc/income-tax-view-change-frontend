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

package models.liabilitycalculation.view

import testConstants.NewCalcBreakdownTestConstants.liabilityCalculationModelSuccessFull
import testUtils.UnitSpec

class AllowancesAndDeductionsViewModelSpec extends UnitSpec {

  "AllowancesAndDeductionsViewModel model" when {
    "should create the Allowances and Deductions view model with a minimal Calculation object" in {
      val expectedAllowancesAndDeductionsViewModel = AllowancesAndDeductionsViewModel(
        personalAllowance = None,
        reducedPersonalAllowance = None,
        personalAllowanceBeforeTransferOut = None,
        transferredOutAmount = None,
        pensionContributions = None,
        lossesAppliedToGeneralIncome = None,
        giftOfInvestmentsAndPropertyToCharity = None,
        grossAnnuityPayments = None,
        qualifyingLoanInterestFromInvestments = None,
        postCessationTradeReceipts = None,
        paymentsToTradeUnionsForDeathBenefits = None,
        totalAllowancesAndDeductions = None,
        totalReliefs = None
      )

      AllowancesAndDeductionsViewModel().getAllowancesAndDeductionsViewModel(calcOpt = None) shouldBe expectedAllowancesAndDeductionsViewModel

    }

    "successful successModelFull" should {

      "should create the Allowances and Deductions view model" in {
        val expectedAllowancesAndDeductionsViewModel = AllowancesAndDeductionsViewModel(
          personalAllowance = Some(12500),
          reducedPersonalAllowance = Some(12500),
          personalAllowanceBeforeTransferOut = Some(5000.99),
          transferredOutAmount = Some(5000.99),
          pensionContributions = Some(5000.99),
          lossesAppliedToGeneralIncome = Some(12500),
          giftOfInvestmentsAndPropertyToCharity = Some(12500),
          grossAnnuityPayments = Some(5000.99),
          qualifyingLoanInterestFromInvestments = Some(5000.99),
          postCessationTradeReceipts = Some(5000.99),
          paymentsToTradeUnionsForDeathBenefits = Some(5000.99),
          totalAllowancesAndDeductions = Some(12500),
          totalReliefs = Some(5000.99)
        )

        AllowancesAndDeductionsViewModel().getAllowancesAndDeductionsViewModel(
          calcOpt = liabilityCalculationModelSuccessFull.calculation) shouldBe expectedAllowancesAndDeductionsViewModel

      }
    }
  }
}
