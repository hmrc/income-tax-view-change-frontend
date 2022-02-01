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

case class IncomeBreakdownViewModel(
                                     totalPayeEmploymentAndLumpSumIncome: Option[BigDecimal] = None,
                                     totalBenefitsInKind: Option[BigDecimal] = None,
                                     totalEmploymentExpenses: Option[BigDecimal] = None,
                                     totalSelfEmploymentProfit: Option[BigDecimal] = None,
                                     totalPropertyProfit: Option[BigDecimal] = None,
                                     totalFHLPropertyProfit: Option[BigDecimal] = None,
                                     totalForeignPropertyProfit: Option[BigDecimal] = None,
                                     totalEeaFhlProfit: Option[BigDecimal] = None,
                                     chargeableForeignDividends: Option[BigDecimal] = None,
                                     chargeableForeignSavingsAndGains: Option[BigDecimal] = None,
                                     chargeableOverseasPensionsStateBenefitsRoyalties: Option[BigDecimal] = None,
                                     chargeableAllOtherIncomeReceivedWhilstAbroad: Option[BigDecimal] = None,
                                     totalOverseasIncomeAndGains: Option[BigDecimal] = None,
                                     totalForeignBenefitsAndGifts: Option[BigDecimal] = None,
                                     savingsAndGainsTaxableIncome: Option[BigDecimal] = None,
                                     totalOfAllGains: Option[BigDecimal] = None,
                                     dividendsTaxableIncome: Option[BigDecimal] = None,
                                     totalOccupationalPensionIncome: Option[BigDecimal] = None,
                                     totalStateBenefitsIncome: Option[BigDecimal] = None,
                                     totalShareSchemesIncome: Option[BigDecimal] = None,
                                     totalIncomeReceived: Option[BigDecimal] = None
                                   )

object IncomeBreakdownViewModel {
  def apply(calcOpt: Option[Calculation]): Option[IncomeBreakdownViewModel] = {
    calcOpt match {
      case Some(c) =>
        Some(IncomeBreakdownViewModel(
          totalPayeEmploymentAndLumpSumIncome = c.employmentAndPensionsIncome.flatMap(eapi => eapi.totalPayeEmploymentAndLumpSumIncome),
          totalBenefitsInKind = c.employmentAndPensionsIncome.flatMap(eapi => eapi.totalBenefitsInKind),
          totalEmploymentExpenses = c.employmentExpenses.flatMap(ee => ee.totalEmploymentExpenses),
          totalSelfEmploymentProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalSelfEmploymentProfit) map { i => i: BigDecimal },
          totalPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalPropertyProfit) map { i => i: BigDecimal },
          totalFHLPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalFHLPropertyProfit) map { i => i: BigDecimal },
          totalForeignPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalForeignPropertyProfit) map { i => i: BigDecimal },
          totalEeaFhlProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalEeaFhlProfit) map { i => i: BigDecimal },
          chargeableForeignDividends = c.dividendsIncome.flatMap(di => di.chargeableForeignDividends) map { i => i: BigDecimal },
          chargeableForeignSavingsAndGains = c.savingsAndGainsIncome.flatMap(sagi => sagi.chargeableForeignSavingsAndGains) map { i => i: BigDecimal },
          chargeableOverseasPensionsStateBenefitsRoyalties = c.foreignIncome.flatMap(fi => fi.chargeableOverseasPensionsStateBenefitsRoyalties),
          chargeableAllOtherIncomeReceivedWhilstAbroad = c.foreignIncome.flatMap(fi => fi.chargeableAllOtherIncomeReceivedWhilstAbroad),
          totalOverseasIncomeAndGains = c.foreignIncome.flatMap(fi => fi.overseasIncomeAndGains.flatMap(oiag => Some(oiag.gainAmount))),
          totalForeignBenefitsAndGifts = c.foreignIncome.flatMap(fi => fi.totalForeignBenefitsAndGifts),
          savingsAndGainsTaxableIncome = c.taxCalculation.flatMap(tc =>
            Some(tc.incomeTax).flatMap(it => it.savingsAndGains.flatMap(sag => Some(sag.taxableIncome)))),
          totalOfAllGains = c.chargeableEventGainsIncome.flatMap(cegi => Some(cegi.totalOfAllGains)),
          dividendsTaxableIncome = c.taxCalculation.flatMap(tc => Some(tc.incomeTax).flatMap(it => it.dividends.flatMap(d => Some(d.taxableIncome)))),
          totalOccupationalPensionIncome = c.employmentAndPensionsIncome.flatMap(eapi => eapi.totalOccupationalPensionIncome),
          totalStateBenefitsIncome = c.stateBenefitsIncome.flatMap(sbi => sbi.totalStateBenefitsIncome),
          totalShareSchemesIncome = c.shareSchemesIncome.flatMap(ssi => Some(ssi.totalIncome)),
          totalIncomeReceived = c.taxCalculation.flatMap(tc => Some(tc.incomeTax).flatMap(it => Some(it.totalIncomeReceivedFromAllSources)))
        ))
      case _ => None
    }
  }
}
