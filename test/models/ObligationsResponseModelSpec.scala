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

package models

import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.{QuarterTypeCalendar, QuarterTypeStandard}
import models.itsaStatus.ITSAStatus
import models.obligations._
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.obligationsAllDeadlinesSuccessNotValidObligationType
import testConstants.NextUpdatesTestConstants._
import testConstants.{BaseTestConstants, NextUpdatesTestConstants}
import testUtils.TestSupport

class ObligationsResponseModelSpec extends TestSupport with Matchers with ImplicitDateFormatter {

  "The NextUpdatesModel" should {

    "for the 1st Obligation" should {

      val obligation = nextUpdatesDataSelfEmploymentSuccessModel.obligations.head

      "have the start date as 1st July 2017" in {
        obligation.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 30th October 2017" in {
        obligation.due shouldBe "2017-10-30".toLocalDate
      }

      "have the periodKey as '#002'" in {
        obligation.periodKey shouldBe "#002"
      }

      "have status as fulfilled" in {
        obligation.status shouldBe StatusFulfilled
      }

      "have the obligation type 'Quarterly'" in {
        obligation.obligationType shouldBe "Quarterly"
      }
    }

    "for the 2nd Obligation" should {

      val obligation = nextUpdatesDataSelfEmploymentSuccessModel.obligations(1)

      "have the start date as 1st July 2017" in {
        obligation.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 31st October 2017" in {
        obligation.due shouldBe "2017-10-31".toLocalDate
      }

      "have the periodKey as '#003'" in {
        obligation.periodKey shouldBe "#003"
      }

      "have the obligation status of fulfilled" in {
        obligation.status shouldBe StatusFulfilled
      }

      "have the obligation type 'Quarterly'" in {
        obligation.obligationType shouldBe "Quarterly"
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[GroupedObligationsModel](nextUpdatesDataSelfEmploymentSuccessModel) shouldBe obligationsDataSuccessJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[GroupedObligationsModel](obligationsDataSuccessJson).fold(
        invalid => invalid,
        valid => valid) shouldBe nextUpdatesDataSelfEmploymentSuccessModel
    }

    "call to .currentCrystDeadlines should return sorted obligations by Crystallised obligationType" in {
      val nextUpdatesModel = GroupedObligationsModel(testSelfEmploymentId,
        List(openObligation, crystallisedObligation, quarterlyBusinessObligation, crystallisedObligationTwo))

      nextUpdatesModel.currentCrystDeadlines shouldBe List(crystallisedObligation, crystallisedObligationTwo)
    }

  }

  "The NextUpdatesErrorModel" should {

    "have the correct status code in the model" in {
      obligationsDataErrorModel.code shouldBe testErrorStatus
    }

    "have the correct Error Message in the model" in {
      obligationsDataErrorModel.message shouldBe testErrorMessage
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ObligationsErrorModel](obligationsDataErrorModel) shouldBe obligationsDataErrorJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[ObligationsErrorModel](obligationsDataErrorJson) shouldBe JsSuccess(obligationsDataErrorModel)
    }
  }

  "The ObligationsModel" should {

    "return a list of all models with source in date order" when {

      "calling .allDeadlinesWithSource" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource()(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ObligationWithIncomeType("nextUpdates.propertyIncome", overdueQuarterlyObligation),
          ObligationWithIncomeType("nextUpdates.business", overdueObligation),
          ObligationWithIncomeType("nextUpdates.business", openObligation),
          ObligationWithIncomeType("nextUpdates.propertyIncome", openQuarterlyObligation),
          ObligationWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation)
        )
      }
    }

    "return a list of all models with source in dateReceived order if the previous flag is set to true" when {

      "calling .allDeadlinesWithSource" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesWithDateReceivedSuccessModel.allDeadlinesWithSource(previous = true)(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ObligationWithIncomeType("nextUpdates.business", openObligation.copy(dateReceived = Some(mockedCurrentTime20171031.plusDays(1)))),
          ObligationWithIncomeType("nextUpdates.propertyIncome", overdueQuarterlyObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(3)))),
          ObligationWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(6))))
        )
      }
    }

    "return an empty list" when {

      "calling .allDeadlinesWithSource" in {
        obligationsAllDeadlinesSuccessNotValidObligationType.allDeadlinesWithSource()(
          BaseTestConstants.testMtdItUserNoIncomeSource) shouldBe List()
      }
    }

    "return a list of only specific updates with source in date order" when {

      "calling .allQuarterly" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allQuarterly(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ObligationWithIncomeType("nextUpdates.propertyIncome", overdueQuarterlyObligation),
          ObligationWithIncomeType("nextUpdates.business", overdueObligation),
          ObligationWithIncomeType("nextUpdates.business", openObligation),
          ObligationWithIncomeType("nextUpdates.propertyIncome", openQuarterlyObligation)
        )
      }

      "calling .allCrystallised" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allCrystallised(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ObligationWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation)
        )
      }

      "calling .groupByQuarterPeriod" in {
        val nextUpdateModelWithIncomeTypeList: Seq[ObligationWithIncomeType] = List(
          ObligationWithIncomeType("nextUpdates.propertyIncome", overdueQuarterlyObligation),
          ObligationWithIncomeType("nextUpdates.business", overdueObligation),
          ObligationWithIncomeType("nextUpdates.business", openObligation),
          ObligationWithIncomeType("nextUpdates.propertyIncome", openQuarterlyObligation),
          ObligationWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation)
        )

        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.groupByQuarterPeriod(
          nextUpdateModelWithIncomeTypeList) shouldBe Map(
          None -> List(
            ObligationWithIncomeType("nextUpdates.crystallisedAll", SingleObligationModel("2017-10-01", "2018-10-30", "2017-10-31", "Crystallisation", None, "", StatusFulfilled))),
          Some(QuarterTypeCalendar) -> List(
            ObligationWithIncomeType("nextUpdates.business", SingleObligationModel("2017-07-01", "2017-09-30", "2017-10-30", "Quarterly", None, "#002", StatusFulfilled)),
            ObligationWithIncomeType("nextUpdates.business", SingleObligationModel("2017-07-01", "2017-09-30", "2017-10-31", "Quarterly", None, "#003", StatusFulfilled))),
          Some(QuarterTypeStandard) -> List(
            ObligationWithIncomeType("nextUpdates.propertyIncome", SingleObligationModel("2017-04-06", "2018-04-05", "2017-10-01", "Quarterly", None, "#002", StatusFulfilled)),
            ObligationWithIncomeType("nextUpdates.propertyIncome", SingleObligationModel("2017-04-06", "2018-04-05", "2017-10-31", "Quarterly", None, "#003", StatusFulfilled)))
        )
      }
    }

    "return only quarterly business obligations if R17 is enabled and user is Mandated" in {
      obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource(
        r17ContentEnabled = true,
        currentYearITSAStatus = Some(ITSAStatus.Mandated)
      )(testMtdItUser) shouldBe List(
        ObligationWithIncomeType("nextUpdates.business", overdueObligation),
        ObligationWithIncomeType("nextUpdates.business", openObligation)
      )
    }

    "return only quarterly business obligations if R17 is enabled and user is Voluntary" in {
      obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource(
        r17ContentEnabled = true,
        currentYearITSAStatus = Some(ITSAStatus.Voluntary)
      )(testMtdItUser) shouldBe List(
        ObligationWithIncomeType("nextUpdates.business", overdueObligation),
        ObligationWithIncomeType("nextUpdates.business", openObligation)
      )
    }

    "return empty list if R17 is enabled and user is Annual" in {
      obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource(
        r17ContentEnabled = true,
        currentYearITSAStatus = Some(ITSAStatus.Annual)
      )(testMtdItUser) shouldBe Nil
    }
  }
}
