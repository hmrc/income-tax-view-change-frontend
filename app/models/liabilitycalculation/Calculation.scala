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

package models.liabilitycalculation

import exceptions.MissingFieldException
import models.liabilitycalculation.taxcalculation.TaxCalculation
import play.api.libs.json.{Json, OFormat}

case class Calculation(
                        partnerIncome: Option[PartnerIncome] = None,
                        allowancesAndDeductions: Option[AllowancesAndDeductions] = None,
                        reliefs: Option[Reliefs] = None,
                        taxDeductedAtSource: Option[TaxDeductedAtSource] = None,
                        giftAid: Option[GiftAid] = None,
                        marriageAllowanceTransferredIn: Option[MarriageAllowanceTransferredIn] = None,
                        studentLoans: Option[Seq[StudentLoan]] = None,
                        employmentAndPensionsIncome: Option[EmploymentAndPensionsIncome] = None,
                        employmentExpenses: Option[EmploymentExpenses] = None,
                        stateBenefitsIncome: Option[StateBenefitsIncome] = None,
                        shareSchemesIncome: Option[ShareSchemesIncome] = None,
                        foreignIncome: Option[ForeignIncome] = None,
                        chargeableEventGainsIncome: Option[ChargeableEventGainsIncome] = None,
                        savingsAndGainsIncome: Option[SavingsAndGainsIncome] = None,
                        dividendsIncome: Option[DividendsIncome] = None,
                        otherIncome: Option[OtherIncome] = None,
                        incomeSummaryTotals: Option[IncomeSummaryTotals] = None,
                        taxCalculation: Option[TaxCalculation] = None,
                        endOfYearEstimate: Option[EndOfYearEstimate] = None,
                        pensionSavingsTaxCharges: Option[PensionSavingsTaxCharges] = None,
                        transitionProfit: Option[TransitionProfit] = None,
                        highIncomeChildBenefitCharge: Option[HighIncomeChildBenefitCharge] = None
                      )

object Calculation {
  implicit val format: OFormat[Calculation] = Json.format[Calculation]
}

case class PartnerIncome(totalPartnerIncome: BigDecimal)

object PartnerIncome {
  implicit val format: OFormat[PartnerIncome] = Json.format[PartnerIncome]
}

case class ChargeableEventGainsIncome(totalOfAllGains: Int)

object ChargeableEventGainsIncome {
  implicit val format: OFormat[ChargeableEventGainsIncome] = Json.format[ChargeableEventGainsIncome]
}

case class DividendsIncome(totalUkDividends: Option[Int] = None, chargeableForeignDividends: Option[Int] = None)

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

final case class OtherIncome(totalOtherIncome: BigDecimal)

object OtherIncome {
  implicit val format: OFormat[OtherIncome] = Json.format[OtherIncome]
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

case class StateBenefitsIncome(totalStateBenefitsIncome: Option[BigDecimal] = None,
                               totalStateBenefitsIncomeExcStatePensionLumpSum: Option[BigDecimal] = None)

object StateBenefitsIncome {
  implicit val format: OFormat[StateBenefitsIncome] = Json.format[StateBenefitsIncome]
}

case class StudentLoan(planType: Option[String] = None,
                       studentLoanTotalIncomeAmount: Option[BigDecimal] = None,
                       studentLoanChargeableIncomeAmount: Option[BigDecimal] = None,
                       studentLoanRepaymentAmount: Option[BigDecimal] = None,
                       studentLoanRepaymentAmountNetOfDeductions: Option[BigDecimal] = None,
                       studentLoanApportionedIncomeThreshold: Option[BigDecimal] = None,
                       studentLoanRate: Option[BigDecimal] = None
                      ) {
  def planTypeActual: String = planType.map(_.substring(1)).getOrElse(throw MissingFieldException("Plan type"))
  def studentLoanRepaymentAmountActual: BigDecimal = studentLoanRepaymentAmount.getOrElse(throw MissingFieldException("Student Loan Repayment Amount"))
}

object StudentLoan {
  implicit val format: OFormat[StudentLoan] = Json.format[StudentLoan]
}

case class PensionSavingsTaxCharges(totalPensionCharges: Option[BigDecimal] = None,
                                    totalTaxPaid: Option[BigDecimal] = None,
                                    totalPensionChargesDue: Option[BigDecimal] = None)

object PensionSavingsTaxCharges {
  implicit val format: OFormat[PensionSavingsTaxCharges] = Json.format[PensionSavingsTaxCharges]
}

case class TransitionProfit(totalTaxableTransitionProfit: Option[BigDecimal] = None)

object TransitionProfit {
  implicit val format: OFormat[TransitionProfit] = Json.format[TransitionProfit]
}

case class HighIncomeChildBenefitCharge(adjustedNetIncome: BigDecimal,
                                        amountOfChildBenefitReceived: BigDecimal,
                                        incomeThreshold: BigDecimal,
                                        childBenefitChargeTaper: BigDecimal,
                                        rate: Short,
                                        highIncomeChildBenefitCharge: BigDecimal)

object HighIncomeChildBenefitCharge {
  implicit val format: OFormat[HighIncomeChildBenefitCharge] = Json.format[HighIncomeChildBenefitCharge]
}
