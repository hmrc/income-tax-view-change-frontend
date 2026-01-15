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

package models.liabilitycalculation.viewmodels

import auth.MtdItUser
import models.financialDetails.SecondLatePaymentPenalty
import models.incomeSourceDetails.TaxYear
import models.obligations.ObligationsModel
import models.taxyearsummary.TaxYearSummaryChargeItem

import java.time.LocalDate

case class TaxYearSummaryViewModel(calculationSummary: Option[CalculationSummary],
                                   previousCalculationSummary: Option[CalculationSummary],
                                   charges: List[TaxYearSummaryChargeItem],
                                   obligations: ObligationsModel,
                                   showForecastData: Boolean = false,
                                   ctaViewModel: TYSClaimToAdjustViewModel,
                                   LPP2Url: String,
                                   pfaEnabled: Boolean
                                  ) {

  def showSubmissions: Boolean = {
    obligations.obligations.exists(_.obligations.nonEmpty)
  }

  def getLastUpdatedObligationEndDate: Option[LocalDate] = {
    val obligationsSortedDateDescending = obligations.obligations
      .filter(_.obligations.nonEmpty)
      .maxByOption(_.obligations.flatMap(obligation => Some(obligation.end)).max)

    obligationsSortedDateDescending.flatMap { obligationsGroup =>
      val filteredObligations = obligationsGroup.obligations.filter(_.dateReceived.nonEmpty)
      if (filteredObligations.nonEmpty) {
        Some(filteredObligations.head.end)
      } else {
        None
      }
    }
  }

  def groupedObligations(implicit user: MtdItUser[_]) = obligations.allDeadlinesWithSource(previous = true)
    .reverse.groupBy[LocalDate] { nextUpdateWithIncomeType => nextUpdateWithIncomeType.obligation.due }
    .toList
    .collect {
      case (due, dueObligations) => (due, obligations.groupByQuarterPeriod(dueObligations.distinct))
    }
    .sortBy(_._1).reverse

  val forecastIncome = calculationSummary.flatMap(model => model.forecastIncome).getOrElse(0)
  val forecastTotalTaxableIncome = calculationSummary.flatMap(model => model.forecastTotalTaxableIncome).getOrElse(0)
  val forecastIncomeAndNics = calculationSummary.flatMap(model => model.forecastIncomeTaxAndNics).getOrElse(BigDecimal(0))

  val forecastDeductions = calculationSummary.flatMap(model => model.forecastAllowancesAndDeductions).getOrElse(BigDecimal(0))

  private def validateCalculationSummary(calculationSummary: Option[CalculationSummary]): Unit = {
    calculationSummary.filter(_ => showForecastData).foreach(calculationSummaryValue => {
      require(calculationSummaryValue.forecastIncomeTaxAndNics.isDefined, "missing Forecast Tax Due")
      require(calculationSummaryValue.timestamp.isDefined, "missing Calculation timestamp")
    })
  }

  validateCalculationSummary(calculationSummary)
  validateCalculationSummary(previousCalculationSummary)

  def getForecastSummaryHref(taxYear: Int, isAgent: Boolean): String = {
    if(isAgent) {
      controllers.routes.ForecastIncomeSummaryController.showAgent(taxYear).url
    } else {
      controllers.routes.ForecastIncomeSummaryController.show(taxYear).url
    }
  }

  def getForecastTaxDueHref(taxYear: Int, isAgent: Boolean): String = {
    if(isAgent) {
      controllers.routes.ForecastTaxCalcSummaryController.showAgent(taxYear).url
    } else {
      controllers.routes.ForecastTaxCalcSummaryController.show(taxYear).url
    }
  }

  def getChargeSummaryHref(chargeItem: TaxYearSummaryChargeItem,
                           taxYear: Int,
                           isAgent: Boolean,
                           origin: Option[String]): String = {
    if(chargeItem.transactionType == SecondLatePaymentPenalty) {
      LPP2Url
    } else {
      if(isAgent) {
        controllers.routes.ChargeSummaryController.showAgent(taxYear, chargeItem.transactionId, chargeItem.isAccruingInterest).url
      } else {
        controllers.routes.ChargeSummaryController.show(taxYear, chargeItem.transactionId, chargeItem.isAccruingInterest, origin).url
      }
    }
  }
}

case class TYSClaimToAdjustViewModel(poaTaxYear: Option[TaxYear]) {

  val claimToAdjustTaxYear: Option[TaxYear] = poaTaxYear
}