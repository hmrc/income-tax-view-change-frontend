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
import models.incomeSourceDetails._

import java.time.LocalDate

case class ManageIncomeSourceDetailsViewModel(incomeSourceId: IncomeSourceId,
                                              incomeSource: Option[String],
                                              tradingName: Option[String],
                                              tradingStartDate: Option[LocalDate],
                                              address: Option[AddressModel],
                                              isTraditionalAccountingMethod: Option[Boolean],
                                              latencyYearsQuarterly: LatencyYearsQuarterly,
                                              latencyYearsAnnual: LatencyYearsAnnual,
                                              latencyYearsCrystallised: LatencyYearsCrystallised,
                                              latencyDetails: Option[LatencyDetails],
                                              incomeSourceType: IncomeSourceType,
                                              quarterReportingType: Option[QuarterReportingType],
                                              currentTaxYearEnd: Int
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

  //TODO: Could be removed while oldIncomeSource journey is cleaned up.
  def shouldShowTaxYears: Boolean = {
    latencyYearsQuarterly.secondYear.getOrElse(false) && latencyDetails.isDefined
  }

  def isBusinessInLatency: Boolean = {
    latencyDetails.exists(_.isBusinessOrPropertyInLatency(currentTaxYearEnd))
  }

  def showLatencyDetails: Boolean = {
    latencyYearsCrystallised.secondYear.contains(false)
  }

  def shouldShowChangeLinksForTaxYearOne: Boolean = {
    isBusinessInLatency &&
    (latencyYearsCrystallised.firstYear contains false) &&
      ((latencyDetails.exists(_.latencyIndicator1 == "A") && latencyYearsAnnual.firstYear.contains(false)) ||
        latencyDetails.exists(_.latencyIndicator1 == "Q"))
  }

  def shouldShowChangeLinksForTaxYearTwo: Boolean = {
    isBusinessInLatency &&
    (latencyYearsCrystallised.secondYear contains false) &&
      ((latencyDetails.exists(_.latencyIndicator2 == "A") && latencyYearsAnnual.secondYear.contains(false)) ||
        latencyDetails.exists(_.latencyIndicator2 == "Q"))
  }
}



