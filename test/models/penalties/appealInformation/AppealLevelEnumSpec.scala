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

package models.penalties.appealInformation

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import testUtils.TestSupport

class AppealLevelEnumSpec extends TestSupport {

  "AppealLevelEnum" should {

    "be writeable to JSON for appeal level '01'" in {
      val result = Json.toJson(AppealLevelEnum.HMRC)(AppealLevelEnum.format)
      result shouldBe JsString("01")
    }

    "be writeable to JSON for appeal level '02'" in {
      val result = Json.toJson(AppealLevelEnum.Tribunal)(AppealLevelEnum.format)
      result shouldBe JsString("02")
    }

    "be readable from JSON for appeal level '01'" in {
      val result = Json.fromJson(JsString("01"))(AppealLevelEnum.format)
      result shouldBe JsSuccess(AppealLevelEnum.HMRC)
    }

    "be readable from JSON for appeal level '02'" in {
      val result = Json.fromJson(JsString("02"))(AppealLevelEnum.format)
      result shouldBe JsSuccess(AppealLevelEnum.Tribunal)
    }

    "throw a JsError when unable to read the JSON returned" in {
      val result = Json.fromJson(JsString("10"))(AppealLevelEnum.format)
      result shouldBe JsError("10 not recognised")
    }
  }

}
