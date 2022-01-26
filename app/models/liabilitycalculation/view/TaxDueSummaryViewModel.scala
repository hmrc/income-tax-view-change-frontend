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

import models.liabilitycalculation.taxcalculation.{BusinessAssetsDisposalsAndInvestorsRel, CgtTaxBands, Nic4Bands, TaxBands}
import models.liabilitycalculation.{Calculation, Messages, ReliefsClaimed}

case class TaxDueSummaryViewModel(
                                   taxRegime: Option[String] = None,
                                   class2VoluntaryContributions: Option[Boolean] = None,
                                   messages: Option[Messages] = None,
                                   lossesAppliedToGeneralIncome: Option[Int] = None,
                                   grossGiftAidPayments: Option[BigDecimal] = None,
                                   giftAidTax: Option[BigDecimal] = None,
                                   marriageAllowanceTransferredInAmount: Option[BigDecimal] = None,
                                   reliefsClaimed: Option[Seq[ReliefsClaimed]] = None,
                                   totalResidentialFinanceCostsRelief: Option[BigDecimal] = None,
                                   totalForeignTaxCreditRelief: Option[BigDecimal] = None,
                                   topSlicingReliefAmount: Option[BigDecimal] = None,
                                   totalTaxableIncome: Option[BigDecimal] = None,
                                   payPensionsProfit: Seq[TaxBands] = Seq(),
                                   savingsAndGains: Seq[TaxBands] = Seq(),
                                   lumpSums: Seq[TaxBands] = Seq(),
                                   dividends: Seq[TaxBands] = Seq(),
                                   gainsOnLifePolicies: Seq[TaxBands] = Seq(),
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
                                   taxDeductedAtSource: TaxDeductedAtSourceViewModel = TaxDeductedAtSourceViewModel()
                                 ) {

  def getRateHeaderKey: String = {
    taxRegime match {
      case Some("Scotland") => ".scotland"
      case _ => ".uk"
    }
  }

  def getModifiedBaseTaxBand: Option[TaxBands] = {
    val payPensionsProfitTaxBand = payPensionsProfit.find(_.name.equals("BRT"))
    val savingsTaxBand = savingsAndGains.find(_.name.equals("BRT"))
    val dividendsTaxBand = dividends.find(_.name.equals("BRT"))
    val lumpSumsTaxBand = lumpSums.find(_.name.equals("BRT"))
    val gainsOnLifePoliciesTaxBand = gainsOnLifePolicies.find(_.name.equals("BRT"))

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

  def apply(calcOpt: Option[Calculation]): TaxDueSummaryViewModel = {
    calcOpt match {
      case Some(calc) => TaxDueSummaryViewModel(
        taxRegime = None,
        class2VoluntaryContributions = None,
        messages = None,
        lossesAppliedToGeneralIncome = None,
        grossGiftAidPayments = None,
        giftAidTax = None,
        marriageAllowanceTransferredInAmount = None,
        reliefsClaimed = None,
        totalResidentialFinanceCostsRelief = None,
        totalForeignTaxCreditRelief = None,
        topSlicingReliefAmount = None,
        totalTaxableIncome = None,
        payPensionsProfit = Seq(),
        savingsAndGains = Seq(),
        lumpSums = Seq(),
        dividends = Seq(),
        gainsOnLifePolicies = Seq(),
        totalNotionalTax = None,
        incomeTaxDueAfterTaxReductions = None,
        totalPensionSavingsTaxCharges = None,
        statePensionLumpSumCharges = None,
        payeUnderpaymentsCodedOut = None,
        nic4Bands = None,
        class2NicsAmount = None,
        capitalGainsTax = CapitalGainsTaxViewModel(
          totalTaxableGains = None,
          adjustments = None,
          foreignTaxCreditRelief = None,
          taxOnGainsAlreadyPaid = None,
          capitalGainsTaxDue = None,
          capitalGainsOverpaid = None,
          propertyAndInterestTaxBands = Seq(),
          otherGainsTaxBands = Seq(),
          businessAssetsDisposalsAndInvestorsRel = None
        ),
        totalStudentLoansRepaymentAmount = None,
        saUnderpaymentsCodedOut = None,
        totalIncomeTaxAndNicsDue = None,
        totalTaxDeducted = None,
        taxDeductedAtSource = TaxDeductedAtSourceViewModel(
          ukLandAndProperty = None,
          cis = None,
          voidISAs = None,
          payeEmployments = None,
          ukPensions = None,
          stateBenefits = None,
          specialWithholdingTax = None,
          inYearAdjustmentCodedInLaterTaxYear = None
        )
      )
      case None => TaxDueSummaryViewModel()
    }
  }

}

case class CapitalGainsTaxViewModel(
                                     totalTaxableGains: Option[BigDecimal] = None,
                                     adjustments: Option[BigDecimal] = None,
                                     foreignTaxCreditRelief: Option[BigDecimal] = None,
                                     taxOnGainsAlreadyPaid: Option[BigDecimal] = None,
                                     capitalGainsTaxDue: Option[BigDecimal] = None,
                                     capitalGainsOverpaid: Option[BigDecimal] = None,
                                     propertyAndInterestTaxBands: Seq[CgtTaxBands] = Seq(),
                                     otherGainsTaxBands: Seq[CgtTaxBands] = Seq(),
                                     businessAssetsDisposalsAndInvestorsRel: Option[BusinessAssetsDisposalsAndInvestorsRel] = None
                                   )

case class TaxDeductedAtSourceViewModel(
                                         payeEmployments: Option[BigDecimal] = None,
                                         ukPensions: Option[BigDecimal] = None,
                                         stateBenefits: Option[BigDecimal] = None,
                                         cis: Option[BigDecimal] = None,
                                         ukLandAndProperty: Option[BigDecimal] = None,
                                         specialWithholdingTax: Option[BigDecimal] = None,
                                         voidISAs: Option[BigDecimal] = None,
                                         savings: Option[BigDecimal] = None,
                                         inYearAdjustmentCodedInLaterTaxYear: Option[BigDecimal] = None,
                                         total: Option[BigDecimal] = None,
                                         totalIncomeTaxAndNicsDue: Option[BigDecimal] = None
                                       ) {
  val allFields: Seq[(String, BigDecimal)] = Seq(
    "inYearAdjustment" -> inYearAdjustmentCodedInLaterTaxYear,
    "payeEmployments" -> payeEmployments,
    "ukPensions" -> ukPensions,
    "stateBenefits" -> stateBenefits,
    "cis" -> cis,
    "ukLandAndProperty" -> ukLandAndProperty,
    "specialWithholdingTax" -> specialWithholdingTax,
    "voidISAs" -> voidISAs,
    "savings" -> savings
  ).collect {
    case (key, Some(amount)) => key -> amount
  }
  val nonEmpty: Boolean = allFields.nonEmpty
}

