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

package models.penalties.breathingSpace

import play.api.libs.json.{JsObject, Json}
import testConstants.PenaltiesTestConstants.breathingSpace
import testUtils.TestSupport

class BreathingSpaceSpec extends TestSupport {

  val breathingSpaceJson: JsObject = Json.obj(
    "BSStartDate" -> "2020-04-06",
    "BSEndDate" -> "2020-12-30"
  )

  "BreathingSpace" should {
    "successfully write to JSON" in {
      val result = Json.toJson(breathingSpace)(BreathingSpace.format)
      result shouldBe breathingSpaceJson
    }

    "successfully read from JSON" in {
      val result = breathingSpaceJson.as[BreathingSpace](BreathingSpace.format)
      result shouldBe breathingSpace
    }
  }
}
