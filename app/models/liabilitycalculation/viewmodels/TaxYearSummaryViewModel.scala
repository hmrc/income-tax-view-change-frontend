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

import implicits.ImplicitDateParser
import models.liabilitycalculation.LiabilityCalculationResponse

import java.time.LocalDate

case class TaxYearSummaryViewModel(timestamp: Option[LocalDate],
                                   crystallised: Option[Boolean],
                                   unattendedCalc: Boolean,
                                   taxDue: BigDecimal,
                                   income: Int,
                                   deductions: BigDecimal,
                                   totalTaxableIncome: Int,
                                   forecastIncome: Option[Int] = None,
                                   forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                                   forecastAllowancesAndDeductions: Option[BigDecimal] = None,
                                   forecastTotalTaxableIncome:Option[Int] = None,
                                   periodFrom: Option[LocalDate] = None,
                                   periodTo: Option[LocalDate] = None)

object TaxYearSummaryViewModel extends ImplicitDateParser {
  def isUnattendedCalc(calculationReason: Option[String]): Boolean = calculationReason match {
    case Some("unattendedCalculation") => true
    case _ => false
  }

  def getTaxDue(calc:LiabilityCalculationResponse): BigDecimal = {
// totalIncomeTaxAndNicsAndCgt shall be used as tax due, in-case the values is none totalIncomeTaxAndNicsDue shall be used
    val totalIncomeTaxAndNicsDue = calc.calculation.flatMap(c => c.taxCalculation.map(_.totalIncomeTaxAndNicsDue))
    val totalIncomeTaxAndNicsAndCgt = calc.calculation.flatMap(c => c.taxCalculation.flatMap(_.totalIncomeTaxAndNicsAndCgt))

    totalIncomeTaxAndNicsAndCgt.getOrElse(totalIncomeTaxAndNicsDue.getOrElse(BigDecimal(0)))

  }

  def apply(calc: LiabilityCalculationResponse): TaxYearSummaryViewModel = {

    TaxYearSummaryViewModel(
      timestamp = calc.metadata.calculationTimestamp.map(_.toZonedDateTime.toLocalDate),
      crystallised = calc.metadata.crystallised,
      unattendedCalc = isUnattendedCalc(calc.metadata.calculationReason),
      taxDue = getTaxDue(calc),
      income = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalIncomeReceivedFromAllSources)).getOrElse(0),
      deductions = calc.calculation.flatMap(c => c.taxCalculation.map(tc => tc.incomeTax.totalAllowancesDeductionsReliefs)).getOrElse(0.00),
      totalTaxableIncome = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalTaxableIncome)).getOrElse(0),
      forecastIncome = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalEstimatedIncome)),
      forecastIncomeTaxAndNics = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.incomeTaxNicAndCgtAmount)),
      forecastAllowancesAndDeductions = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalAllowancesAndDeductions)),
      forecastTotalTaxableIncome = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalTaxableIncome)),
      periodFrom = calc.metadata.periodFrom,
      periodTo = calc.metadata.periodTo
    )
  }
}


