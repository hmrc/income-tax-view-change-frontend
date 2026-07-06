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

package returns.services

import common.config.featureswitch.FeatureSwitching
import common.models.obligations.{GroupedObligationsModel, ObligationsErrorModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import common.testUtils.TestSupport
import returns.mocks.connectors.MockObligationsConnector
import shared.testConstants.NextUpdatesTestConstants.obligationsDataSuccessModel as _
import play.api.http.Status.INTERNAL_SERVER_ERROR

import java.time.LocalDate

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: SingleObligationModel = SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled)

  def currentObligation(date: LocalDate): SingleObligationModel = SingleObligationModel(date, date, date, "Quarterly", None, "#001", StatusFulfilled)

  val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
  val crystallisationDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)
  
  
  "The NextUpdatesService.getNextUpdates method" when {
    "it receives a fromDate and toDate" should {
      "valid current and previous obligations are returned" should {
        "return all obligations" in {

          setupMockAllObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(ObligationsModel(Seq(
            GroupedObligationsModel("idOne", List(previousObligation))
          )))

          val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            GroupedObligationsModel("idOne", List(previousObligation))
          ))
        }
      }

      "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockAllObligationsWithDates(
          from = fixedDate.minusDays(1),
          to = fixedDate.plusDays(1)
        )(ObligationsModel(Seq(
          GroupedObligationsModel("idOne", List(previousObligation))
        )))
        setupMockNextUpdates(ObligationsModel(Seq(
          GroupedObligationsModel("idTwo", List(currentObligation(fixedDate.plusDays(3))))
        )))

        val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
          fromDate = fixedDate.minusDays(1),
          toDate = fixedDate.plusDays(1)
        ).futureValue

        result shouldBe ObligationsModel(Seq(
          GroupedObligationsModel("idOne", List(previousObligation))
        ))
      }

      "return an error" when {
        "an error is returned from  getAllObligations" in {
          setupMockAllObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(ObligationsErrorModel(INTERNAL_SERVER_ERROR, "not found"))

          val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsErrorModel(INTERNAL_SERVER_ERROR, "not found")
        }
      }
    }
  }
}
