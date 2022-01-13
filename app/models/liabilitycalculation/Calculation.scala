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
import models.liabilitycalculation.taxcalculation._

case class Calculation(
                        allowancesAndDeductions: Option[AllowancesAndDeductions],
                        reliefs: Option[Reliefs],
                        taxDeductedAtSource: Option[TaxDeductedAtSource],
                        giftAid: Option[GiftAid],
                        marriageAllowanceTransferredIn: Option[MarriageAllowanceTransferredIn],
                        employmentAndPensionsIncome: Option[EmploymentAndPensionsIncome],
                        employmentExpenses: Option[EmploymentExpenses],
                        stateBenefitsIncome: Option[StateBenefitsIncome],
                        shareSchemesIncome: Option[ShareSchemesIncome],
                        foreignIncome: Option[ForeignIncome],
                        chargeableEventGainsIncome: Option[ChargeableEventGainsIncome],
                        savingsAndGainsIncome: Option[SavingsAndGainsIncome],
                        dividendsIncome: Option[DividendsIncome],
                        incomeSummaryTotals: Option[IncomeSummaryTotals],
                        taxCalculation: Option[TaxCalculation]
                      )

object Calculation {
  implicit val format: OFormat[Calculation] = Json.format[Calculation]

  def getAllowancesModel(c: Calculation): AllowancesModel = {
    AllowancesModel(
      personalAllowance = c.allowancesAndDeductions.flatMap(ad => ad.personalAllowance),
      reducedPersonalAllowance = c.allowancesAndDeductions.flatMap(ad => ad.reducedPersonalAllowance),
      personalAllowanceBeforeTransferOut = c.allowancesAndDeductions.flatMap(ad =>
        ad.marriageAllowanceTransferOut.flatMap(ma => Some(ma.personalAllowanceBeforeTransferOut))),
      transferredOutAmount = c.allowancesAndDeductions.flatMap(ad =>
        ad.marriageAllowanceTransferOut.flatMap(ma => Some(ma.transferredOutAmount))),
      pensionContributions = c.allowancesAndDeductions.flatMap(ad => ad.pensionContributions),
      lossesAppliedToGeneralIncome = c.allowancesAndDeductions.flatMap(ad => ad.lossesAppliedToGeneralIncome),
      giftOfInvestmentsAndPropertyToCharity = c.allowancesAndDeductions.flatMap(ad => ad.giftOfInvestmentsAndPropertyToCharity),
      grossAnnuityPayments = c.allowancesAndDeductions.flatMap(ad => ad.grossAnnuityPayments),
      qualifyingLoanInterestFromInvestments = c.allowancesAndDeductions.flatMap(ad => ad.qualifyingLoanInterestFromInvestments),
      postCessationTradeReceipts = c.allowancesAndDeductions.flatMap(ad => ad.postCessationTradeReceipts),
      paymentsToTradeUnionsForDeathBenefits = c.allowancesAndDeductions.flatMap(ad => ad.paymentsToTradeUnionsForDeathBenefits),
      totalAllowancesAndDeductions = c.taxCalculation.map(tc => tc.incomeTax.totalAllowancesAndDeductions),
      totalReliefs = c.taxCalculation.flatMap(tc => tc.incomeTax.totalReliefs)
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
