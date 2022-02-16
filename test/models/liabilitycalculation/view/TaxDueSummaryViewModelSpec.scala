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

import models.liabilitycalculation.{Message, Messages, ReliefsClaimed}
import models.liabilitycalculation.taxcalculation.{BusinessAssetsDisposalsAndInvestorsRel, CgtTaxBands, Nic4Bands, TaxBands}
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

class TaxDueSummaryViewModelSpec extends UnitSpec {

  "TaxDueSummaryViewModel model" when {
    "create a minimal TaxDueSummaryViewModel when there is a minimal Calculation response" in {
      TaxDueSummaryViewModel(liabilityCalculationModelDeductionsMinimal) shouldBe
        TaxDueSummaryViewModel(
          taxRegime = "UK",
          messages = None, lossesAppliedToGeneralIncome = None,
          grossGiftAidPayments = None, giftAidTax = None,
          marriageAllowanceTransferredInAmount = None,
          reliefsClaimed = None,
          totalResidentialFinanceCostsRelief = None,
          totalForeignTaxCreditRelief = None,
          topSlicingReliefAmount = None, totalTaxableIncome = None,
          payPensionsProfitBands = None, savingsAndGainsBands = None,
          lumpSumsBands = None, dividendsBands = None, gainsOnLifePoliciesBands = None,
          totalNotionalTax = None, incomeTaxDueAfterTaxReductions = None,
          totalPensionSavingsTaxCharges = None, statePensionLumpSumCharges = None,
          payeUnderpaymentsCodedOut = None, nic4Bands = None, class2NicsAmount = None,
          capitalGainsTax = CapitalGainsTaxViewModel(None, None, None, None, None, None, None, None, None),
          totalStudentLoansRepaymentAmount = None, saUnderpaymentsCodedOut = None, totalIncomeTaxAndNicsDue = None,
          totalTaxDeducted = None,
          taxDeductedAtSource = TaxDeductedAtSourceViewModel(None, None, None, None, None, None, None, None, None)
        )
    }

    "successful successModelFull" should {

      "create a full TaxDueSummaryViewModel when there is a full Calculation" in {
        val expectedTaxDueSummaryViewModel = TaxDueSummaryViewModel(
          taxRegime = "UK",
          class2VoluntaryContributions = true,
          messages = Some(Messages(
            Some(List(Message("infoId1", "info msg text1"))),
            Some(List(Message("warnId1", "warn msg text1"))),
            Some(List(Message("errorId1", "error msg text1"))))
          ),
          lossesAppliedToGeneralIncome = Some(12500),
          grossGiftAidPayments = Some(12500),
          giftAidTax = Some(5000.99),
          marriageAllowanceTransferredInAmount = Some(5000.99),
          reliefsClaimed = Some(List(ReliefsClaimed("vctSubscriptions", Some(5000.99)),
            ReliefsClaimed("vctSubscriptions2", Some(5000.99)))),
          totalResidentialFinanceCostsRelief = Some(5000.99),
          totalForeignTaxCreditRelief = Some(5000.99),
          topSlicingReliefAmount = Some(5000.99),
          totalTaxableIncome = Some(12500),
          payPensionsProfitBands = Some(List(TaxBands("SSR", 20, 12500, 12500, 12500, 5000.99))),
          savingsAndGainsBands = Some(List(TaxBands("SSR", 20, 12500, 12500, 12500, 5000.99))),
          lumpSumsBands = Some(List(TaxBands("SSR", 20, 12500, 12500, 12500, 5000.99))),
          dividendsBands = Some(List(TaxBands("SSR", 20, 12500, 12500, 12500, 5000.99))),
          gainsOnLifePoliciesBands = Some(List(TaxBands("SSR", 20, 12500, 12500, 12500, 5000.99))),
          totalNotionalTax = Some(5000.99),
          incomeTaxDueAfterTaxReductions = Some(5000.99),
          totalPensionSavingsTaxCharges = Some(5000.99),
          statePensionLumpSumCharges = Some(5000.99),
          payeUnderpaymentsCodedOut = Some(5000.99),
          nic4Bands = Some(List(Nic4Bands("ZRT", 12500, 20, 5000.99))),
          class2NicsAmount = Some(5000.99),
          capitalGainsTax = CapitalGainsTaxViewModel(
            totalTaxableGains = Some(5000.99),
            adjustments = Some(-99999999999.99),
            foreignTaxCreditRelief = Some(5000.99),
            taxOnGainsAlreadyPaid = Some(5000.99),
            capitalGainsTaxDue = Some(5000.99),
            capitalGainsOverpaid = Some(5000.99),
            propertyAndInterestTaxBands = Some(List(CgtTaxBands("lowerRate", 20, 5000.99, 5000.99),
              CgtTaxBands("lowerRate2", 21, 5000.99, 5000.99))),
            otherGainsTaxBands = Some(List(CgtTaxBands("lowerRate", 20, 5000.99, 5000.99),
              CgtTaxBands("lowerRate2", 21, 5000.99, 5000.99))),
            businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(Some(5000.99),
              Some(20), Some(5000.99)))),
          totalStudentLoansRepaymentAmount = Some(5000.99),
          saUnderpaymentsCodedOut = Some(-99999999999.99),
          totalIncomeTaxAndNicsDue = Some(-99999999999.99),
          totalTaxDeducted = Some(-99999999999.99),
          taxDeductedAtSource = TaxDeductedAtSourceViewModel(
            payeEmployments = Some(5000.99),
            ukPensions = Some(5000.99),
            stateBenefits = Some(-99999999999.99),
            cis = Some(5000.99),
            ukLandAndProperty = Some(5000.99),
            specialWithholdingTax = Some(5000.99),
            voidISAs = Some(5000.99),
            savings = Some(5000.99),
            inYearAdjustmentCodedInLaterTaxYear = Some(5000.99))
        )

        TaxDueSummaryViewModel(liabilityCalculationModelSuccessFull) shouldBe expectedTaxDueSummaryViewModel
      }
    }
  }
}
