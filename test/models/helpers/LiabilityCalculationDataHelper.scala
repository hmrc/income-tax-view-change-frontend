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

package models.helpers

import enums.CesaSAReturn
import models.liabilitycalculation._
import models.liabilitycalculation.taxcalculation._

trait LiabilityCalculationDataHelper {

  val pensionsProfit = PayPensionsProfit(
    taxBands = Some(Seq(TaxBands(
      name = "BRT",
      rate = 20,
      bandLimit = 12500,
      apportionedBandLimit = 12500,
      income = 12500,
      taxAmount = 5000.99
    )))
  )
  val taxCalculationResult = TaxCalculation(
    incomeTax = IncomeTax(
      totalIncomeReceivedFromAllSources = 12500,
      totalAllowancesAndDeductions = 12500,
      totalTaxableIncome = 12500,
      payPensionsProfit = Some(pensionsProfit),
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
      giftAidTaxChargeWhereBasicRateDiffers = Some(6565.99)
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
  )
  val taxCalculationResultSB = TaxCalculation(
    incomeTax = IncomeTax(
      totalIncomeReceivedFromAllSources = 12500,
      totalAllowancesAndDeductions = 12500,
      totalTaxableIncome = 12500,
      payPensionsProfit = None,
      savingsAndGains = Some(SavingsAndGains(
        taxableIncome = 12500,
        taxBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 20,
          bandLimit = 12510,
          apportionedBandLimit = 12520,
          income = 12530,
          taxAmount = 5001.99
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
      payeUnderpaymentsCodedOut = Some(5000.99)
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
  )
  val taxCalculationResultDB = TaxCalculation(
    incomeTax = IncomeTax(
      totalIncomeReceivedFromAllSources = 12500,
      totalAllowancesAndDeductions = 12500,
      totalTaxableIncome = 12500,
      payPensionsProfit = None,
      savingsAndGains = None,
      dividends = Some(Dividends(
        taxableIncome = 12600,
        taxBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 21,
          bandLimit = 12700,
          apportionedBandLimit = 12800,
          income = 12900,
          taxAmount = 5123.99
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
      payeUnderpaymentsCodedOut = Some(5000.99)
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
  )
  val taxCalculationResultLS = TaxCalculation(
    incomeTax = IncomeTax(
      totalIncomeReceivedFromAllSources = 12500,
      totalAllowancesAndDeductions = 12500,
      totalTaxableIncome = 12500,
      payPensionsProfit = None,
      savingsAndGains = None,
      dividends = None,
      lumpSums = Some(LumpSums(
        taxBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 30,
          bandLimit = 13500,
          apportionedBandLimit = 15500,
          income = 16500,
          taxAmount = 7000.99
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
      payeUnderpaymentsCodedOut = Some(5000.99)
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
  )
  val taxCalculationResultGLP = TaxCalculation(
    incomeTax = IncomeTax(
      totalIncomeReceivedFromAllSources = 12500,
      totalAllowancesAndDeductions = 12500,
      totalTaxableIncome = 12500,
      payPensionsProfit = None,
      savingsAndGains = None,
      dividends = None,
      lumpSums = None,
      gainsOnLifePolicies = Some(GainsOnLifePolicies(
        taxBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 50,
          bandLimit = 32500,
          apportionedBandLimit = 42500,
          income = 52500,
          taxAmount = 7000.99
        )))
      )),
      totalReliefs = Some(5000.99),
      totalNotionalTax = Some(5000.99),
      incomeTaxDueAfterTaxReductions = Some(5000.99),
      totalPensionSavingsTaxCharges = Some(5000.99),
      statePensionLumpSumCharges = Some(5000.99),
      payeUnderpaymentsCodedOut = Some(5000.99)
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
  )
  val calculationResult = Calculation(
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
      giftAidTaxReductionWhereBasicRateDiffers = Some(GiftAidTaxReductionWhereBasicRateDiffers(amount = Some(127.49))))),
    savingsAndGainsIncome = Some(SavingsAndGainsIncome(
      chargeableForeignSavingsAndGains = Some(12500)
    )),
    shareSchemesIncome = Some(ShareSchemesIncome(
      totalIncome = 5000.99
    )),
    stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99),
      totalStateBenefitsIncomeExcStatePensionLumpSum = Some(5000.99))),
    taxCalculation = Some(taxCalculationResult),
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
    )))

  val liabilityCalculationModelSuccessfulConversionPB = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "errorId1", text = "error msg text1")))
    )),
    calculation = Some(calculationResult),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      calculationTrigger = Some(CesaSAReturn)
    )
  )

  val liabilityCalculationModelSuccessfulConversionSB = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "errorId1", text = "error msg text1")))
    )),
    calculation = Some(calculationResult.copy(taxCalculation = Some(taxCalculationResultSB))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      calculationTrigger = Some(CesaSAReturn)
    )
  )
  val liabilityCalculationModelSuccessfulConversionDB = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "errorId1", text = "error msg text1")))
    )),
    calculation = Some(calculationResult.copy(taxCalculation = Some(taxCalculationResultDB))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      calculationTrigger = Some(CesaSAReturn)
    )
  )
  val liabilityCalculationModelSuccessfulConversionLS = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "errorId1", text = "error msg text1")))
    )),
    calculation = Some(calculationResult.copy(taxCalculation = Some(taxCalculationResultLS))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      calculationTrigger = Some(CesaSAReturn)
    )
  )
  val liabilityCalculationModelSuccessfulConversionGLP = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = Some(true)
    )),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "infoId1", text = "info msg text1"))),
      warnings = Some(Seq(Message(id = "warnId1", text = "warn msg text1"))),
      errors = Some(Seq(Message(id = "errorId1", text = "error msg text1")))
    )),
    calculation = Some(calculationResult.copy(taxCalculation = Some(taxCalculationResultGLP))),
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      calculationTrigger = Some(CesaSAReturn)
    )
  )

  val liabilityCalculationModelSuccessfulWithNoCalc = liabilityCalculationModelSuccessfulConversionPB.copy(
    calculation = None
  )

  val errorMessagesIndividual = Some(List(
    Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
    Message("C15507", "you’ve claimed £2000 in Property Income Allowance but this is more than turnover for your UK property."),
    Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
    Message("C55009", "updates cannot include gaps.")
  ))

  val errorMessagesAgent = Some(List(
    Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
    Message("C15507", "your client claimed £2000 in Property Income Allowance but this is more than turnover for their UK property."),
    Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
    Message("C55009", "updates cannot include gaps.")
  ))

  val InfoMessagesScottishTaxRegime = Some(List(
    Message("C22225", "Your tax has been reduced because of Gift Aid charity donations - the Scottish Basic Rate of Income Tax is higher than the rate at which charities have obtained relief."),
    Message("C22226", "Your tax has increased because of Gift Aid charity donations - the Scottish Basic Rate of Income Tax is lower than the rate at which charities have obtained relief."),
  ))

  val InfoMessagesWelshTaxRegime = Some(List(
    Message("C22225", "Your tax has been reduced because of Gift Aid charity donations - the Welsh Basic Rate of Income Tax is higher than the rate at which charities have obtained relief."),
    Message("C22226", "Your tax has increased because of Gift Aid charity donations - the Welsh Basic Rate of Income Tax is lower than the rate at which charities have obtained relief."),
  ))

}


