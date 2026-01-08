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

package testConstants

import controllers.ForecastIncomeSummaryControllerTestConstants.taxableIncome
import enums.{MTDIndividual, MTDUserRole}
import models.liabilitycalculation._
import models.liabilitycalculation.taxcalculation._

object NewCalcBreakdownItTestConstants {

  val liabilityCalculationModelError: LiabilityCalculationError = LiabilityCalculationError(432, "someerrorhere")

  val liabilityCalculationModelDeductionsMinimal: LiabilityCalculationResponse =
    LiabilityCalculationResponse(
      inputs = Inputs(personalInformation = PersonalInformation(
        taxRegime = "UK", class2VoluntaryContributions = None
      )),
      calculation = Some(Calculation(
        allowancesAndDeductions = Some(AllowancesAndDeductions()))),
      metadata = Metadata(
        calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
        calculationType = "crystallisation",
        calculationReason = Some("customerRequest"),
      ),
      messages = None,
      submissionChannel = None
    )

  val liabilityCalculationModelSuccessful: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1")))
    )),
    calculation = Some(Calculation(
      allowancesAndDeductions = Some(AllowancesAndDeductions(
        personalAllowance = Some(12500),
        reducedPersonalAllowance = Some(12500),
        marriageAllowanceTransferOut = Some(MarriageAllowanceTransferOut(
          personalAllowanceBeforeTransferOut = 5000.99,
          transferredOutAmount = 5000.99)),
        pensionContributions = Some(5000.99),
        lossesAppliedToGeneralIncome = Some(12500),
        giftOfInvestmentsAndPropertyToCharity = Some(12500),
        grossAnnuityPayments = Some(5000.99),
        qualifyingLoanInterestFromInvestments = Some(5000.99),
        postCessationTradeReceipts = Some(5000.99),
        paymentsToTradeUnionsForDeathBenefits = Some(5000.99))),
      chargeableEventGainsIncome = Some(ChargeableEventGainsIncome(
        totalOfAllGains = 12500
      )),
      dividendsIncome = Some(DividendsIncome(chargeableForeignDividends = Some(12500))),
      employmentAndPensionsIncome = Some(EmploymentAndPensionsIncome(
        totalPayeEmploymentAndLumpSumIncome = Some(5000.99),
        totalBenefitsInKind = Some(5000.99),
        totalOccupationalPensionIncome = Some(5000.99)
      )),
      employmentExpenses = Some(EmploymentExpenses(totalEmploymentExpenses = Some(5000.99))),
      foreignIncome = Some(ForeignIncome(
        chargeableOverseasPensionsStateBenefitsRoyalties = Some(5000.99),
        chargeableAllOtherIncomeReceivedWhilstAbroad = Some(5000.99),
        overseasIncomeAndGains = Some(OverseasIncomeAndGains(gainAmount = 5000.99)),
        totalForeignBenefitsAndGifts = Some(5000.99)
      )),
      giftAid = Some(GiftAid(
        grossGiftAidPayments = 12500,
        giftAidTax = 5000.99
      )),
      incomeSummaryTotals = Some(IncomeSummaryTotals(
        totalSelfEmploymentProfit = Some(12500),
        totalPropertyProfit = Some(12500),
        totalFHLPropertyProfit = Some(12500),
        totalForeignPropertyProfit = Some(12500),
        totalEeaFhlProfit = Some(12500)
      )),
      marriageAllowanceTransferredIn = Some(MarriageAllowanceTransferredIn(amount = Some(5000.99))),
      studentLoans = Some(Seq(
        StudentLoan(
          planType = Option("01"),
          studentLoanRepaymentAmount = Some(BigDecimal(5000.99))
        ),
        StudentLoan(
          planType = Option("03"),
          studentLoanRepaymentAmount = Some(BigDecimal(5000.99)
          )
        ))),
      reliefs = Some(Reliefs(reliefsClaimed = Some(Seq(ReliefsClaimed(
        `type` = "vctSubscriptions",
        amountUsed = Some(5000.99)),
        ReliefsClaimed(
          `type` = "vctSubscriptions2",
          amountUsed = Some(5000.99))
      )),
        residentialFinanceCosts = Some(ResidentialFinanceCosts(totalResidentialFinanceCostsRelief = 5000.99)),
        foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(totalForeignTaxCreditRelief = 5000.99)),
        topSlicingRelief = Some(TopSlicingRelief(amount = Some(5000.99))),
        giftAidTaxReductionWhereBasicRateDiffers = Some(GiftAidTaxReductionWhereBasicRateDiffers(amount = Some(127.49))))
      ),
      savingsAndGainsIncome = Some(SavingsAndGainsIncome(
        chargeableForeignSavingsAndGains = Some(12500)
      )),
      shareSchemesIncome = Some(ShareSchemesIncome(
        totalIncome = 5000.99
      )),
      otherIncome = Some(OtherIncome(totalOtherIncome = 500.00)),
      stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99),
        totalStateBenefitsIncomeExcStatePensionLumpSum = Some(5000.99))),
      taxCalculation = Some(TaxCalculation(
        incomeTax = IncomeTax(
          totalIncomeReceivedFromAllSources = 12500,
          totalAllowancesAndDeductions = 12500,
          totalTaxableIncome = 12500,
          payPensionsProfit = Some(PayPensionsProfit(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          savingsAndGains = Some(SavingsAndGains(
            taxableIncome = 12500,
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          dividends = Some(Dividends(
            taxableIncome = 12500,
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          lumpSums = Some(LumpSums(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          gainsOnLifePolicies = Some(GainsOnLifePolicies(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          totalReliefs = Some(5000.99),
          totalNotionalTax = Some(5000.99),
          incomeTaxDueAfterTaxReductions = Some(5000.99),
          totalPensionSavingsTaxCharges = Some(5000.99),
          statePensionLumpSumCharges = Some(5000.99),
          payeUnderpaymentsCodedOut = Some(5000.99),
          incomeTaxChargedOnTransitionProfits = Some(700.00)
        ),
        nics = Some(Nics(
          class4Nics = Some(Class4Nics(nic4Bands = Seq(Nic4Bands(
            name = "ZRT",
            income = 12500,
            rate = 20,
            amount = 5000.99
          )))),
          class2Nics = Some(Class2Nics(amount = Some(5000.99)))
        )),
        capitalGainsTax = Some(CapitalGainsTax(
          totalTaxableGains = 5000.99,
          adjustments = Some(-99999999999.99),
          foreignTaxCreditRelief = Some(5000.99),
          taxOnGainsAlreadyPaid = Some(5000.99),
          capitalGainsTaxDue = 5000.99,
          capitalGainsOverpaid = Some(5000.99),
          residentialPropertyAndCarriedInterest = Some(ResidentialPropertyAndCarriedInterest(
            cgtTaxBands = Some(Seq(CgtTaxBands(
              name = "lowerRate",
              rate = 20,
              income = 5000.99,
              taxAmount = 5000.99
            ),
              CgtTaxBands(
                name = "lowerRate2",
                rate = 21,
                income = 5000.99,
                taxAmount = 5000.99
              )))
          )),
          otherGains = Some(OtherGains(
            cgtTaxBands = Some(Seq(CgtTaxBands(
              name = "lowerRate",
              rate = 20,
              income = 5000.99,
              taxAmount = 5000.99
            ),
              CgtTaxBands(
                name = "lowerRate2",
                rate = 21,
                income = 5000.99,
                taxAmount = 5000.99
              )))
          )),
          businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(
            taxableGains = Some(5000.99),
            rate = Some(20),
            taxAmount = Some(5000.99)
          ))
        )),
        totalStudentLoansRepaymentAmount = Some(5000.99),
        saUnderpaymentsCodedOut = Some(-99999999999.99),
        totalIncomeTaxAndNicsDue = 90500.99,
        totalTaxDeducted = Some(-20500.99)
      )),
      endOfYearEstimate = Some(EndOfYearEstimate(
        incomeSource = Some(List(
          IncomeSource("01", Some("self-employment1"), taxableIncome),
          IncomeSource("01", Some("self-employment2"), taxableIncome),
          IncomeSource("02", None, taxableIncome),
          IncomeSource("03", None, taxableIncome),
          IncomeSource("04", None, taxableIncome),
          IncomeSource("05", Some("employment1"), taxableIncome),
          IncomeSource("05", Some("employment2"), taxableIncome),
          IncomeSource("06", None, taxableIncome),
          IncomeSource("07", None, taxableIncome),
          IncomeSource("08", None, taxableIncome),
          IncomeSource("09", None, taxableIncome),
          IncomeSource("10", None, taxableIncome),
          IncomeSource("11", None, taxableIncome),
          IncomeSource("12", None, taxableIncome),
          IncomeSource("13", None, taxableIncome),
          IncomeSource("14", None, taxableIncome),
          IncomeSource("15", None, taxableIncome),
          IncomeSource("16", None, taxableIncome),
          IncomeSource("17", None, taxableIncome),
          IncomeSource("18", None, taxableIncome),
          IncomeSource("19", None, taxableIncome),
          IncomeSource("20", None, taxableIncome),
          IncomeSource("21", None, taxableIncome),
          IncomeSource("22", None, taxableIncome),
          IncomeSource("98", None, taxableIncome)
        )),
        totalEstimatedIncome = Some(12500),
        totalTaxableIncome = Some(12500),
        totalAllowancesAndDeductions = Some(4200.00),
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
      )),
      taxDeductedAtSource = Some(TaxDeductedAtSource(
        ukLandAndProperty = Some(5000.99),
        bbsi = Some(5000.99),
        cis = Some(5000.99),
        voidedIsa = Some(5000.99),
        payeEmployments = Some(5000.99),
        occupationalPensions = Some(5000.99),
        stateBenefits = Some(-99999999999.99),
        specialWithholdingTaxOrUkTaxPaid = Some(5000.99),
        inYearAdjustmentCodedInLaterTaxYear = Some(5000.99)
      )),
      transitionProfit = Some(TransitionProfit(totalTaxableTransitionProfit = Some(3000.00))))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
    ),
    submissionChannel = None
  )

  val liabilityCalculationModelSuccessfulExpected: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1")))
    )),
    calculation = Some(Calculation(
      allowancesAndDeductions = Some(AllowancesAndDeductions(
        personalAllowance = Some(12500),
        reducedPersonalAllowance = Some(12500),
        marriageAllowanceTransferOut = Some(MarriageAllowanceTransferOut(
          personalAllowanceBeforeTransferOut = 5000.99,
          transferredOutAmount = 5000.99)),
        pensionContributions = Some(5000.99),
        lossesAppliedToGeneralIncome = Some(12500),
        giftOfInvestmentsAndPropertyToCharity = Some(12500),
        grossAnnuityPayments = Some(5000.99),
        qualifyingLoanInterestFromInvestments = Some(5000.99),
        postCessationTradeReceipts = Some(5000.99),
        paymentsToTradeUnionsForDeathBenefits = Some(5000.99))),
      chargeableEventGainsIncome = Some(ChargeableEventGainsIncome(
        totalOfAllGains = 12500
      )),
      dividendsIncome = Some(DividendsIncome(chargeableForeignDividends = Some(12500))),
      employmentAndPensionsIncome = Some(EmploymentAndPensionsIncome(
        totalPayeEmploymentAndLumpSumIncome = Some(5000.99),
        totalBenefitsInKind = Some(5000.99),
        totalOccupationalPensionIncome = Some(5000.99)
      )),
      employmentExpenses = Some(EmploymentExpenses(totalEmploymentExpenses = Some(5000.99))),
      foreignIncome = Some(ForeignIncome(
        chargeableOverseasPensionsStateBenefitsRoyalties = Some(5000.99),
        chargeableAllOtherIncomeReceivedWhilstAbroad = Some(5000.99),
        overseasIncomeAndGains = Some(OverseasIncomeAndGains(gainAmount = 5000.99)),
        totalForeignBenefitsAndGifts = Some(5000.99)
      )),
      giftAid = Some(GiftAid(
        grossGiftAidPayments = 12500,
        giftAidTax = 5000.99
      )),
      incomeSummaryTotals = Some(IncomeSummaryTotals(
        totalSelfEmploymentProfit = Some(12500),
        totalPropertyProfit = Some(12500),
        totalFHLPropertyProfit = Some(12500),
        totalForeignPropertyProfit = Some(12500),
        totalEeaFhlProfit = Some(12500)
      )),
      marriageAllowanceTransferredIn = Some(MarriageAllowanceTransferredIn(amount = Some(5000.99))),
      studentLoans = Some(Seq(
        StudentLoan(
          planType = Option("01"),
          studentLoanRepaymentAmount = Some(BigDecimal(5000.99))
        ),
        StudentLoan(
          planType = Option("03"),
          studentLoanRepaymentAmount = Some(BigDecimal(5000.99)
          )
        ))),
      reliefs = Some(Reliefs(reliefsClaimed = Some(Seq(ReliefsClaimed(
        `type` = "vctSubscriptions",
        amountUsed = Some(5000.99)),
        ReliefsClaimed(
          `type` = "vctSubscriptions2",
          amountUsed = Some(5000.99))
      )),
        residentialFinanceCosts = Some(ResidentialFinanceCosts(totalResidentialFinanceCostsRelief = 5000.99)),
        foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(totalForeignTaxCreditRelief = 5000.99)),
        topSlicingRelief = Some(TopSlicingRelief(amount = Some(5000.99))),
        giftAidTaxReductionWhereBasicRateDiffers = Some(GiftAidTaxReductionWhereBasicRateDiffers(amount = Some(127.49))))
      ),
      savingsAndGainsIncome = Some(SavingsAndGainsIncome(
        chargeableForeignSavingsAndGains = Some(12500)
      )),
      shareSchemesIncome = Some(ShareSchemesIncome(
        totalIncome = 5000.99
      )),
      otherIncome = Some(OtherIncome(totalOtherIncome = 500.00)),
      stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99),
        totalStateBenefitsIncomeExcStatePensionLumpSum = Some(5000.99))),
      taxCalculation = Some(TaxCalculation(
        incomeTax = IncomeTax(
          totalIncomeReceivedFromAllSources = 12500,
          totalAllowancesAndDeductions = 12500,
          totalTaxableIncome = 12500,
          payPensionsProfit = Some(PayPensionsProfit(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          savingsAndGains = Some(SavingsAndGains(
            taxableIncome = 12500,
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          dividends = Some(Dividends(
            taxableIncome = 12500,
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          lumpSums = Some(LumpSums(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          gainsOnLifePolicies = Some(GainsOnLifePolicies(
            taxBands = Some(Seq(TaxBands(
              name = "SSR",
              rate = 20,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 5000.99
            )))
          )),
          totalReliefs = Some(5000.99),
          totalNotionalTax = Some(5000.99),
          incomeTaxDueAfterTaxReductions = Some(5000.99),
          totalPensionSavingsTaxCharges = Some(5000.99),
          statePensionLumpSumCharges = Some(5000.99),
          payeUnderpaymentsCodedOut = Some(5000.99),
          incomeTaxChargedOnTransitionProfits = Some(700.00)
        ),
        nics = Some(Nics(
          class4Nics = Some(Class4Nics(nic4Bands = Seq(Nic4Bands(
            name = "ZRT",
            income = 12500,
            rate = 20,
            amount = 5000.99
          )))),
          class2Nics = Some(Class2Nics(amount = Some(5000.99)))
        )),
        capitalGainsTax = Some(CapitalGainsTax(
          totalTaxableGains = 5000.99,
          adjustments = Some(-99999999999.99),
          foreignTaxCreditRelief = Some(5000.99),
          taxOnGainsAlreadyPaid = Some(5000.99),
          capitalGainsTaxDue = 5000.99,
          capitalGainsOverpaid = Some(5000.99),
          residentialPropertyAndCarriedInterest = Some(ResidentialPropertyAndCarriedInterest(
            cgtTaxBands = Some(Seq(CgtTaxBands(
              name = "lowerRate",
              rate = 20,
              income = 5000.99,
              taxAmount = 5000.99
            ),
              CgtTaxBands(
                name = "lowerRate2",
                rate = 21,
                income = 5000.99,
                taxAmount = 5000.99
              )))
          )),
          otherGains = Some(OtherGains(
            cgtTaxBands = Some(Seq(CgtTaxBands(
              name = "lowerRate",
              rate = 20,
              income = 5000.99,
              taxAmount = 5000.99
            ),
              CgtTaxBands(
                name = "lowerRate2",
                rate = 21,
                income = 5000.99,
                taxAmount = 5000.99
              )))
          )),
          businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(
            taxableGains = Some(5000.99),
            rate = Some(20),
            taxAmount = Some(5000.99)
          ))
        )),
        totalStudentLoansRepaymentAmount = Some(5000.99),
        saUnderpaymentsCodedOut = Some(-99999999999.99),
        totalIncomeTaxAndNicsDue = 90500.99,
        totalTaxDeducted = Some(-20500.99)
      )),
      endOfYearEstimate = Some(EndOfYearEstimate(
        incomeSource = Some(List(
          IncomeSource(
            incomeSourceType = "01",
            incomeSourceName = Some("source1"),
            taxableIncome = 12500
          ),
          IncomeSource(
            incomeSourceType = "02",
            incomeSourceName = Some("source2"),
            taxableIncome = 12500
          ))),
        totalEstimatedIncome = Some(12500),
        totalTaxableIncome = Some(12500),
        totalAllowancesAndDeductions = Some(4200.00),
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
      )),
      taxDeductedAtSource = Some(TaxDeductedAtSource(
        ukLandAndProperty = Some(5000.99),
        bbsi = Some(5000.99),
        cis = Some(5000.99),
        voidedIsa = Some(5000.99),
        payeEmployments = Some(5000.99),
        occupationalPensions = Some(5000.99),
        stateBenefits = Some(-99999999999.99),
        specialWithholdingTaxOrUkTaxPaid = Some(5000.99),
        inYearAdjustmentCodedInLaterTaxYear = Some(5000.99)
      )),
      transitionProfit = Some(TransitionProfit(totalTaxableTransitionProfit = Some(3000.00))))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
    ),
    submissionChannel = None
  )

  val liabilityCalculationModelSuccessfulNotCrystallised: LiabilityCalculationResponse = liabilityCalculationModelSuccessful.copy(
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "inYear",
    ),
    submissionChannel = None
  )

  val liabilityCalculationModelSuccessfulWithAmendment: LiabilityCalculationResponse = liabilityCalculationModelSuccessful.copy(
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "CA",
    ),
    submissionChannel = None
  )

  val liabilityCalculationModelErrorMessages = liabilityCalculationModelDeductionsMinimal.copy(messages = Some(Messages(
    errors = Some(List(
      Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
      Message("C15507", "you’ve claimed £2000 in Property Income Allowance but this is more than turnover for your UK property."),
      Message("C15510", "the Rent-a-Room relief claimed for a jointly let property cannot be more than 10% of the Rent-a-Room limit."),
      Message("C55010", "updates cannot include overlaps.")
    ))
  )))

  val liabilityCalculationModelErrorMessagesFormatted: MTDUserRole => LiabilityCalculationResponse = mtdUserRole =>
    liabilityCalculationModelDeductionsMinimal.copy(messages = Some(Messages(
      errors = Some(List(
        Message("C55012", "the update must align to the accounting period end date of 5 January 2023."),
        Message("C15507", s"${if (mtdUserRole == MTDIndividual) "you’ve" else "your client"} claimed £2000 in Property Income Allowance but this is more than turnover for ${if (mtdUserRole == MTDIndividual) "your" else "their"} UK property."),
        Message("C15510", "the Rent-a-Room relief claimed for a jointly let property cannot be more than 10% of the Rent-a-Room limit."),
        Message("C55010", "updates cannot include overlaps.")
      ))
    )))
}
