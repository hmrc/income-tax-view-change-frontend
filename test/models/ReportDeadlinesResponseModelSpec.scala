/*
 * Copyright 2018 HM Revenue & Customs
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
import models.reportDeadlines._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ReportDeadlinesResponseModelSpec extends UnitSpec with Matchers{

  "The ReportDeadlinesModel" should {

    "for the 1st Obligation" should {

      val obligation = obligationsDataSuccessModel.obligations.head

      "have the start date as 1st April 2017" in {
        obligation.start shouldBe "2017-4-1".toLocalDate
      }

      "have the end date as 30th June 2017" in {
        obligation.end shouldBe "2017-6-30".toLocalDate
      }

      "have the due date as 31st July 2017" in {
        obligation.due shouldBe "2017-7-31".toLocalDate
      }

      "have the obligation met status as 'true'" in {
        obligation.met shouldBe true
      }

      "return 'Received' with getObligationStatus" in {
        obligation.getReportDeadlineStatus shouldBe Received
      }

      "have the obligation type 'QuarterlyObligation'" in {
        obligation.obligationType shouldBe QuarterlyObligation
      }
    }

    "for the 2nd Obligation" should {

      val obligation = obligationsDataSuccessModel.obligations(1)

      "have the start date as 1st July 2017" in {
        obligation.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 30th October 2017" in {
        obligation.due shouldBe "2017-10-30".toLocalDate
      }

      "have the obligation met status as 'false'" in {
        obligation.met shouldBe false
      }

      "return 'Overdue' with getObligationStatus" in {
        obligation.getReportDeadlineStatus shouldBe Overdue
      }

      "have the obligation type 'QuarterlyObligation'" in {
        obligation.obligationType shouldBe QuarterlyObligation
      }
    }

    "for the 3rd Obligation" should {

      val obligation = obligationsDataSuccessModel.obligations(2)

      "have the start date as 1st July 2017" in {
        obligation.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 31st October 2017" in {
        obligation.due shouldBe "2017-10-31".toLocalDate
      }

      "have the obligation met status as 'false'" in {
        obligation.met shouldBe false
      }

      "return 'Open' with getObligationStatus" in {
        obligation.getReportDeadlineStatus shouldBe Open("2017-10-31".toLocalDate)
      }

      "have the obligation type 'QuarterlyObligation'" in {
        obligation.obligationType shouldBe QuarterlyObligation
      }
    }

    "for an EOPS Obligation" should {

      val obligation = obligationsEOPSDataSuccessModel.obligations.head

      "have the start date as 6th April 2017" in {
        obligation.start shouldBe "2017-4-6".toLocalDate
      }

      "have the end date as 5th April 2018" in {
        obligation.end shouldBe "2018-4-5".toLocalDate
      }

      "have the due date as 1st May 2018" in {
        obligation.due shouldBe "2018-5-1".toLocalDate
      }

      "have the obligation met status as 'true'" in {
        obligation.met shouldBe true
      }

      "return 'Open' with getObligationStatus" in {
        obligation.getReportDeadlineStatus shouldBe Received
      }

      "have the obligation type 'EopsObligation'" in {
        obligation.obligationType shouldBe EopsObligation
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ReportDeadlinesModel](obligationsDataSuccessModel) shouldBe obligationsDataSuccessJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[ReportDeadlinesModel](obligationsDataSuccessJson).fold(
        invalid => invalid,
        valid => valid) shouldBe obligationsDataSuccessModel
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

  "The ReportDeadlinesResponseModel .apply function" should {

    "create a ReportDeadlines Model when passed the correct Json" in {
      ReportDeadlinesResponseModel.apply("ReportDeadlinesModel", obligationsDataSuccessJson) shouldBe obligationsDataSuccessModel
    }

    "create a ReportDeadlinesErrorModel when passed the correct Json" in {
      ReportDeadlinesResponseModel.apply("ReportDeadlinesErrorModel", obligationsDataErrorJson) shouldBe obligationsDataErrorModel
    }

    "create a ReportDeadlinesModel when passed the correct Json" in {
      ReportDeadlinesResponseModel.apply("ReportDeadlineModel", reportDeadlineReceivedJson) shouldBe receivedObligation
    }
  }

  "The ReportDeadlinesResponseModel .unapply function" should {

    "extract the ReportDeadlinesModel and Json" in {
      ReportDeadlinesResponseModel.unapply(obligationsDataSuccessModel) shouldBe Some(("ReportDeadlinesModel", obligationsDataSuccessJson))
    }
    "extract the ReportDeadlinesErrorModel and Json" in {
      ReportDeadlinesResponseModel.unapply(obligationsDataErrorModel) shouldBe Some(("ReportDeadlinesErrorModel", obligationsDataErrorJson))
    }
    "extract the ReportDeadlineModel and Json" in {
      ReportDeadlinesResponseModel.unapply(receivedObligation) shouldBe Some(("ReportDeadlineModel", reportDeadlineReceivedJson))
    }
  }
}