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

package models.incomeSourceDetails.viewmodels

import enums.IncomeSourceJourney.IncomeSourceType
import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface

import java.time.LocalDate

case class IncomeSourceCeasedObligationsViewModel(incomeSourceType: IncomeSourceType,
                                                  firstQuarterlyUpdate: Option[DatesModel],
                                                  finalDeclarationUpdate: Seq[DatesModel],
                                                  numberOfOverdueObligationCount: Int,
                                                  viewUpcomingUpdatesLinkMessageKey: String,
                                                  businessName: Option[String],
                                                  insetWarningMessageKey: Option[(String, String)],
                                                  currentTaxYear: TaxYear,
                                                  isAgent: Boolean) {

}

object IncomeSourceCeasedObligationsViewModel {
  def apply(obligationsViewModel: ObligationsViewModel,
            incomeSourceType: IncomeSourceType,
            businessName: Option[String],
            cessationDate: LocalDate,
            isAgent: Boolean)(implicit dateService: DateServiceInterface): IncomeSourceCeasedObligationsViewModel = {

    val flattenQuarterlyObligations = obligationsViewModel.quarterlyObligationsDates.flatten
    val allObligations = flattenQuarterlyObligations ++ obligationsViewModel.finalDeclarationDates
    val numberOfOverdueObligationCount = allObligations.count(_.inboundCorrespondenceDue isBefore dateService.getCurrentDate)

    val viewFinalDeclarationDates = if (obligationsViewModel.quarterlyObligationsDates.flatten.isEmpty)
      obligationsViewModel.finalDeclarationDates.take(2)
    else obligationsViewModel.finalDeclarationDates.take(1)

    val previousYearStart = dateService.getCurrentTaxYearStart.minusYears(1)

    val insetWarningMessageKey: Option[(String, String)] = (flattenQuarterlyObligations.isEmpty, cessationDate isBefore previousYearStart, numberOfOverdueObligationCount) match {
      case (_, true, count) if count > 1 => Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.previous-year.text"))
      case (_, true, count) if count == 1 => Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.previous-year.text"))
      case (true, _, count) if count > 1 => Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.annually.text"))
      case (true, _, count) if count == 1 => Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.annually.text"))
      case (false, _, count) if count > 1 => Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.quarterly.multiple.text"))
      case (false, _, count) if count == 1 => Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.quarterly.single.text"))
      case _ => None
    }

    val viewUpcomingUpdatesLinkMessageKey = if (numberOfOverdueObligationCount == 0) "business-ceased.obligation.view-updates.text" else "business-ceased.obligation.view-updates-overdue.text"


    IncomeSourceCeasedObligationsViewModel(incomeSourceType = incomeSourceType,
      firstQuarterlyUpdate = obligationsViewModel.quarterlyObligationsDates.flatten.headOption,
      finalDeclarationUpdate = viewFinalDeclarationDates,
      numberOfOverdueObligationCount = numberOfOverdueObligationCount,
      viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkMessageKey,
      businessName = businessName,
      insetWarningMessageKey = insetWarningMessageKey,
      currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd),
      isAgent = isAgent)
  }
}