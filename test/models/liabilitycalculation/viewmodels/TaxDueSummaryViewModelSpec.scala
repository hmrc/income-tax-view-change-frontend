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

package models.liabilitycalculation.viewmodels

import controllers.constants.IncomeSourceAddedControllerConstants.testObligationsModel
import exceptions.MissingFieldException
import models.liabilitycalculation.taxcalculation.{BusinessAssetsDisposalsAndInvestorsRel, CgtTaxBands, Nic4Bands, TaxBands}
import models.liabilitycalculation.{Message, Messages, ReliefsClaimed, StudentLoan}
import models.obligations._
import testConstants.BusinessDetailsTestConstants.{fixedDate, getCurrentTaxYearEnd}
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

class TaxDueSummaryViewModelSpec extends UnitSpec {

  "TaxDueSummaryViewModel model" when {
    "create a minimal TaxDueSummaryViewModel when there is a minimal Calculation response" in {
      TaxDueSummaryViewModel(liabilityCalculationModelDeductionsMinimal(), testObligationsModel) shouldBe
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
          totalStudentLoansRepaymentAmount = None, saUnderpaymentsCodedOut = None,
          totalIncomeTaxAndNicsDue = Some(0),
          totalTaxDeducted = None,
          taxDeductedAtSource = TaxDeductedAtSourceViewModel(None, None, None, None, None, None, None, None, None),
          finalDeclarationOrTaxReturnIsSubmitted = true
        )
    }

    "successful successModelFull" should {

      "create a full TaxDueSummaryViewModel when there is a full Calculation" in {
        val expectedTaxDueSummaryViewModel = TaxDueSummaryViewModel(
          taxRegime = "UK",
          class2VoluntaryContributions = true,
          messages = Some(Messages(
            Some(List(Message("C22211", "info msg text1"))),
            Some(List(Message("C22214", "warn msg text1"))),
            Some(List(Message("C22216", "error msg text1"))))
          ),
          lossesAppliedToGeneralIncome = Some(12500),
          grossGiftAidPayments = Some(12500),
          giftAidTax = Some(5000.99),
          marriageAllowanceTransferredInAmount = Some(5000.99),
          studentLoans = Some(List(StudentLoan(Some("01"), Some(5000.99), Some(5000.99), Some(5000.99), Some(5000.99), Some(1500), Some(20)))),
          reliefsClaimed = Some(List(ReliefsClaimed("vctSubscriptions", Some(5000.99)),
            ReliefsClaimed("deficiencyRelief", Some(5000.99)))),
          totalResidentialFinanceCostsRelief = Some(5000.99),
          totalForeignTaxCreditRelief = Some(5000.99),
          topSlicingReliefAmount = Some(5000.99),
          giftAidTaxReductionWhereBasicRateDiffers = Some(127.49),
          totalTaxableIncome = Some(12500),
          payPensionsProfitBands = Some(List(TaxBands("BRT", 20, 12500, 12500, 12500, 5000.99))),
          savingsAndGainsBands = Some(List(TaxBands("ZRT", 0, 12500, 12500, 12500, 0))),
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
            adjustments = Some(-2500.99),
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
          saUnderpaymentsCodedOut = Some(-2500.99),
          totalIncomeTaxAndNicsDue = Some(5000.99),
          totalTaxDeducted = Some(50000.99),
          taxDeductedAtSource = TaxDeductedAtSourceViewModel(
            payeEmployments = Some(5000.99),
            ukPensions = Some(5000.99),
            stateBenefits = Some(5000.99),
            cis = Some(5000.99),
            ukLandAndProperty = Some(5000.99),
            specialWithholdingTax = Some(5000.99),
            voidISAs = Some(5000.99),
            savings = Some(5000.99),
            inYearAdjustmentCodedInLaterTaxYear = Some(5000.99),
            taxTakenOffTradingIncome = Some(563.12)
          ),
          giftAidTaxChargeWhereBasicRateDiffers = Some(6565.99),
          transitionProfitRow = TransitionProfitRow(Some(700.00), Some(3000.00)),
          finalDeclarationOrTaxReturnIsSubmitted = true
        )

        TaxDueSummaryViewModel(liabilityCalculationModelSuccessful, testObligationsModel) shouldBe expectedTaxDueSummaryViewModel
      }

      "create a full TaxDueSummaryViewModel when there is a full Calculation for Income tax and CGT " in {
        val taxDue = 6000
        val expectedTaxDueSummaryViewModel = TaxDueSummaryViewModel(
          taxRegime = "UK",
          class2VoluntaryContributions = true,
          messages = Some(Messages(
            Some(List(Message("C22211", "info msg text1"))),
            Some(List(Message("C22214", "warn msg text1"))),
            Some(List(Message("C22216", "error msg text1"))))
          ),
          lossesAppliedToGeneralIncome = Some(12500),
          grossGiftAidPayments = Some(12500),
          giftAidTax = Some(5000.99),
          marriageAllowanceTransferredInAmount = Some(5000.99),
          studentLoans = Some(List(StudentLoan(Some("01"), Some(5000.99), Some(5000.99), Some(5000.99), Some(5000.99), Some(1500), Some(20)))),
          reliefsClaimed = Some(List(ReliefsClaimed("vctSubscriptions", Some(5000.99)),
            ReliefsClaimed("deficiencyRelief", Some(5000.99)))),
          totalResidentialFinanceCostsRelief = Some(5000.99),
          totalForeignTaxCreditRelief = Some(5000.99),
          topSlicingReliefAmount = Some(5000.99),
          giftAidTaxReductionWhereBasicRateDiffers = Some(127.49),
          totalTaxableIncome = Some(12500),
          payPensionsProfitBands = Some(List(TaxBands("BRT", 20, 12500, 12500, 12500, 5000.99))),
          savingsAndGainsBands = Some(List(TaxBands("ZRT", 0, 12500, 12500, 12500, 0))),
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
            adjustments = Some(-2500.99),
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
          saUnderpaymentsCodedOut = Some(-2500.99),
          totalIncomeTaxAndNicsDue = Some(6000),
          totalTaxDeducted = Some(50000.99),
          taxDeductedAtSource = TaxDeductedAtSourceViewModel(
            payeEmployments = Some(5000.99),
            ukPensions = Some(5000.99),
            stateBenefits = Some(5000.99),
            cis = Some(5000.99),
            ukLandAndProperty = Some(5000.99),
            specialWithholdingTax = Some(5000.99),
            voidISAs = Some(5000.99),
            savings = Some(5000.99),
            inYearAdjustmentCodedInLaterTaxYear = Some(5000.99),
            taxTakenOffTradingIncome = Some(563.12)
          ),
          giftAidTaxChargeWhereBasicRateDiffers = Some(6565.99),
          transitionProfitRow = TransitionProfitRow(Some(700.00), Some(3000.00)),
          finalDeclarationOrTaxReturnIsSubmitted = true
        )

        val liabilityCalculationModel = liabilityCalculationModelSuccessful.copy(
          calculation = Some(liabilityCalculationModelSuccessful.calculation.get.copy(
            taxCalculation = Some(liabilityCalculationModelSuccessful.calculation.get.taxCalculation.get.copy(
              totalIncomeTaxAndNicsAndCgt = Some(taxDue))))))

        TaxDueSummaryViewModel(liabilityCalculationModel, testObligationsModel) shouldBe expectedTaxDueSummaryViewModel
      }
    }
    "grossGiftAidPaymentsActual" should{
      "return value when value is present" in{
        val amount = BigDecimal(1000.0)
        val model = TaxDueSummaryViewModel(grossGiftAidPayments = Some(amount))

        model.grossGiftAidPaymentsActual shouldBe amount
      }

      "throw MissingFieldException when value is not present" in{
        val model = TaxDueSummaryViewModel()

        intercept[MissingFieldException] {
          model.grossGiftAidPaymentsActual
        }
      }
    }

    "getModifiedBaseTaxBandActual" should{
      "return value when value is present" in{
        val taxBand = TaxBands("BRT", BigDecimal(1.0), 1, 1, 1, BigDecimal(1000.0))

        val model = TaxDueSummaryViewModel(payPensionsProfitBands = Some(Seq(taxBand)))

        model.getModifiedBaseTaxBandActual shouldBe taxBand
      }

      "throw MissingFieldException when value is not present" in{
        val model = TaxDueSummaryViewModel()

        intercept[MissingFieldException] {
          model.getModifiedBaseTaxBandActual
        }
      }
    }

    "lossesAppliedToGeneralIncomeActual" should{
      "return value when value is present" in{

        val model = TaxDueSummaryViewModel(lossesAppliedToGeneralIncome = Some(0))

        model.lossesAppliedToGeneralIncomeActual shouldBe 0
      }

      "throw MissingFieldException when value is not present" in{
        val model = TaxDueSummaryViewModel()

        intercept[MissingFieldException] {
          model.lossesAppliedToGeneralIncomeActual
        }
      }
    }

    "giftAidTaxActual" should{
      "return value when value is present" in{
        val amount = BigDecimal(1000.0)
        val model = TaxDueSummaryViewModel(giftAidTax = Some(amount))

        model.giftAidTaxActual shouldBe amount
      }

      "throw MissingFieldException when value is not present" in{
        val model = TaxDueSummaryViewModel()

        intercept[MissingFieldException] {
          model.giftAidTaxActual
        }
      }
    }
  }

  "TransitionProfitRow" when {
    "incomeTaxCharged and totalTaxableProfit are available" should {
      "return TransitionProfitRow" in {
        TransitionProfitRow(Some(BigDecimal(300.00)), Some(BigDecimal(300.00))).get shouldBe TransitionProfitRow(300.00, 300.00)
      }
    }
    "only one field is available" should {
      "return None" in {
        TransitionProfitRow(None, Some(BigDecimal(300.00))) shouldBe None
        TransitionProfitRow(Some(BigDecimal(300.00)), None) shouldBe None
      }
    }
    "all fields are missing" should {
      "return None" in {
        TransitionProfitRow(None, None) shouldBe None
      }
    }
  }
}