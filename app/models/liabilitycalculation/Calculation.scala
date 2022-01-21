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

package models.liabilitycalculation

import models.liabilitycalculation.taxcalculation.TaxCalculation
import play.api.libs.json.{Json, OFormat}
import models.liabilitycalculation.viewModels.IncomeBreakdownViewModel

case class Calculation(
                        allowancesAndDeductions: Option[AllowancesAndDeductions] = None,
                        reliefs: Option[Reliefs] = None,
                        taxDeductedAtSource: Option[TaxDeductedAtSource] = None,
                        giftAid: Option[GiftAid] = None,
                        marriageAllowanceTransferredIn: Option[MarriageAllowanceTransferredIn] = None,
                        employmentAndPensionsIncome: Option[EmploymentAndPensionsIncome] = None,
                        employmentExpenses: Option[EmploymentExpenses] = None,
                        stateBenefitsIncome: Option[StateBenefitsIncome] = None,
                        shareSchemesIncome: Option[ShareSchemesIncome] = None,
                        foreignIncome: Option[ForeignIncome] = None,
                        chargeableEventGainsIncome: Option[ChargeableEventGainsIncome] = None,
                        savingsAndGainsIncome: Option[SavingsAndGainsIncome] = None,
                        dividendsIncome: Option[DividendsIncome] = None,
                        incomeSummaryTotals: Option[IncomeSummaryTotals] = None,
                        taxCalculation: Option[TaxCalculation] = None
                      )

object Calculation {
  implicit val format: OFormat[Calculation] = Json.format[Calculation]

  def getIncomeBreakdownViewModel(c: Calculation): IncomeBreakdownViewModel = {
    IncomeBreakdownViewModel(
      totalPayeEmploymentAndLumpSumIncome = c.employmentAndPensionsIncome.flatMap(eapi => eapi.totalPayeEmploymentAndLumpSumIncome),
      totalBenefitsInKind = c.employmentAndPensionsIncome.flatMap(eapi => eapi.totalBenefitsInKind),
      totalEmploymentExpenses = c.employmentExpenses.flatMap(ee => ee.totalEmploymentExpenses),
      totalSelfEmploymentProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalSelfEmploymentProfit) map {i => i: BigDecimal},
      totalPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalPropertyProfit) map {i => i: BigDecimal},
      totalFHLPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalFHLPropertyProfit) map {i => i: BigDecimal},
      totalForeignPropertyProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalForeignPropertyProfit) map {i => i: BigDecimal},
      totalEeaFhlProfit = c.incomeSummaryTotals.flatMap(ist => ist.totalEeaFhlProfit) map {i => i: BigDecimal},
      chargeableForeignDividends = c.dividendsIncome.flatMap(di => di.chargeableForeignDividends) map {i => i: BigDecimal},
      chargeableForeignSavingsAndGains = c.savingsAndGainsIncome.flatMap(sagi => sagi.chargeableForeignSavingsAndGains) map {i => i: BigDecimal},
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
    )
  }
}

case class ChargeableEventGainsIncome(totalOfAllGains: Int)

object ChargeableEventGainsIncome {
  implicit val format: OFormat[ChargeableEventGainsIncome] = Json.format[ChargeableEventGainsIncome]
}

case class DividendsIncome(chargeableForeignDividends: Option[Int] = None)

object DividendsIncome {
  implicit val format: OFormat[DividendsIncome] = Json.format[DividendsIncome]
}

case class EmploymentAndPensionsIncome(
                                        totalPayeEmploymentAndLumpSumIncome: Option[BigDecimal] = None,
                                        totalOccupationalPensionIncome: Option[BigDecimal] = None,
                                        totalBenefitsInKind: Option[BigDecimal] = None
                                      )

object EmploymentAndPensionsIncome {
  implicit val format: OFormat[EmploymentAndPensionsIncome] = Json.format[EmploymentAndPensionsIncome]
}

case class EmploymentExpenses(totalEmploymentExpenses: Option[BigDecimal] = None)

object EmploymentExpenses {
  implicit val format: OFormat[EmploymentExpenses] = Json.format[EmploymentExpenses]
}

case class ForeignIncome(
                          chargeableOverseasPensionsStateBenefitsRoyalties: Option[BigDecimal] = None,
                          chargeableAllOtherIncomeReceivedWhilstAbroad: Option[BigDecimal] = None,
                          overseasIncomeAndGains: Option[OverseasIncomeAndGains],
                          totalForeignBenefitsAndGifts: Option[BigDecimal] = None
                        )

object ForeignIncome {
  implicit val format: OFormat[ForeignIncome] = Json.format[ForeignIncome]
}

case class OverseasIncomeAndGains(gainAmount: BigDecimal)

object OverseasIncomeAndGains {
  implicit val format: OFormat[OverseasIncomeAndGains] = Json.format[OverseasIncomeAndGains]
}

case class GiftAid(
                    grossGiftAidPayments: Int,
                    giftAidTax: BigDecimal
                  )

object GiftAid {
  implicit val format: OFormat[GiftAid] = Json.format[GiftAid]
}

case class IncomeSummaryTotals(
                                totalSelfEmploymentProfit: Option[Int] = None,
                                totalPropertyProfit: Option[Int] = None,
                                totalFHLPropertyProfit: Option[Int] = None,
                                totalForeignPropertyProfit: Option[Int] = None,
                                totalEeaFhlProfit: Option[Int] = None
                              )

object IncomeSummaryTotals {
  implicit val writes: OFormat[IncomeSummaryTotals] = Json.format[IncomeSummaryTotals]
}

case class MarriageAllowanceTransferredIn(
                                           amount: Option[BigDecimal] = None
                                         )

object MarriageAllowanceTransferredIn {
  implicit val format: OFormat[MarriageAllowanceTransferredIn] = Json.format[MarriageAllowanceTransferredIn]
}

case class SavingsAndGainsIncome(chargeableForeignSavingsAndGains: Option[Int] = None)

object SavingsAndGainsIncome {
  implicit val format: OFormat[SavingsAndGainsIncome] = Json.format[SavingsAndGainsIncome]
}

case class ShareSchemesIncome(totalIncome: BigDecimal)

object ShareSchemesIncome {
  implicit val format: OFormat[ShareSchemesIncome] = Json.format[ShareSchemesIncome]
}

case class StateBenefitsIncome(totalStateBenefitsIncome: Option[BigDecimal] = None)

object StateBenefitsIncome {
  implicit val format: OFormat[StateBenefitsIncome] = Json.format[StateBenefitsIncome]
}
