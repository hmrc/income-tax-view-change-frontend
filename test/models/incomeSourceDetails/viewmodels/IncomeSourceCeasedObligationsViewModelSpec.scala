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

import enums.IncomeSourceJourney.SelfEmployment
import org.mockito.Mockito.{mock, when}
import play.api.mvc.Call
import services.DateService
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesFull
import testUtils.UnitSpec

import java.time.LocalDate

class IncomeSourceCeasedObligationsViewModelSpec extends UnitSpec {

  val day: LocalDate = LocalDate.of(2022, 1, 1)
  val eopsDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false, obligationType = "EOPS")
  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")
  val finalDeclarationDatesSeq: Seq[DatesModel] = Seq(finalDeclarationDates, finalDeclarationDates, finalDeclarationDates, finalDeclarationDates)
  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(eopsDates),
    finalDeclarationDatesSeq,
    2023,
    showPrevTaxYears = true
  )

  val mockDateService: DateService = mock(classOf[DateService])
  val viewAllBusinessLink: Call = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent = false)
  val viewUpcomingUpdatesLink: Call = controllers.routes.NextUpdatesController.getNextUpdates()

  "IncomeSourceCeasedObligationsViewModel apply" when {
    "provided with Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel" in {
        val currentDate = day.plusYears(3)
        val numberOfOverdueObligationCount = (quarterlyObligationDatesFull.flatten ++ finalDeclarationDatesSeq)
          .count(_.inboundCorrespondenceDue isBefore currentDate)
        when(mockDateService.getCurrentDate).thenReturn(currentDate)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = quarterlyObligationDatesFull.flatten.headOption,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = numberOfOverdueObligationCount,
          viewAllBusinessLink = viewAllBusinessLink,
          viewUpcomingUpdatesLink = viewUpcomingUpdatesLink,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithAllData,
          incomeSourceType = SelfEmployment,
          businessName = None, isAgent = false
        )(mockDateService)


        actual shouldBe expected
      }
    }

    "provided with no Quarterly Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel with two final declaration dates" in {
        val currentDate = day.plusYears(3)
        val numberOfOverdueObligationCount = finalDeclarationDatesSeq.count(_.inboundCorrespondenceDue isBefore currentDate)
        when(mockDateService.getCurrentDate).thenReturn(currentDate)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(2),
          numberOfOverdueObligationCount = numberOfOverdueObligationCount,
          viewAllBusinessLink = viewAllBusinessLink,
          viewUpcomingUpdatesLink = viewUpcomingUpdatesLink,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(
          obligationsViewModel = viewModelWithAllData.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          businessName = None, isAgent = false
        )(mockDateService)


        actual shouldBe expected
      }
    }
  }

}
