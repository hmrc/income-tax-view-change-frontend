/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants.Obligations._
import assets.TestConstants._
import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class ObligationsResponseModelSpec extends UnitSpec with Matchers{

  "The ObligationsModel" should {

    "for the 1st Obligation" should {

      val obligation1 = obligationsDataSuccessModel.obligations.head

      "have the start date as 1st April 2017" in {
        obligation1.start shouldBe "2017-4-1".toLocalDate
      }

      "have the end date as 30th June 2017" in {
        obligation1.end shouldBe "2017-6-30".toLocalDate
      }

      "have the due date as 31st July 2017" in {
        obligation1.due shouldBe "2017-7-31".toLocalDate
      }

      "have the obligation met status as 'true'" in {
        obligation1.met shouldBe true
      }

      "return 'Received' with getObligationStatus" in {
        obligation1.getObligationStatus shouldBe Received
      }
    }

    "for the 2nd Obligation" should {

      val obligation2 = obligationsDataSuccessModel.obligations(1)

      "have the start date as 1st July 2017" in {
        obligation2.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation2.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 30th October 2017" in {
        obligation2.due shouldBe "2017-10-30".toLocalDate
      }

      "have the obligation met status as 'false'" in {
        obligation2.met shouldBe false
      }

      "return 'Overdue' with getObligationStatus" in {
        obligation2.getObligationStatus shouldBe Overdue
      }
    }

    "for the 3rd Obligation" should {

      val obligation3 = obligationsDataSuccessModel.obligations(2)

      "have the start date as 1st July 2017" in {
        obligation3.start shouldBe "2017-7-1".toLocalDate
      }

      "have the end date as 30th September 2017" in {
        obligation3.end shouldBe "2017-9-30".toLocalDate
      }

      "have the due date as 31st October 2017" in {
        obligation3.due shouldBe "2017-10-31".toLocalDate
      }

      "have the obligation met status as 'false'" in {
        obligation3.met shouldBe false
      }

      "return 'Open' with getObligationStatus" in {
        obligation3.getObligationStatus shouldBe Open("2017-10-31".toLocalDate)
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ObligationsModel](obligationsDataSuccessModel) shouldBe obligationsDataSuccessJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(obligationsDataSuccessString).as[ObligationsModel] shouldBe obligationsDataSuccessModel
    }
  }

  "The ObligationsErrorModel" should {

    "have the correct status code in the model" in {
      obligationsDataErrorModel.code shouldBe testErrorStatus
    }

    "have the correct Error Message in the model" in {
      obligationsDataErrorModel.message shouldBe testErrorMessage
    }

    "be formatted to JSON correctly" in {
      Json.toJson[ObligationsErrorModel](obligationsDataErrorModel) shouldBe obligationsDataErrorJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(obligationsDataErrorString).as[ObligationsErrorModel] shouldBe obligationsDataErrorModel
    }
  }
}