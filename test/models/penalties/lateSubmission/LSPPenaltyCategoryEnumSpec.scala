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

package models.penalties.lateSubmission

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import testUtils.TestSupport

class LSPPenaltyCategoryEnumSpec extends TestSupport {

  "LSPPenaltyCategoryEnum" should {

    "be writeable to JSON for an LSP penalty category of 'P'" in {
      val result = Json.toJson(Point)(LSPPenaltyCategoryEnum.writes.writes(_))
      result shouldBe JsString("P")
    }

    "be writeable to JSON for an LSP penalty category of 'T'" in {
      val result = Json.toJson(Threshold)(LSPPenaltyCategoryEnum.writes.writes(_))
      result shouldBe JsString("T")
    }

    "be writeable to JSON for an LSP penalty category of 'C'" in {
      val result = Json.toJson(Charge)(LSPPenaltyCategoryEnum.writes.writes(_))
      result shouldBe JsString("C")
    }

    "be readable from JSON for an LSP penalty category of 'P'" in {
      val result = Json.fromJson(JsString("P"))(LSPPenaltyCategoryEnum.format)
      result shouldBe JsSuccess(Point)
    }

    "be readable from JSON for an LSP penalty category of 'T'" in {
      val result = Json.fromJson(JsString("T"))(LSPPenaltyCategoryEnum.format)
      result shouldBe JsSuccess(Threshold)
    }

    "be readable from JSON for an LSP penalty category of 'C'" in {
      val result = Json.fromJson(JsString("C"))(LSPPenaltyCategoryEnum.format)
      result shouldBe JsSuccess(Charge)
    }

    "throw a JsError when the JSON returned is unrecognised" in {
      val result = Json.fromJson(JsString("String"))(LSPPenaltyCategoryEnum.format)
      result shouldBe JsError("STRING not recognised as a LSP penalty category")
    }
  }
}
