/*
 * Copyright 2021 HM Revenue & Customs
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

package models.calculation

import models.financialTransactions.TransactionModel

case class CalcOverview(timestamp: Option[String],
                        income: BigDecimal,
                        deductions: BigDecimal,
                        totalTaxableIncome: BigDecimal,
                        taxDue: BigDecimal,
                        payment: BigDecimal,
                        totalRemainingDue: BigDecimal,
                        crystallised: Boolean)

object CalcOverview {

  def apply(calculation: Calculation, transaction: Option[TransactionModel]): CalcOverview = {
    val valueDue: BigDecimal = calculation.totalIncomeTaxAndNicsDue.getOrElse(0.00)
    CalcOverview(
      timestamp = calculation.timestamp,
      income = calculation.totalIncomeReceived.getOrElse(0.00),
      deductions = calculation.allowancesAndDeductions.totalAllowancesDeductionsReliefs.getOrElse(0.00),
      totalTaxableIncome = calculation.totalTaxableIncome.getOrElse(0.00),
      taxDue = calculation.totalIncomeTaxAndNicsDue.getOrElse(0.00),
      payment = transaction.flatMap(_.clearedAmount).getOrElse(0.00),
      totalRemainingDue = transaction.flatMap(_.outstandingAmount).getOrElse(valueDue),
      crystallised = calculation.crystallised
    )
  }
}
