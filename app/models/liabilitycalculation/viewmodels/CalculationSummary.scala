/*
 * Copyright 2024 HM Revenue & Customs
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
import models.liabilitycalculation.{LiabilityCalculationResponse, Messages}

import java.time.LocalDate

case class CalculationSummary(timestamp: Option[LocalDate],
                              crystallised: Boolean,
                              unattendedCalc: Boolean,
                              taxDue: BigDecimal,
                              income: Int,
                              deductions: BigDecimal,
                              totalTaxableIncome: Int,
                              forecastIncome: Option[Int] = None,
                              forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                              forecastAllowancesAndDeductions: Option[BigDecimal] = None,
                              forecastTotalTaxableIncome: Option[Int] = None,
                              periodFrom: Option[LocalDate] = None,
                              periodTo: Option[LocalDate] = None,
                              messages: Option[Messages] = None) {

  def errorPresent() = {
    messages.exists(_.errorMessages.nonEmpty)
  }

}

object CalculationSummary extends ImplicitDateParser {
  private def getEstimatedTotalTax(calc: LiabilityCalculationResponse): Option[BigDecimal] = {
    val incomeTaxNicAndCgtAmount: Option[BigDecimal] = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.incomeTaxNicAndCgtAmount))
    val incomeTaxNicAmount: Option[BigDecimal] = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.incomeTaxNicAmount))
    incomeTaxNicAndCgtAmount match {
      case Some(x) => Some(x)
      case None => incomeTaxNicAmount
    }
  }

  private def isUnattendedCalc(calculationReason: Option[String]): Boolean = calculationReason match {
    case Some("unattendedCalculation") => true
    case _ => false
  }

  def getTaxDue(calc: LiabilityCalculationResponse): BigDecimal = {
    val totalIncomeTaxAndNicsDue = calc.calculation.flatMap(c => c.taxCalculation.map(_.totalIncomeTaxAndNicsDue))
    val totalIncomeTaxAndNicsAndCgt = calc.calculation.flatMap(c => c.taxCalculation.flatMap(_.totalIncomeTaxAndNicsAndCgt))
    totalIncomeTaxAndNicsAndCgt.getOrElse(totalIncomeTaxAndNicsDue.getOrElse(BigDecimal(0)))
  }

  def apply(calc: LiabilityCalculationResponse): CalculationSummary = {

    CalculationSummary(
      timestamp = calc.metadata.calculationTimestamp.map(_.toZonedDateTime.toLocalDate),
      crystallised = calc.metadata.isCalculationCrystallised,
      unattendedCalc = isUnattendedCalc(calc.metadata.calculationReason),
      taxDue = getTaxDue(calc),
      income = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalIncomeReceivedFromAllSources)).getOrElse(0),
      deductions = calc.calculation.flatMap(c => c.taxCalculation.map(tc => tc.incomeTax.totalAllowancesAndDeductions)).getOrElse[Int](0),
      totalTaxableIncome = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalTaxableIncome)).getOrElse(0),
      forecastIncome = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalEstimatedIncome)),
      forecastIncomeTaxAndNics = getEstimatedTotalTax(calc),
      forecastAllowancesAndDeductions = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalAllowancesAndDeductions)),
      forecastTotalTaxableIncome = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalTaxableIncome)),
      periodFrom = calc.metadata.periodFrom,
      periodTo = calc.metadata.periodTo,
      messages = calc.messages
    )
  }
}