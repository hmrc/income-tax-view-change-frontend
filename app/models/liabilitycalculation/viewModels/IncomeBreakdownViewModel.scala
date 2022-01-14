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

package models.liabilitycalculation.viewModels

case class IncomeBreakdownViewModel(
                                     totalPayeEmploymentAndLumpSumIncome: Option[BigDecimal],
                                     totalBenefitsInKind: Option[BigDecimal],
                                     totalEmploymentExpenses: Option[BigDecimal],
                                     totalSelfEmploymentProfit: Option[BigDecimal],
                                     totalPropertyProfit: Option[BigDecimal],
                                     totalFHLPropertyProfit: Option[BigDecimal],
                                     totalForeignPropertyProfit: Option[BigDecimal],
                                     totalEeaFhlProfit: Option[BigDecimal],
                                     chargeableForeignDividends: Option[BigDecimal],
                                     chargeableForeignSavingsAndGains: Option[BigDecimal],
                                     chargeableOverseasPensionsStateBenefitsRoyalties: Option[BigDecimal],
                                     chargeableAllOtherIncomeReceivedWhilstAbroad: Option[BigDecimal],
                                     totalOverseasIncomeAndGains: Option[BigDecimal],
                                     totalForeignBenefitsAndGifts: Option[BigDecimal],
                                     savingsAndGainsTaxableIncome: Option[BigDecimal],
                                     totalOfAllGains: Option[BigDecimal],
                                     dividendsTaxableIncome: Option[BigDecimal],
                                     totalOccupationalPensionIncome: Option[BigDecimal],
                                     totalStateBenefitsIncome: Option[BigDecimal],
                                     totalShareSchemesIncome: Option[BigDecimal],
                                     totalIncomeReceived: Option[BigDecimal]
                                   )
