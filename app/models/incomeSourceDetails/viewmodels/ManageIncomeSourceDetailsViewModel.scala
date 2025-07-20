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

package models.incomeSourceDetails.viewmodels

import enums.IncomeSourceJourney.IncomeSourceType
import models.core.{AddressModel, IncomeSourceId}
import models.incomeSourceDetails.{LatencyDetails, LatencyYearsCrystallised, LatencyYearsQuarterly, QuarterReportingType}

import java.time.LocalDate

case class ManageIncomeSourceDetailsViewModel(incomeSourceId: IncomeSourceId,
                                              incomeSource: Option[String],
                                              tradingName: Option[String],
                                              tradingStartDate: Option[LocalDate],
                                              address: Option[AddressModel],
                                              isTraditionalAccountingMethod: Option[Boolean],
                                              latencyYearsQuarterly: LatencyYearsQuarterly,
                                              latencyYearsCrystallised: LatencyYearsCrystallised,
                                              latencyDetails: Option[LatencyDetails],
                                              incomeSourceType: IncomeSourceType,
                                              quarterReportingType: Option[QuarterReportingType]
                                             ) {

  def latencyValueAsKey(latencyIndicator: String): String = {
    latencyIndicator match {
      case "A" => "annually"
      case "Q" => "quarterly"
    }
  }

  def businessAccountingMethodAsKey(isTraditionalAccountingMethod: Boolean): String = {
    if (isTraditionalAccountingMethod) {
      "incomeSources.manage.business-manage-details.traditional-accounting"
    } else {
      "incomeSources.manage.business-manage-details.cash-accounting"
    }
  }

  def shouldShowInsetText: Boolean = {
    latencyYearsQuarterly.firstYear.getOrElse(false) || latencyYearsQuarterly.secondYear.getOrElse(false)
  }

  def isBusinessInLatency: Boolean = {
    !latencyDetails.exists(_.taxYear2.toInt < dateService.getCurrentTaxYearEnd)
  }

  def shouldShowChangeLinksForTaxYear1: Boolean = {

  }
}



