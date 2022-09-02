/*
 * Copyright 2022 HM Revenue & Customs
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
import models.nextUpdates._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.obligationsAllDeadlinesSuccessNotValidObligationType
import testConstants.NextUpdatesTestConstants._
import testConstants.{BaseTestConstants, NextUpdatesTestConstants}
import testUtils.TestSupport

class NextUpdatesResponseModelSpec extends TestSupport with Matchers with ImplicitDateFormatter {


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

      "return 'Overdue' with getObligationStatus" in {
        obligation.getNextUpdateStatus shouldBe Overdue("2017-10-30")
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

      "return 'Open' with getObligationStatus" in {
        obligation.getNextUpdateStatus shouldBe Open("2017-10-31")
      }

      "have the obligation type 'Quarterly'" in {
        obligation.obligationType shouldBe "Quarterly"
      }
    }

    "for the 1st EOPS Obligation" should {

      val obligation = obligationsEOPSDataSuccessModel.obligations.head

      "have the start date as 6th April 2017" in {
        obligation.start shouldBe "2017-4-6".toLocalDate
      }

      "have the end date as 5th April 2018" in {
        obligation.end shouldBe "2018-4-5".toLocalDate
      }

      "have the due date as 1st Oct 2018" in {
        obligation.due shouldBe "2017-10-1".toLocalDate
      }

      "have the periodKey as '#002'" in {
        obligation.periodKey shouldBe "#002"
      }

      "return 'Open' with getObligationStatus" in {
        obligation.getNextUpdateStatus shouldBe Overdue("2017-10-1")
      }

      "have the obligation type 'Eops'" in {
        obligation.obligationType shouldBe "EOPS"
      }
    }

    "for the 2nd EOPS Obligation" should {

      val obligation = obligationsEOPSDataSuccessModel.obligations(1)

      "have the start date as 6th April 2017" in {
        obligation.start shouldBe "2017-4-6".toLocalDate
      }

      "have the end date as 5th April 2018" in {
        obligation.end shouldBe "2018-4-5".toLocalDate
      }

      "have the due date as 31st Oct 2018" in {
        obligation.due shouldBe "2017-10-31".toLocalDate
      }

      "have the periodKey as '#003'" in {
        obligation.periodKey shouldBe "#003"
      }

      "return 'Open' with getObligationStatus" in {
        obligation.getNextUpdateStatus shouldBe Open("2017-10-31")
      }

      "have the obligation type 'Eops'" in {
        obligation.obligationType shouldBe "EOPS"
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[NextUpdatesModel](nextUpdatesDataSelfEmploymentSuccessModel) shouldBe obligationsDataSuccessJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[NextUpdatesModel](obligationsDataSuccessJson).fold(
        invalid => invalid,
        valid => valid) shouldBe nextUpdatesDataSelfEmploymentSuccessModel
    }

    // TODO remove it when done
    "call to obligation.currentTime() should return expected date" in {
      val obligation = nextUpdatesDataSelfEmploymentSuccessModel.obligations.head

      obligation.currentTime() shouldBe mockedCurrentTime20171031
    }

    "call to .currentCrystDeadlines should return sorted obligations by Crystallised obligationType" in {
      val nextUpdatesModel: NextUpdatesModel = NextUpdatesModel(testSelfEmploymentId, List(openObligation, crystallisedObligation, quarterlyBusinessObligation, crystallisedObligationTwo))

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
      Json.toJson[NextUpdatesErrorModel](obligationsDataErrorModel) shouldBe obligationsDataErrorJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[NextUpdatesErrorModel](obligationsDataErrorJson) shouldBe JsSuccess(obligationsDataErrorModel)
    }
  }

  "The ObligationsModel" should {

    "return a list of all models with source in date order" when {

      "calling .allDeadlinesWithSource" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource()(
          BaseTestConstants.testMtdItUser) shouldBe List(
          NextUpdateModelWithIncomeType("nextUpdates.propertyIncome", overdueEOPSObligation),
          NextUpdateModelWithIncomeType("nextUpdates.business", overdueObligation),
          NextUpdateModelWithIncomeType("nextUpdates.business", openObligation),
          NextUpdateModelWithIncomeType("nextUpdates.propertyIncome", openEOPSObligation),
          NextUpdateModelWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation)
        )
      }
    }

    "return a list of all models with source in dateReceived order if the previous flag is set to true" when {

      "calling .allDeadlinesWithSource" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesWithDateReceivedSuccessModel.allDeadlinesWithSource(previous = true)(
          BaseTestConstants.testMtdItUser) shouldBe List(
          NextUpdateModelWithIncomeType("nextUpdates.business", openObligation.copy(dateReceived = Some(mockedCurrentTime20171031.plusDays(1)))),
          NextUpdateModelWithIncomeType("nextUpdates.propertyIncome", overdueEOPSObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(3)))),
          NextUpdateModelWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(6))))
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
          NextUpdateModelWithIncomeType("nextUpdates.business", overdueObligation),
          NextUpdateModelWithIncomeType("nextUpdates.business", openObligation)
        )
      }

      "calling .allEops" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allEops(
          BaseTestConstants.testMtdItUser) shouldBe List(
          NextUpdateModelWithIncomeType("nextUpdates.propertyIncome", overdueEOPSObligation),
          NextUpdateModelWithIncomeType("nextUpdates.propertyIncome", openEOPSObligation)
        )
      }

      "calling .allCrystallised" in {
        NextUpdatesTestConstants.obligationsAllDeadlinesSuccessModel.allCrystallised(
          BaseTestConstants.testMtdItUser) shouldBe List(
          NextUpdateModelWithIncomeType("nextUpdates.crystallisedAll", crystallisedObligation)
        )
      }
    }
  }
}
