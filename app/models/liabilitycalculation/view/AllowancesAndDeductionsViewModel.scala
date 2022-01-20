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

import models.liabilitycalculation.Calculation

case class AllowancesAndDeductionsViewModel(
                                             personalAllowance: Option[BigDecimal] = None,
                                             reducedPersonalAllowance: Option[BigDecimal] = None,
                                             personalAllowanceBeforeTransferOut: Option[BigDecimal] = None,
                                             transferredOutAmount: Option[BigDecimal] = None,
                                             pensionContributions: Option[BigDecimal] = None,
                                             lossesAppliedToGeneralIncome: Option[BigDecimal] = None,
                                             giftOfInvestmentsAndPropertyToCharity: Option[BigDecimal] = None,
                                             grossAnnuityPayments: Option[BigDecimal] = None,
                                             qualifyingLoanInterestFromInvestments: Option[BigDecimal] = None,
                                             postCessationTradeReceipts: Option[BigDecimal] = None,
                                             paymentsToTradeUnionsForDeathBenefits: Option[BigDecimal] = None,
                                             totalAllowancesAndDeductions: Option[BigDecimal] = None,
                                             totalReliefs: Option[BigDecimal] = None
                                           ) {
  val totalAllowancesDeductionsReliefs: Option[BigDecimal] = (totalAllowancesAndDeductions ++ totalReliefs).reduceOption(_ + _)

  val personalAllowanceDisplayValue: Option[BigDecimal] =
    personalAllowanceBeforeTransferOut.fold(reducedPersonalAllowance.fold(personalAllowance)(Some(_)))(Some(_))

  def getAllowancesAndDeductionsViewModel(calcOpt: Option[Calculation]): AllowancesAndDeductionsViewModel = {
    calcOpt match {
      case Some(calc) =>
        AllowancesAndDeductionsViewModel(
          personalAllowance = calc.allowancesAndDeductions.flatMap(ad => ad.personalAllowance.map(pa => BigDecimal(pa))),
          reducedPersonalAllowance = calc.allowancesAndDeductions.flatMap(ad => ad.reducedPersonalAllowance.map(rpa => BigDecimal(rpa))),
          personalAllowanceBeforeTransferOut = calc.allowancesAndDeductions.flatMap(ad =>
            ad.marriageAllowanceTransferOut.flatMap(ma => Some(ma.personalAllowanceBeforeTransferOut))),
          transferredOutAmount = calc.allowancesAndDeductions.flatMap(ad =>
            ad.marriageAllowanceTransferOut.flatMap(ma => Some(ma.transferredOutAmount))),
          pensionContributions = calc.allowancesAndDeductions.flatMap(ad => ad.pensionContributions),
          lossesAppliedToGeneralIncome = calc.allowancesAndDeductions.flatMap(ad => ad.lossesAppliedToGeneralIncome.map(la => BigDecimal(la))),
          giftOfInvestmentsAndPropertyToCharity = calc.allowancesAndDeductions.flatMap(ad =>
            ad.giftOfInvestmentsAndPropertyToCharity.map(gift => BigDecimal(gift))),
          grossAnnuityPayments = calc.allowancesAndDeductions.flatMap(ad => ad.grossAnnuityPayments),
          qualifyingLoanInterestFromInvestments = calc.allowancesAndDeductions.flatMap(ad => ad.qualifyingLoanInterestFromInvestments),
          postCessationTradeReceipts = calc.allowancesAndDeductions.flatMap(ad => ad.postCessationTradeReceipts),
          paymentsToTradeUnionsForDeathBenefits = calc.allowancesAndDeductions.flatMap(ad => ad.paymentsToTradeUnionsForDeathBenefits),
          totalAllowancesAndDeductions = calc.taxCalculation.map(tc => tc.incomeTax.totalAllowancesAndDeductions),
          totalReliefs = calc.taxCalculation.flatMap(tc => tc.incomeTax.totalReliefs)
        )
      case None => AllowancesAndDeductionsViewModel()
    }

  }
}
