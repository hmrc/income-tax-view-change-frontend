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

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter

case class TaxYearSummaryViewModel(timestamp: Option[String],
                                   crystallised: Option[Boolean],
                                   unattendedCalc: Boolean,
                                   taxDue: BigDecimal,
                                   income: Int,
                                   deductions: BigDecimal,
                                   totalTaxableIncome: Int,
                                   forecastIncome: Option[Int] = None,
                                   forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                                   inYearCalculationRunDate: Option[String] = None)

object TaxYearSummaryViewModel {
  def apply(calc: LiabilityCalculationResponse, taxYear: Int): TaxYearSummaryViewModel = {
    TaxYearSummaryViewModel(
      timestamp = calc.metadata.calculationTimestamp,
      crystallised = calc.metadata.crystallised,
      unattendedCalc = isUnattendedCalc(calc.metadata.calculationReason),
      taxDue = calc.calculation.flatMap(c => c.taxCalculation.map(_.totalIncomeTaxAndNicsDue)).getOrElse(0.00),
      income = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalIncomeReceivedFromAllSources)).getOrElse(0),
      deductions = calc.calculation.flatMap(c => c.taxCalculation.map(tc => tc.incomeTax.totalAllowancesDeductionsReliefs)).getOrElse(0.00),
      totalTaxableIncome = calc.calculation.flatMap(c => c.taxCalculation.map(_.incomeTax.totalTaxableIncome)).getOrElse(0),
      forecastIncome = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.totalEstimatedIncome)),
      forecastIncomeTaxAndNics = calc.calculation.flatMap(c => c.endOfYearEstimate.flatMap(_.incomeTaxNicAndCgtAmount)),
      inYearCalculationRunDate = calc.metadata.calculationTimestamp.flatMap(timestamp => getInYearCalculationRunDate(timestamp, taxYear))
    )
  }

  def isUnattendedCalc(calculationReason: Option[String]): Boolean = calculationReason match {
    case Some("unattendedCalculation") => true
    case _ => false
  }

  def getInYearCalculationRunDate(calculationTimestamp: String, taxYear: Int): Option[String] = {
    val firstQuarterstart = LocalDate.parse(s"${taxYear - 1}-04-05")
    val firstQuarterEnd = LocalDate.parse(s"${taxYear - 1}-07-05")
    val SecondQuarterEnd = LocalDate.parse(s"${taxYear - 1}-10-05")
    val thirdQuarterEnd = LocalDate.parse(s"${taxYear}-01-05")
    val fourthQuarterEnd = LocalDate.parse(s"${taxYear}-04-05")

    val instant = java.time.Instant.parse(calculationTimestamp)
    val date = LocalDateTime.ofInstant(instant,ZoneId.of(ZoneOffset.UTC.getId)).toLocalDate

    date match {
      case date: LocalDate if (date.isBefore(firstQuarterEnd) || date.isEqual(firstQuarterEnd)) && date.isAfter(firstQuarterstart) => None
      case date: LocalDate if (date.isBefore(SecondQuarterEnd) || date.isEqual(SecondQuarterEnd)) && date.isAfter(firstQuarterEnd) => Some("5th July")
      case date: LocalDate if (date.isBefore(thirdQuarterEnd) || date.isEqual(thirdQuarterEnd)) && date.isAfter(SecondQuarterEnd) => Some("5th October")
      case date: LocalDate if (date.isBefore(fourthQuarterEnd) || date.isEqual(fourthQuarterEnd)) && date.isAfter(thirdQuarterEnd) => Some("5th January")
      case date: LocalDate if date.isAfter(fourthQuarterEnd)  => Some("5th April")
      case _ => None
    }
  }
}


