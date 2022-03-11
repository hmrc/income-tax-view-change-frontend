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

import models.liabilitycalculation.LiabilityCalculationResponse

case class TaxYearOverviewViewModel(timestamp: String,
                                    crystallised: Option[Boolean],
                                    taxDue: BigDecimal,
                                    income: Int,
                                    deductions: BigDecimal,
                                    totalTaxableIncome: Int)

object TaxYearOverviewViewModel {
  def apply(calc: LiabilityCalculationResponse): TaxYearOverviewViewModel = {
    TaxYearOverviewViewModel(
      timestamp = calc.metadata.calculationTimestamp,
      crystallised = calc.metadata.crystallised,
      taxDue = calc.calculation.flatMap(c => c.taxCalculation.map(_.totalIncomeTaxAndNicsDue)).getOrElse(0.00),
      income = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalIncomeReceivedFromAllSources)).getOrElse(0),
      deductions = calc.calculation.flatMap(c => c.taxCalculation.map(tc => tc.incomeTax.totalAllowancesDeductionsReliefs)).getOrElse(0.00),
      totalTaxableIncome = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalTaxableIncome)).getOrElse(0)
    )
  }
}


