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

package testConstants


import models.liabilitycalculation._
import models.liabilitycalculation.taxcalculation._
import models.liabilitycalculation.viewmodels.{CapitalGainsTaxViewModel, TaxDeductedAtSourceViewModel, TaxDueSummaryViewModel}

import java.time.LocalDate

object NewCalcBreakdownUnitTestConstants {

  val liabilityCalculationModelError: LiabilityCalculationError = LiabilityCalculationError(432, "someerrorhere")

  def liabilityCalculationModelDeductionsMinimal(calculationReason: String): LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    calculation = Some(Calculation(
      allowancesAndDeductions = Some(AllowancesAndDeductions()))),
    metadata = Metadata(
      calculationTimestamp = None,
      calculationType = "IY",
      calculationReason = calculationReason,
      periodFrom = LocalDate.of(2022, 1, 1),
      periodTo = LocalDate.of(2023, 1, 1))
  )

  val liabilityCalculationModelSuccessful: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "C22211", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "C22214", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "C22216", text = "error msg text1")))
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
        totalForeignPropertyProfit = Some(12500)
      )),
      marriageAllowanceTransferredIn = Some(MarriageAllowanceTransferredIn(amount = Some(5000.99))),
      studentLoans = Some(Seq(StudentLoan(
        planType = "01",
        studentLoanTotalIncomeAmount = BigDecimal(5000.99),
        studentLoanChargeableIncomeAmount = BigDecimal(5000.99),
        studentLoanRepaymentAmount = BigDecimal(5000.99),
        studentLoanRepaymentAmountNetOfDeductions = BigDecimal(5000.99),
        studentLoanApportionedIncomeThreshold = 1500,
        studentLoanRate = BigDecimal(20)
      ))),
      reliefs = Some(Reliefs(reliefsClaimed = Some(Seq(ReliefsClaimed(
        `type` = "vctSubscriptions",
        amountUsed = Some(5000.99)),
        ReliefsClaimed(
          `type` = "deficiencyRelief",
          amountUsed = Some(5000.99))
      )),
        residentialFinanceCosts = Some(ResidentialFinanceCosts(totalResidentialFinanceCostsRelief = 5000.99)),
        foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(totalForeignTaxCreditRelief = 5000.99)),
        topSlicingRelief = Some(TopSlicingRelief(amount = Some(5000.99))),
        giftAidTaxReductionWhereBasicRateDiffers = Some(GiftAidTaxReductionWhereBasicRateDiffers(amount = Some(127.49))))),
      savingsAndGainsIncome = Some(SavingsAndGainsIncome(
        chargeableForeignSavingsAndGains = Some(12500)
      )),
      shareSchemesIncome = Some(ShareSchemesIncome(
        totalIncome = 5000.99
      )),
      otherIncome = Some(OtherIncome(
        totalOtherIncome = 500.00
      )),
      stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99),
        totalStateBenefitsIncomeExcStatePensionLumpSum = Some(9000.99))),
      taxCalculation = Some(TaxCalculation(
        incomeTax = IncomeTax(
          totalIncomeReceivedFromAllSources = 12500,
          totalAllowancesAndDeductions = 12500,
          totalTaxableIncome = 12500,
          payPensionsProfit = Some(PayPensionsProfit(
            taxBands = Some(Seq(TaxBands(
              name = "BRT",
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
              name = "ZRT",
              rate = 0,
              bandLimit = 12500,
              apportionedBandLimit = 12500,
              income = 12500,
              taxAmount = 0
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
          giftAidTaxChargeWhereBasicRateDiffers = Some(6565.99),
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
          adjustments = Some(-2500.99),
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
        saUnderpaymentsCodedOut = Some(-2500.99),
        totalIncomeTaxAndNicsDue = 5000.99,
        totalTaxDeducted = Some(50000.99)
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
        totalTaxableIncome = Some(8300),
        totalAllowancesAndDeductions = Some(4200),
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
        stateBenefits = Some(5000.99),
        specialWithholdingTaxOrUkTaxPaid = Some(5000.99),
        inYearAdjustmentCodedInLaterTaxYear = Some(5000.99),
        taxTakenOffTradingIncome = Some(563.12)
      )),
      pensionSavingsTaxCharges = Some(PensionSavingsTaxCharges(
        totalPensionCharges = Some(5000.99),
        totalTaxPaid = Some(5000.99),
        totalPensionChargesDue = Some(5000.99)
      )),
      transitionProfit = Some(TransitionProfit(totalTaxableTransitionProfit = Some(3000))))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "IY",
      calculationReason = "customerRequest",
      periodFrom = LocalDate.of(2018, 1, 1),
      periodTo = LocalDate.of(2019, 1, 1)
    )
  )

  val liabilityCalculationModelErrorMessagesForIndividual = liabilityCalculationModelDeductionsMinimal(calculationReason = "customerRequest").copy(messages = Some(Messages(
    errors = Some(List(
      Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
      Message("C15507", "you’ve claimed £2000 in Property Income Allowance but this is more than turnover for your UK property."),
      Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
      Message("C55009", "updates cannot include gaps.")
    ))
  )))

  val liabilityCalculationModelErrorMessagesForAgent = liabilityCalculationModelDeductionsMinimal(calculationReason = "customerRequest").copy(messages = Some(Messages(
    errors = Some(List(
      Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
      Message("C15507", "your client claimed £2000 in Property Income Allowance but this is more than turnover for their UK property."),
      Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
      Message("C55009", "updates cannot include gaps.")
    ))
  )))

  val liabilityCalculationModelSuccessfulNotCrystallised = liabilityCalculationModelSuccessful.copy(metadata =
    Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "IY",
      calculationReason = "customerRequest",
      periodFrom = LocalDate.of(2018, 1, 1),
      periodTo = LocalDate.of(2019, 1, 1)
    )

  )

  val arrayTestFull = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = None,
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
        totalForeignPropertyProfit = Some(12500)
      )),
      marriageAllowanceTransferredIn = Some(MarriageAllowanceTransferredIn(amount = Some(5000.99))),
      reliefs = Some(Reliefs(reliefsClaimed = None,
        residentialFinanceCosts = Some(ResidentialFinanceCosts(totalResidentialFinanceCostsRelief = 5000.99)),
        foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(totalForeignTaxCreditRelief = 5000.99)),
        topSlicingRelief = Some(TopSlicingRelief(amount = Some(5000.99))),
        giftAidTaxReductionWhereBasicRateDiffers = None)),
      savingsAndGainsIncome = Some(SavingsAndGainsIncome(
        chargeableForeignSavingsAndGains = Some(12500)
      )),
      shareSchemesIncome = Some(ShareSchemesIncome(
        totalIncome = 5000.99
      )),
      otherIncome = Some(OtherIncome(
        totalOtherIncome = 500.00
      )),
      stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99),
        totalStateBenefitsIncomeExcStatePensionLumpSum = Some(5000.99))),
      taxCalculation = Some(TaxCalculation(
        incomeTax = IncomeTax(
          totalIncomeReceivedFromAllSources = 12500,
          totalAllowancesAndDeductions = 12500,
          totalTaxableIncome = 12500,
          payPensionsProfit = Some(PayPensionsProfit(
            taxBands = None
          )),
          savingsAndGains = Some(SavingsAndGains(
            taxableIncome = 12500,
            taxBands = None
          )),
          dividends = Some(Dividends(
            taxableIncome = 12500,
            taxBands = None
          )),
          lumpSums = Some(LumpSums(
            taxBands = None
          )),
          gainsOnLifePolicies = Some(GainsOnLifePolicies(
            taxBands = None
          )),
          totalReliefs = Some(5000.99),
          totalNotionalTax = Some(5000.99),
          incomeTaxDueAfterTaxReductions = Some(5000.99),
          totalPensionSavingsTaxCharges = Some(5000.99),
          statePensionLumpSumCharges = Some(5000.99),
          payeUnderpaymentsCodedOut = Some(5000.99),
          giftAidTaxChargeWhereBasicRateDiffers = None,
          incomeTaxChargedOnTransitionProfits = Some(5000.99)
        ),
        nics = Some(Nics(
          class4Nics = Some(Class4Nics(nic4Bands = Seq())),
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
            cgtTaxBands = None
          )),
          otherGains = Some(OtherGains(
            cgtTaxBands = None
          )),
          businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(
            taxableGains = Some(5000.99),
            rate = Some(20),
            taxAmount = Some(5000.99)
          ))
        )),
        totalStudentLoansRepaymentAmount = Some(5000.99),
        saUnderpaymentsCodedOut = Some(-99999999999.99),
        totalIncomeTaxAndNicsDue = -99999999999.99,
        totalTaxDeducted = Some(-99999999999.99)
      )),
      endOfYearEstimate = Some(EndOfYearEstimate(
        incomeSource = None,
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
      transitionProfit = Some(TransitionProfit(totalTaxableTransitionProfit = Some(12500))))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "IY",
      calculationReason = "customerRequest",
      periodFrom = LocalDate.of(2018, 1, 1),
      periodTo = LocalDate.of(2019, 1, 1))
  )

  val taxDueSummaryViewModelStandard = TaxDueSummaryViewModel(
    taxRegime = "Uk",
    payPensionsProfitBands = Some(
      Seq(
        TaxBands(
          name = "BRT",
          rate = 20.0,
          bandLimit = 12500,
          apportionedBandLimit = 12500,
          income = 20000,
          taxAmount = 4000.00
        ),
        TaxBands(
          name = "HRT",
          rate = 40.0,
          bandLimit = 12500,
          apportionedBandLimit = 12500,
          income = 100000,
          taxAmount = 40000.00
        ),
        TaxBands(
          name = "ART",
          rate = 45.0,
          bandLimit = 12500,
          apportionedBandLimit = 12500,
          income = 50000,
          taxAmount = 22500.00
        )
      )
    ),
    dividendsBands = Some(Seq(
      TaxBands(
        name = "BRT",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTBR",
        rate = 0,
        income = 1000,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 37.5,
        income = 2000,
        taxAmount = 750.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTHR",
        rate = 0,
        income = 2000,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 38.1,
        income = 3000,
        taxAmount = 1143.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTAR",
        rate = 0,
        income = 3000,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    savingsAndGainsBands = Some(Seq(
      TaxBands(
        name = "SSR",
        rate = 0.0,
        income = 1,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "BRT",
        rate = 10.0,
        income = 20,
        taxAmount = 2.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTBR",
        rate = 0.0,
        income = 20,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 2000,
        taxAmount = 800.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTHR",
        rate = 0.0,
        income = 10000,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 50.0,
        income = 100000,
        taxAmount = 5000.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    lumpSumsBands = Some(Seq(
      TaxBands(
        name = "BRT",
        rate = 20.0,
        income = 20000,
        taxAmount = 4000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 100000,
        taxAmount = 40000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 45.0,
        income = 50000,
        taxAmount = 22500.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    gainsOnLifePoliciesBands = Some(Seq(
      TaxBands(
        name = "BRT",
        rate = 20.0,
        income = 20000,
        taxAmount = 4000.00,
        bandLimit = 14000,
        apportionedBandLimit = 14000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 100000,
        taxAmount = 40000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 45.0,
        income = 50000,
        taxAmount = 22500.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    nic4Bands = Some(Seq(
      Nic4Bands(
        name = "ZRT",
        rate = 1,
        income = 2000,
        amount = 100
      ),
      Nic4Bands(
        name = "BRT",
        rate = 2,
        income = 3000,
        amount = 200
      ),
      Nic4Bands(
        name = "HRT",
        rate = 3,
        income = 5000,
        amount = 300
      )
    )),
    class2VoluntaryContributions = true,
    class2NicsAmount = Some(10000),
    giftAidTax = Some(5000),
    totalPensionSavingsTaxCharges = Some(5000),
    statePensionLumpSumCharges = Some(5000),
    totalStudentLoansRepaymentAmount = Some(5000),
    marriageAllowanceTransferredInAmount = Some(252),
    topSlicingReliefAmount = Some(1200),
    totalResidentialFinanceCostsRelief = Some(5000),
    totalForeignTaxCreditRelief = Some(6000),
    totalNotionalTax = Some(7000),
    incomeTaxDueAfterTaxReductions = Some(2000),
    payeUnderpaymentsCodedOut = Some(254),
    saUnderpaymentsCodedOut = Some(400),
    totalAnnuityPaymentsTaxCharged = Some(367.21),
    totalRoyaltyPaymentsTaxCharged = Some(240.35),
    reliefsClaimed = Some(Seq(ReliefsClaimed("deficiencyRelief", Some(1000)), ReliefsClaimed("vctSubscriptions", Some(2000)),
      ReliefsClaimed("eisSubscriptions", Some(3000)), ReliefsClaimed("seedEnterpriseInvestment", Some(4000)),
      ReliefsClaimed("communityInvestment", Some(5000)), ReliefsClaimed("socialEnterpriseInvestment", Some(6000)),
      ReliefsClaimed("maintenancePayments", Some(7000)),
      ReliefsClaimed("qualifyingDistributionRedemptionOfSharesAndSecurities", Some(8000)),
      ReliefsClaimed("nonDeductableLoanInterest", Some(9000))
    )),
    capitalGainsTax = CapitalGainsTaxViewModel(
      businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(
        taxableGains = Some(10000.0),
        rate = Some(10.0),
        taxAmount = Some(1000.0)
      )),
      propertyAndInterestTaxBands = Some(Seq(
        CgtTaxBands(
          name = "higherRate",
          rate = 28.0,
          income = 30000.0,
          taxAmount = 8400.0
        ),
        CgtTaxBands(
          name = "lowerRate",
          rate = 18.0,
          income = 20000.0,
          taxAmount = 3600.0
        )
      )),
      otherGainsTaxBands = Some(Seq(
        CgtTaxBands(
          name = "higherRate",
          rate = 28.0,
          income = 12000.0,
          taxAmount = 3360.0
        ),
        CgtTaxBands(
          name = "lowerRate",
          rate = 20.0,
          income = 11000.0,
          taxAmount = 2200.0
        )
      )),
      totalTaxableGains = Some(1234.56),
      adjustments = Some(123.45),
      foreignTaxCreditRelief = Some(2345.67),
      taxOnGainsAlreadyPaid = Some(3456.78),
      capitalGainsTaxDue = Some(4567.89),
      capitalGainsOverpaid = Some(234.56)
    ),
    taxDeductedAtSource = TaxDeductedAtSourceViewModel(
      payeEmployments = Some(100.0),
      ukPensions = Some(200.0),
      stateBenefits = Some(300.0),
      cis = Some(400.0),
      ukLandAndProperty = Some(500.0),
      specialWithholdingTax = Some(600.0),
      voidISAs = Some(700.0),
      savings = Some(800.0),
      inYearAdjustmentCodedInLaterTaxYear = Some(900.0),
      taxTakenOffTradingIncome = Some(563.12)
    ),
    totalTaxDeducted = Some(1000)
  )

  val taxDueSummaryViewModelZeroIncome = TaxDueSummaryViewModel(
    payPensionsProfitBands = Some(Seq(TaxBands(
      name = "BRT",
      rate = 20.0,
      income = 0,
      taxAmount = 4000.00,
      bandLimit = 15000,
      apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 0,
        taxAmount = 40000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 45.0,
        income = 0,
        taxAmount = 22500.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    lumpSumsBands = Some(Seq(TaxBands(
      name = "BRT",
      rate = 20.0,
      income = 0,
      taxAmount = 4000.00,
      bandLimit = 15000,
      apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 0,
        taxAmount = 40000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 45.0,
        income = 0,
        taxAmount = 22500.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )
    ),
    gainsOnLifePoliciesBands = Some(Seq(TaxBands(
      name = "BRT",
      rate = 20.0,
      income = 0,
      taxAmount = 4000.00,
      bandLimit = 14000,
      apportionedBandLimit = 14000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 0,
        taxAmount = 40000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 45.0,
        income = 0,
        taxAmount = 22500.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    )),
    savingsAndGainsBands = Some(Seq(
      TaxBands(
        name = "SSR",
        rate = 0.0,
        income = 0,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "BRT",
        rate = 10.0,
        income = 0,
        taxAmount = 2.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTBR",
        rate = 0.0,
        income = 0,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        income = 0,
        taxAmount = 800.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ZRTHR",
        rate = 0.0,
        income = 0,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
      TaxBands(
        name = "ART",
        rate = 50.0,
        income = 0,
        taxAmount = 5000.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000)
    ))
  )

  val taxDueSummaryViewModelMarriageAllowance = TaxDueSummaryViewModel(
    marriageAllowanceTransferredInAmount = Some(1234)
  )

  val taxDueSummaryViewModelTopSlicingRelief = TaxDueSummaryViewModel(
    topSlicingReliefAmount = Some(2345)
  )
  val taxDueSummaryViewModelNic2 = TaxDueSummaryViewModel(
    class2NicsAmount = Some(10000)
  )
  val taxDueSummaryViewModelVoluntaryNic2 = TaxDueSummaryViewModel(
    class2VoluntaryContributions = true,
    class2NicsAmount = Some(10000)
  )

  val taxDueSummaryViewModelGiftAid = TaxDueSummaryViewModel(
    totalIncomeTaxAndNicsDue = Some(543.21),
    totalTaxableIncome = Some(0),
    taxRegime = "UK",
    giftAidTax = Some(5000)
  )

  val taxDueSummaryViewModelPensionLumpSum = TaxDueSummaryViewModel(
    statePensionLumpSumCharges = Some(5000)
  )

  val taxDueSummaryViewModelPensionSavings = TaxDueSummaryViewModel(
    totalPensionSavingsTaxCharges = Some(5000)
  )

  val taxDueSummaryViewModelScottishBands = TaxDueSummaryViewModel(
    taxRegime = "Scotland",
    payPensionsProfitBands = Some(Seq(
      TaxBands(
        name = "SRT",
        rate = 10.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 2000.00
      ),
      TaxBands(
        name = "BRT",
        rate = 20.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 4000.00
      ),
      TaxBands(
        name = "IRT",
        rate = 25.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 45000.00
      ),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 100000,
        taxAmount = 40000.00
      ),
      TaxBands(
        name = "AVRT",
        rate = 45.0,
        bandLimit = 125140,
        apportionedBandLimit = 125140,
        income = 100000,
        taxAmount = 11250.00
      ),
      TaxBands(
        name = "ART_scottish",
        rate = 45.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 500000,
        taxAmount = 22500.00
      )
    ))
  )

}
