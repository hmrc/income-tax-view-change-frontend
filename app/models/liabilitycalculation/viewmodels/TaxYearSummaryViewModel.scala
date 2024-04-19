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

import models.financialDetails.DocumentDetailWithDueDate
import models.nextUpdates.ObligationsModel


case class TaxYearSummaryViewModel(calculationSummary: Option[CalculationSummary],
                                   charges: List[DocumentDetailWithDueDate],
                                   obligations: ObligationsModel,
                                   codingOutEnabled: Boolean,
                                   showForecastData: Boolean = false,
                                   showUpdates: Boolean) {


  calculationSummary.filter(_ => showForecastData).foreach(calculationSummaryValue => {
    require(calculationSummaryValue.forecastIncomeTaxAndNics.isDefined, "missing Forecast Tax Due")
    require(calculationSummaryValue.timestamp.isDefined, "missing Calculation timestamp")
  })

  obligations.obligations.filter(_ => showUpdates).foreach(maybeObligation => {
    require(maybeObligation.obligations.nonEmpty)
  })


  require(charges.forall(_.documentDetail.originalAmount.isDefined), "missing originalAmount on charges")

}


