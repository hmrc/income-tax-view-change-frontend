/*
 * Copyright 2026 HM Revenue & Customs
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

package models.newHomePage

import obligations.models.{SingleObligationModel, StatusOpen}
import testUtils.UnitSpec

import java.time.LocalDate

class SubmissionDeadlinesViewModelSpec extends UnitSpec {

  val currentDate: LocalDate = LocalDate.now

  private val annualObligationType: String = "Crystallisation"
  private val quarterlyObligationType: String = "Quarterly"

  private def getSingleObligationModels(dueDate: LocalDate = currentDate.plusDays(31), obligationType: String = annualObligationType): Seq[SingleObligationModel] =
    Seq(SingleObligationModel(currentDate.minusMonths(6), currentDate.minusMonths(3), dueDate, obligationType, None, "#002", StatusOpen))

  val multipleAnnualOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(dueDate = currentDate.minusDays(90)) ++
    getSingleObligationModels(dueDate = currentDate.minusDays(120)) ++
    getSingleObligationModels(dueDate = currentDate.minusDays(20))

  val multipleQuarterlyOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(currentDate.minusDays(90), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = currentDate.minusDays(120), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = currentDate.minusDays(20), quarterlyObligationType)

  val twoAnnualOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(currentDate.minusYears(1), annualObligationType) ++
    getSingleObligationModels(dueDate = currentDate.minusYears(2), annualObligationType)

  val twoQuarterlyOverdueObligations: Seq[SingleObligationModel] =
    twoAnnualOverdueObligations.map(_.copy(obligationType = quarterlyObligationType))

  "SubmissionDeadlinesViewModel model" when {
    "getNumberOfOverdueAnnualObligations called with no annual overdue obligations" should {
      "return 0 value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = annualObligationType), currentDate, None, None)

        testModel.getNumberOfOverdueAnnualObligations shouldBe 0
      }
    }

    "getNumberOfOverdueAnnualObligations called with one annual overdue obligations" should {
      "return 1 value" in {
        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(dueDate = currentDate.minusDays(1), obligationType = annualObligationType), currentDate, None, None)

        testModel.getNumberOfOverdueAnnualObligations shouldBe 1
      }
    }

    "getNumberOfOverdueAnnualObligations called with two annual overdue obligations" should {
      "return 2 value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(twoAnnualOverdueObligations, currentDate, None, None)

        testModel.getNumberOfOverdueAnnualObligations shouldBe 2
      }
    }

    "getNumberOfOverdueQuarterlyObligations called with no quarterly overdue obligations" should {
      "return 0 value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = quarterlyObligationType), currentDate, None, None)

        testModel.getNumberOfOverdueQuarterlyObligations shouldBe 0
      }
    }

    "getNumberOfOverdueQuarterlyObligations called with one quarterly overdue obligations" should {
      "return 1 value" in {
        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(dueDate = currentDate.minusDays(1), obligationType = quarterlyObligationType), currentDate, None, None)

        testModel.getNumberOfOverdueQuarterlyObligations shouldBe 1
      }
    }

    "getNumberOfOverdueQuarterlyObligations called with two quarterly overdue obligations" should {
      "return 2 value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(twoQuarterlyOverdueObligations, currentDate, None, None)

        testModel.getNumberOfOverdueQuarterlyObligations shouldBe 2
      }
    }

    "getOldestAnnualOverdueDate called with no obligations" should {
      "return None" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(Seq.empty[SingleObligationModel], currentDate, None, None)

        testModel.getOldestAnnualOverdueDate shouldBe None
      }
    }

    "getOldestAnnualOverdueDate called with no annual but two quarterly overdue obligations" should {
      "return None" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(twoQuarterlyOverdueObligations, currentDate, None, None)

        testModel.getOldestAnnualOverdueDate shouldBe None
      }
    }

    "getOldestAnnualOverdueDate called with two quarterly overdue obligations" should {
      "return proper value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(multipleAnnualOverdueObligations, currentDate, None, None)

        testModel.getOldestAnnualOverdueDate shouldBe Some(currentDate.minusDays(120))
      }
    }

    "getOldestQuarterlyOverdueDate called with no obligations" should {
      "return None" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(Seq.empty[SingleObligationModel], currentDate, None, None)

        testModel.getOldestQuarterlyOverdueDate shouldBe None
      }
    }

    "getOldestQuarterlyOverdueDate called with no quarterly but two annual overdue obligations" should {
      "return None" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(twoAnnualOverdueObligations, currentDate, None, None)

        testModel.getOldestQuarterlyOverdueDate shouldBe None
      }
    }

    "getOldestQuarterlyOverdueDate called with two quarterly overdue obligations" should {
      "return proper value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(multipleQuarterlyOverdueObligations, currentDate, None, None)

        testModel.getOldestQuarterlyOverdueDate shouldBe Some(currentDate.minusDays(120))
      }
    }

    "isAnnualObligations called with no annual obligations" should {
      "return false value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = quarterlyObligationType), currentDate, None, None)

        testModel.isAnnualObligations shouldBe false
      }
    }

    "isAnnualObligations called with annual obligation" should {
      "return true value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = annualObligationType), currentDate, None, None)

        testModel.isAnnualObligations shouldBe true
      }
    }

    "isQuarterlyObligations called with no quarterly obligations" should {
      "return false value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = annualObligationType), currentDate, None, None)

        testModel.isQuarterlyObligations shouldBe false
      }
    }

    "isQuarterlyObligations called with quarterly obligation" should {
      "return true value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(getSingleObligationModels(obligationType = quarterlyObligationType), currentDate, None, None)

        testModel.isQuarterlyObligations shouldBe true
      }
    }

    "showNextUpdatesTileContent called with no obligations" should {
      "return false value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(Seq.empty[SingleObligationModel], currentDate, None, None)

        testModel.hasOpenObligations shouldBe false
      }
    }

    "showNextUpdatesTileContent called with obligations" should {
      "return true value" in {

        val testModel: SubmissionDeadlinesViewModel =
          SubmissionDeadlinesViewModel(multipleAnnualOverdueObligations, currentDate, None, None)

        testModel.hasOpenObligations shouldBe true
      }
    }
  }
}
