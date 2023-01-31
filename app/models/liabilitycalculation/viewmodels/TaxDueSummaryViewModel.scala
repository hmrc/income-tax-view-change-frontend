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

import models.liabilitycalculation.taxcalculation.{CapitalGainsTax, Nic4Bands, TaxBands}
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel.getTaxDue
import models.liabilitycalculation.{LiabilityCalculationResponse, Messages, ReliefsClaimed, StudentLoan}

case class TaxDueSummaryViewModel(
                                   taxRegime: String = "",
                                   class2VoluntaryContributions: Boolean = false,
                                   messages: Option[Messages] = None,
                                   lossesAppliedToGeneralIncome: Option[Int] = None,
                                   grossGiftAidPayments: Option[BigDecimal] = None,
                                   giftAidTax: Option[BigDecimal] = None,
                                   marriageAllowanceTransferredInAmount: Option[BigDecimal] = None,
                                   studentLoans: Option[Seq[StudentLoan]] = None,
                                   reliefsClaimed: Option[Seq[ReliefsClaimed]] = None,
                                   totalResidentialFinanceCostsRelief: Option[BigDecimal] = None,
                                   totalForeignTaxCreditRelief: Option[BigDecimal] = None,
                                   topSlicingReliefAmount: Option[BigDecimal] = None,
                                   totalTaxableIncome: Option[BigDecimal] = None,
                                   payPensionsProfitBands: Option[Seq[TaxBands]] = None,
                                   savingsAndGainsBands: Option[Seq[TaxBands]] = None,
                                   lumpSumsBands: Option[Seq[TaxBands]] = None,
                                   dividendsBands: Option[Seq[TaxBands]] = None,
                                   gainsOnLifePoliciesBands: Option[Seq[TaxBands]] = None,
                                   totalNotionalTax: Option[BigDecimal] = None,
                                   incomeTaxDueAfterTaxReductions: Option[BigDecimal] = None,
                                   totalPensionSavingsTaxCharges: Option[BigDecimal] = None,
                                   statePensionLumpSumCharges: Option[BigDecimal] = None,
                                   payeUnderpaymentsCodedOut: Option[BigDecimal] = None,
                                   nic4Bands: Option[Seq[Nic4Bands]] = None,
                                   class2NicsAmount: Option[BigDecimal] = None,
                                   capitalGainsTax: CapitalGainsTaxViewModel = CapitalGainsTaxViewModel(),
                                   totalStudentLoansRepaymentAmount: Option[BigDecimal] = None,
                                   saUnderpaymentsCodedOut: Option[BigDecimal] = None,
                                   totalIncomeTaxAndNicsDue: Option[BigDecimal] = None,
                                   totalTaxDeducted: Option[BigDecimal] = None,
                                   taxDeductedAtSource: TaxDeductedAtSourceViewModel = TaxDeductedAtSourceViewModel(),
                                   totalAnnuityPaymentsTaxCharged: Option[Int] = None,
                                   totalRoyaltyPaymentsTaxCharged: Option[BigDecimal] = None
                                 ) {

  def getRateHeaderKey: String = {
    taxRegime.contains("Scotland") match {
      case true => ".scotland"
      case false => ".uk"
    }
  }

  def getModifiedBaseTaxBand: Option[TaxBands] = {
    val payPensionsProfitTaxBand = payPensionsProfitBands.flatMap(bands => bands.find(_.name.equals("BRT")))
    val savingsTaxBand = savingsAndGainsBands.flatMap(bands => bands.find(_.name.equals("BRT")))
    val dividendsTaxBand = dividendsBands.flatMap(bands => bands.find(_.name.equals("BRT")))
    val lumpSumsTaxBand = lumpSumsBands.flatMap(bands => bands.find(_.name.equals("BRT")))
    val gainsOnLifePoliciesTaxBand = gainsOnLifePoliciesBands.flatMap(bands => bands.find(_.name.equals("BRT")))

    (payPensionsProfitTaxBand, savingsTaxBand, dividendsTaxBand, lumpSumsTaxBand, gainsOnLifePoliciesTaxBand) match {
      case (Some(_), _, _, _, _) => payPensionsProfitTaxBand
      case (_, Some(_), _, _, _) => savingsTaxBand
      case (_, _, Some(_), _, _) => dividendsTaxBand
      case (_, _, _, Some(_), _) => lumpSumsTaxBand
      case (_, _, _, _, Some(_)) => gainsOnLifePoliciesTaxBand
      case _ => None
    }
  }
}

object TaxDueSummaryViewModel {

  def apply(calcResponse: LiabilityCalculationResponse): TaxDueSummaryViewModel = {
    calcResponse.calculation match {
      case Some(calc) => TaxDueSummaryViewModel(
        taxRegime = calcResponse.inputs.personalInformation.taxRegime,
        class2VoluntaryContributions = calcResponse.inputs.personalInformation.class2VoluntaryContributions.getOrElse(false),
        messages = calcResponse.messages,
        lossesAppliedToGeneralIncome = calc.allowancesAndDeductions.flatMap(ad => ad.lossesAppliedToGeneralIncome),
        grossGiftAidPayments = calc.giftAid.map(ga => ga.grossGiftAidPayments),
        giftAidTax = calc.giftAid.map(ga => ga.giftAidTax),
        marriageAllowanceTransferredInAmount = calc.marriageAllowanceTransferredIn.flatMap(mati => mati.amount),
        studentLoans = calc.studentLoans,
        reliefsClaimed = calc.reliefs.map(r => r.reliefsClaimed.getOrElse(Seq())),
        totalResidentialFinanceCostsRelief = calc.reliefs.flatMap(r => r.residentialFinanceCosts.map(rfc => rfc.totalResidentialFinanceCostsRelief)),
        totalForeignTaxCreditRelief = calc.reliefs.flatMap(r => r.foreignTaxCreditRelief.map(rfc => rfc.totalForeignTaxCreditRelief)),
        topSlicingReliefAmount = calc.reliefs.flatMap(r => r.topSlicingRelief.flatMap(tsr => tsr.amount)),
        totalTaxableIncome = calc.taxCalculation.map(tc => BigDecimal(tc.incomeTax.totalTaxableIncome)),
        payPensionsProfitBands = calc.taxCalculation.flatMap(tc => tc.incomeTax.payPensionsProfit.map(ppp => ppp.taxBands.getOrElse(Seq()))),
        savingsAndGainsBands = calc.taxCalculation.flatMap(tc => tc.incomeTax.savingsAndGains.map(ppp => ppp.taxBands.getOrElse(Seq()))),
        lumpSumsBands = calc.taxCalculation.flatMap(tc => tc.incomeTax.lumpSums.map(ppp => ppp.taxBands.getOrElse(Seq()))),
        dividendsBands = calc.taxCalculation.flatMap(tc => tc.incomeTax.dividends.map(ppp => ppp.taxBands.getOrElse(Seq()))),
        gainsOnLifePoliciesBands = calc.taxCalculation.flatMap(tc => tc.incomeTax.gainsOnLifePolicies.map(ppp => ppp.taxBands.getOrElse(Seq()))),
        totalNotionalTax = calc.taxCalculation.flatMap(tc => tc.incomeTax.totalNotionalTax),
        incomeTaxDueAfterTaxReductions = calc.taxCalculation.flatMap(tc => tc.incomeTax.incomeTaxDueAfterTaxReductions),
        totalPensionSavingsTaxCharges = calc.taxCalculation.flatMap(tc => tc.incomeTax.totalPensionSavingsTaxCharges),
        statePensionLumpSumCharges = calc.taxCalculation.flatMap(tc => tc.incomeTax.statePensionLumpSumCharges),
        payeUnderpaymentsCodedOut = calc.taxCalculation.flatMap(tc => tc.incomeTax.payeUnderpaymentsCodedOut),
        nic4Bands = calc.taxCalculation.flatMap(tc => tc.nics.flatMap(nics => nics.class4Nics.map(class4nics => class4nics.nic4Bands))),
        class2NicsAmount = calc.taxCalculation.flatMap(tc => tc.nics.flatMap(nics => nics.class2Nics.flatMap(class2 => class2.amount))),
        capitalGainsTax = CapitalGainsTaxViewModel(calc.taxCalculation.flatMap(tc => tc.capitalGainsTax)),
        totalStudentLoansRepaymentAmount = calc.taxCalculation.flatMap(tc => tc.totalStudentLoansRepaymentAmount),
        saUnderpaymentsCodedOut = calc.taxCalculation.flatMap(tc => tc.saUnderpaymentsCodedOut),
        totalIncomeTaxAndNicsDue = Some(getTaxDue(calcResponse)),
        totalTaxDeducted = calc.taxCalculation.flatMap(tc => tc.totalTaxDeducted),
        taxDeductedAtSource = TaxDeductedAtSourceViewModel(calc.taxDeductedAtSource),
        totalAnnuityPaymentsTaxCharged = calc.taxCalculation.flatMap(tc => tc.totalAnnuityPaymentsTaxCharged),
        totalRoyaltyPaymentsTaxCharged = calc.taxCalculation.flatMap(tc => tc.totalRoyaltyPaymentsTaxCharged)
      )
      case None => TaxDueSummaryViewModel()
    }
  }
}
