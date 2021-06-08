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

import assets.ChargeHistoryTestConstants._
import models.chargeHistory.ChargesHistoryModel
import org.scalatest.Matchers
import play.api.libs.json.Json
import play.api.test.Helpers.{BAD_REQUEST, NOT_FOUND}
import uk.gov.hmrc.play.test.UnitSpec

class ChargeHistoryResponseModelSpec extends UnitSpec with Matchers {

  "The ChargeHistoryModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[ChargesHistoryModel](testValidChargeHistoryModel) shouldBe testValidChargeHistoryDetailsModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[ChargesHistoryModel](testValidChargeHistoryDetailsModelJson).fold(
        invalid => invalid,
        valid => valid) shouldBe testValidChargeHistoryModel
    }

  }

  "The predicate ChargeHistoryErrorModel.isNoDataFoundError which" should {
    "return true" when {
      "there is a response status 404 and a single API error with code NO_DATA_FOUND" in {
        testChargesHistoryErrorModelNoDataFound
          .isNoDataFoundError shouldBe true
      }
    }

    "return false" when {
      "there is a non-404 response status and a single API error with code NO_DATA_FOUND" in {
        testChargesHistoryErrorModelNoDataFound.copy(status = BAD_REQUEST)
          .isNoDataFoundError shouldBe false
      }

      "there is a response status 404 and multiple API errors" in {
        testChargesHistoryErrorModelBadRequestFailures.copy(status = NOT_FOUND)
          .isNoDataFoundError shouldBe false
      }

      "there is a non-404 response status and multiple API errors" in {
        testChargesHistoryErrorModelBadRequestFailures
          .isNoDataFoundError shouldBe false
      }
    }
  }
}
