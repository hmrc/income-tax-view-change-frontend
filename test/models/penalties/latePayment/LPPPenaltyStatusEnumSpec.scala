/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.latePayment

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import testUtils.TestSupport

class LPPPenaltyStatusEnumSpec extends TestSupport {

  "LPPPenaltyStatusEnum" should {

    "be writeable to JSON for LPP penalty status 'A'" in {
      val result = Json.toJson(AccruingLPPPenaltyStatus)(LPPPenaltyStatusEnum.writes.writes(_))
      result shouldBe JsString("A")
    }

    "be writeable to JSON for LPP penalty status 'P'" in {
      val result = Json.toJson(PostedLPPPenaltyStatus)(LPPPenaltyStatusEnum.writes.writes(_))
      result shouldBe JsString("P")
    }

    "be readable from JSON for LPP penalty status 'A'" in {
      val result = Json.fromJson(JsString("A"))(LPPPenaltyStatusEnum.format)
      result shouldBe JsSuccess(AccruingLPPPenaltyStatus)
    }

    "be readable from JSON for LPP penalty status 'P'" in {
      val result = Json.fromJson(JsString("P"))(LPPPenaltyStatusEnum.format)
      result shouldBe JsSuccess(PostedLPPPenaltyStatus)
    }

    "throw an error when the LPP penalty status is unrecognised" in {
      val result = Json.fromJson(JsString("100"))(LPPPenaltyStatusEnum.format)
      result shouldBe JsError("100 not recognised as a LPP penalty status")
    }
  }

}
