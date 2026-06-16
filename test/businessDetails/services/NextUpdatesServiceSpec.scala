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

package businessDetails.services

import common.config.featureswitch.FeatureSwitching
import common.testUtils.TestSupport
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import obligations.mocks.connectors.MockObligationsConnector
import obligations.testConstants.BusinessDetailsTestConstants.obligationsDataSuccessModel as _
import obligations.testConstants.NextUpdatesTestConstants.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import shared.models.{GroupedObligationsModel, ObligationsErrorModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate
import scala.concurrent.Future

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: SingleObligationModel = SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled)

  def currentObligation(date: LocalDate): SingleObligationModel = SingleObligationModel(date, date, date, "Quarterly", None, "#001", StatusFulfilled)

  val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
  val crystallisationDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

  "The NextUpdatesService.getOpenObligations method" when {

    "a valid list of Next Updates is returned from the connector" should {

      "return a valid list of Next Updates" in {
        setupMockNextUpdates(ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel)))
        TestNextUpdatesService.getOpenObligations().futureValue shouldBe ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockNextUpdates(obligationsDataErrorModel)
        TestNextUpdatesService.getOpenObligations().futureValue shouldBe obligationsDataErrorModel
      }
    }
  }

  "getObligationDates" should {
    "return the correct set of dates given an ObligationsModel" in {

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Quarterly", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false, obligationType = "Quarterly"))
      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe expectedResult
    }
    "show correct error when given a NextUpdatesErrorModel" in {

      val nextModel: ObligationsErrorModel = ObligationsErrorModel(1, "fail")
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe Seq.empty
    }
  }

  "getObligationsViewModel" should {

    "return a valid view model with quarterly obligations and final declaration(s)" in {

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Quarterly", None, "#001", StatusFulfilled)
        )),
        GroupedObligationsModel("123", List(
          SingleObligationModel(day.minusYears(1), day.minusYears(1).plusDays(1), day.minusYears(1).plusDays(2), "Quarterly", None, "#001", StatusFulfilled)
        )
        ),
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq(
          Seq(
            DatesModel(day.minusYears(1), day.minusYears(1).plusDays(1),
              day.minusYears(1).plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly"),
            DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly")
          )
        ),
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no final declaration" in {

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no quarterly obligations" in {

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)

      result.futureValue shouldBe ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
    }
  }
}
