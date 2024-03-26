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
import play.api.mvc.Call
import services.DateServiceInterface

case class IncomeSourceCeasedObligationsViewModel(incomeSourceType: IncomeSourceType,
                                                  firstQuarterlyUpdate: Option[DatesModel],
                                                  finalDeclarationUpdate: Seq[DatesModel],
                                                  numberOfOverdueObligationCount: Int,
                                                  viewAllBusinessLink: Call,
                                                  viewUpcomingUpdatesLink: Call,
                                                  businessName: Option[String],
                                                  isAgent: Boolean) {

}

object IncomeSourceCeasedObligationsViewModel {
  def apply(obligationsViewModel: ObligationsViewModel,
            incomeSourceType: IncomeSourceType,
            businessName: Option[String],
            isAgent: Boolean)(implicit dateService: DateServiceInterface): IncomeSourceCeasedObligationsViewModel = {

    val allObligations = obligationsViewModel.quarterlyObligationsDates.flatten ++ obligationsViewModel.finalDeclarationDates
    val numberOfOverdueObligationCount = allObligations.count(_.inboundCorrespondenceDue isBefore dateService.getCurrentDate)
    val viewAllBusinessLink = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent)
    val viewUpcomingUpdatesLink = controllers.routes.NextUpdatesController.getNextUpdates()

    val viewFinalDeclarationDates = if (obligationsViewModel.quarterlyObligationsDates.flatten.isEmpty)
      obligationsViewModel.finalDeclarationDates.take(2)
    else obligationsViewModel.finalDeclarationDates.take(1)

    IncomeSourceCeasedObligationsViewModel(incomeSourceType = incomeSourceType,
      firstQuarterlyUpdate = obligationsViewModel.quarterlyObligationsDates.flatten.headOption,
      finalDeclarationUpdate = viewFinalDeclarationDates,
      numberOfOverdueObligationCount = numberOfOverdueObligationCount,
      viewAllBusinessLink = viewAllBusinessLink,
      viewUpcomingUpdatesLink = viewUpcomingUpdatesLink, businessName = businessName, isAgent)
  }
}