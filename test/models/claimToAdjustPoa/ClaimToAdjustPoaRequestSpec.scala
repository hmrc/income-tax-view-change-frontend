/*
 * Copyright 2024 HM Revenue & Customs
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

package models.claimToAdjustPoa

import play.api.libs.json.{JsNumber, JsString, Json, __}
import testUtils.UnitSpec

class ClaimToAdjustPoaRequestSpec extends UnitSpec {

  val request = ClaimToAdjustPoaRequest(
    "AA0000A",
    "2024",
    1000.015,
    MainIncomeLower
  )

  "ClaimToAdjustPoaRequest" when {
    "serialised" should {

      val request = ClaimToAdjustPoaRequest(
        "AA00000A",
        "2024",
        1000.015,
        MainIncomeLower
      )

      val json = Json.toJson(request)

      "round amount up value to two decimal places" in {
        (__ \ "amount")(json) shouldBe Seq(JsNumber(1000.02))
      }

      "write adjustment reason as code" in {
        (__ \ "poaAdjustmentReason")(json) shouldBe Seq(JsString("001"))
      }

      "write increase adjustment reason as code" in {
        val increaseRequest = request.copy(poaAdjustmentReason = Increase)
        val increaseJson    = Json.toJson(increaseRequest)
        (__ \ "poaAdjustmentReason")(increaseJson) shouldBe Seq(JsString("005"))
      }
    }
  }
}
