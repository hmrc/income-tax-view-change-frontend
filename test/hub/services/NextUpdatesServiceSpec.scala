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

package hub.services

import common.config.featureswitch.FeatureSwitching
import common.testUtils.TestSupport
import obligations.mocks.connectors.MockObligationsConnector
import obligations.testConstants.BusinessDetailsTestConstants.obligationsDataSuccessModel as _
import obligations.testConstants.NextUpdatesTestConstants.*
import shared.models.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled, StatusOpen}

import java.time.LocalDate

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: SingleObligationModel = SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled)

  def currentObligation(date: LocalDate): SingleObligationModel = SingleObligationModel(date, date, date, "Quarterly", None, "#001", StatusFulfilled)

  val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
  val crystallisationDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

  "getDueDates" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockNextUpdates(obligationsAllDeadlinesSuccessModel)
        getDueDates().futureValue shouldBe
          Right(Seq(LocalDate.of(2017, 10, 30),
            LocalDate.of(2017, 10, 31), LocalDate.of(2017, 10, 1),
            LocalDate.of(2017, 10, 31), LocalDate.of(2017, 10, 31)))
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockNextUpdates(obligationsPropertyOnlySuccessModel)
        getDueDates().futureValue shouldBe
          Right(Seq(LocalDate.of(2017, 10, 1), LocalDate.of(2017, 10, 31)))
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockNextUpdates(obligationsCrystallisedOnlySuccessModel)
        getDueDates().futureValue shouldBe Right(Seq(LocalDate.of(2017, 10, 31)))
      }

      "there are no deadlines available" in new Setup {
        setupMockNextUpdates(emptyObligationsSuccessModel)
        val result: Either[Exception, Seq[LocalDate]] = getDueDates().futureValue
        result.isRight shouldBe true
        result shouldBe Right(Seq.empty)
      }

      "the ObligationsModel contains only empty obligations" in new Setup {
        setupMockNextUpdates(obligationsSuccessModelFiltered)
        val result: Either[Exception, Seq[LocalDate]] = getDueDates().futureValue
        result.isRight shouldBe true
        result shouldBe Right(Seq.empty)
      }


      "the Next Updates returned back an error model" in new Setup {
        setupMockNextUpdates(obligationsDataErrorModel)
        val result: Either[Exception, Seq[LocalDate]] = getDueDates().futureValue
        result.isLeft shouldBe true
        result.left.map(_.getMessage) shouldBe Left("Dummy Error Message")
      }
    }
    "return None" when {
      "404 response from getNextUpdates" in new Setup {
        setupMockNextUpdates(ObligationsModel(Seq.empty))
        getDueDates().futureValue shouldBe Right(Seq())
      }
    }
  }

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

  "getNextDueDates" should {
    "return the earliest quarterly due date and next tax return due date" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(3), "Quarterly", None, "#001", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "Quarterly", None, "#002", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, crystallisationDueDate, "Crystallisation", None, "#003", StatusOpen)
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe Some(fixedDate.plusDays(1))

      nextTaxReturnDueDate shouldBe Some(crystallisationDueDate)
    }

    "return the earliest quarterly due date and fallback static next tax return due date when Crystallisation is not present" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(3), "Quarterly", None, "#001", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "Quarterly", None, "#002", StatusOpen),
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe Some(fixedDate.plusDays(1))

      val staticNextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

      nextTaxReturnDueDate shouldBe Some(staticNextTaxReturnDueDate)
    }

    "return None when no quarterly obligations exist" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, crystallisationDueDate, "Crystallisation", None, "#003", StatusOpen)
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe None

      nextTaxReturnDueDate shouldBe Some(crystallisationDueDate)
    }
    "return an error when the obligations service returns an error model" in new Setup {
      setupMockNextUpdates(obligationsDataErrorModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe None

      val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
      val expectedNextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

      nextTaxReturnDueDate shouldBe Some(expectedNextTaxReturnDueDate)
    }
  }
}
