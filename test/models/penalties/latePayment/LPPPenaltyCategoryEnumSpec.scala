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

class LPPPenaltyCategoryEnumSpec extends TestSupport {

  "LPPPenaltyCategoryEnum" should {

    "be writeable to JSON for LPP category level 'LPP1'" in {
      val result = Json.toJson(LPPPenaltyCategoryEnum.FirstPenalty)(LPPPenaltyCategoryEnum.format)
      result shouldBe JsString("LPP1")
    }

    "be writeable to JSON for LPP category level 'LPP2'" in {
      val result = Json.toJson(LPPPenaltyCategoryEnum.SecondPenalty)(LPPPenaltyCategoryEnum.format)
      result shouldBe JsString("LPP2")
    }

    "be readable from JSON for LPP category level 'LPP1'" in {
      val result = Json.fromJson(JsString("LPP1"))(LPPPenaltyCategoryEnum.format)
      result shouldBe JsSuccess(LPPPenaltyCategoryEnum.FirstPenalty)
    }

    "be readable from JSON for LPP category level 'LPP2'" in {
      val result = Json.fromJson(JsString("LPP2"))(LPPPenaltyCategoryEnum.format)
      result shouldBe JsSuccess(LPPPenaltyCategoryEnum.SecondPenalty)
    }

    "throw a JsError when the JSON returned is unrecognised" in {
      val result = Json.fromJson(JsString("100"))(LPPPenaltyCategoryEnum.format)
      result shouldBe JsError("100 not recognised")
    }
  }

}
