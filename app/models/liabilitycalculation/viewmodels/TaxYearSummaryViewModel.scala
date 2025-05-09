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

import models.incomeSourceDetails.TaxYear
import models.obligations.ObligationsModel
import models.taxyearsummary.TaxYearSummaryChargeItem

case class TaxYearSummaryViewModel(calculationSummary: Option[CalculationSummary],
                                   charges: List[TaxYearSummaryChargeItem],
                                   obligations: ObligationsModel,
                                   showForecastData: Boolean = false,
                                   ctaViewModel: TYSClaimToAdjustViewModel,
                                   LPP2Url: String
                                  ) {

  def showUpdates: Boolean = {
    obligations.obligations.exists(_.obligations.nonEmpty)
  }

  calculationSummary.filter(_ => showForecastData).foreach(calculationSummaryValue => {
    require(calculationSummaryValue.forecastIncomeTaxAndNics.isDefined, "missing Forecast Tax Due")
    require(calculationSummaryValue.timestamp.isDefined, "missing Calculation timestamp")
  })

}

case class TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled: Boolean,
                                     poaTaxYear: Option[TaxYear]) {

  val claimToAdjustTaxYear: Option[TaxYear] = {
    if (adjustPaymentsOnAccountFSEnabled) {
      poaTaxYear
    } else {
      None
    }
  }

}