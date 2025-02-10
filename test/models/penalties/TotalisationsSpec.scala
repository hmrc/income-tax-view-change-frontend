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

package models.penalties

import play.api.libs.json.{JsObject, Json}
import testConstants.PenaltiesTestConstants.totalisationsModel
import testUtils.TestSupport

class TotalisationsSpec extends TestSupport {

  val totalisationsJson: JsObject = Json.obj(
    "LSPTotalValue" -> Some(200),
    "penalisedPrincipalTotal" -> Some(2000),
    "LPPPostedTotal" -> Some(165.25),
    "LPPEstimatedTotal" -> Some(15.26)
  )

  "Totalisations model" should {
    "be able to write to JSON" in {
      val result = Json.toJson(totalisationsModel)(Totalisations.format)
      result shouldBe totalisationsJson
    }

    "read from JSON" in {
      val result = totalisationsJson.as[Totalisations](Totalisations.format)
      result shouldBe totalisationsModel
    }
  }

}
