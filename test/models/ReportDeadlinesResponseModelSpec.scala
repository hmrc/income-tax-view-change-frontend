/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.ReportDeadlinesTestConstants._
import assets.{BaseTestConstants, ReportDeadlinesTestConstants}
import implicits.ImplicitDateFormatter
import models.reportDeadlines._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.test.UnitSpec

import javax.inject.Inject

class ReportDeadlinesResponseModelSpec @Inject()(val languageUtils: LanguageUtils) extends UnitSpec with Matchers with ImplicitDateFormatter {


  "The ReportDeadlinesModel" should {

    "for the 1st Obligation" should {

      val obligation = reportDeadlinesDataSelfEmploymentSuccessModel.obligations.head

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
        obligation.getReportDeadlineStatus shouldBe Overdue("2017-10-30")
      }

      "have the obligation type 'Quarterly'" in {
        obligation.obligationType shouldBe "Quarterly"
      }
    }

    "for the 2nd Obligation" should {

      val obligation = reportDeadlinesDataSelfEmploymentSuccessModel.obligations(1)

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
        obligation.getReportDeadlineStatus shouldBe Open("2017-10-31")
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
        obligation.getReportDeadlineStatus shouldBe Overdue("2017-10-1")
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
        obligation.getReportDeadlineStatus shouldBe Open("2017-10-31")
      }

      "have the obligation type 'Eops'" in {
        obligation.obligationType shouldBe "EOPS"
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ReportDeadlinesModel](reportDeadlinesDataSelfEmploymentSuccessModel) shouldBe obligationsDataSuccessJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[ReportDeadlinesModel](obligationsDataSuccessJson).fold(
        invalid => invalid,
        valid => valid) shouldBe reportDeadlinesDataSelfEmploymentSuccessModel
    }

  }

  "The ReportDeadlinesErrorModel" should {

    "have the correct status code in the model" in {
      obligationsDataErrorModel.code shouldBe testErrorStatus
    }

    "have the correct Error Message in the model" in {
      obligationsDataErrorModel.message shouldBe testErrorMessage
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ReportDeadlinesErrorModel](obligationsDataErrorModel) shouldBe obligationsDataErrorJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[ReportDeadlinesErrorModel](obligationsDataErrorJson) shouldBe JsSuccess(obligationsDataErrorModel)
    }
  }

  "The ObligationsModel" should {

    "return a list of all models with source in date order" when {

      "calling .allDeadlinesWithSource" in {
        ReportDeadlinesTestConstants.obligationsAllDeadlinesSuccessModel.allDeadlinesWithSource()(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ReportDeadlineModelWithIncomeType("Property", overdueEOPSObligation),
          ReportDeadlineModelWithIncomeType("business", overdueObligation),
          ReportDeadlineModelWithIncomeType("business", openObligation),
          ReportDeadlineModelWithIncomeType("Property", openEOPSObligation),
          ReportDeadlineModelWithIncomeType("Crystallised", crystallisedObligation)
        )
      }
    }

    "return a list of only specific updates with source in date order" when {

      "calling .allQuarterly" in {
        ReportDeadlinesTestConstants.obligationsAllDeadlinesSuccessModel.allQuarterly(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ReportDeadlineModelWithIncomeType("business", overdueObligation),
          ReportDeadlineModelWithIncomeType("business", openObligation)
        )
      }

      "calling .allEops" in {
        ReportDeadlinesTestConstants.obligationsAllDeadlinesSuccessModel.allEops(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ReportDeadlineModelWithIncomeType("Property", overdueEOPSObligation),
          ReportDeadlineModelWithIncomeType("Property", openEOPSObligation)
        )
      }

      "calling .allCrystallised" in {
        ReportDeadlinesTestConstants.obligationsAllDeadlinesSuccessModel.allCrystallised(
          BaseTestConstants.testMtdItUser) shouldBe List(
          ReportDeadlineModelWithIncomeType("Crystallised", crystallisedObligation)
        )
      }
    }
  }
}
