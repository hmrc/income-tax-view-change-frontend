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

package common.models.obligations

import common.implicits.ImplicitDateFormatter
import common.testConstants.BaseTestConstants
import common.testConstants.BaseTestConstants.*
import common.testUtils.TestSupport
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}
import shared.testConstants.NextUpdatesTestConstants
import shared.testConstants.NextUpdatesTestConstants.*

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

}
