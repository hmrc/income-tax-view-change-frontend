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

package hub.models.penalties.appealInformation

import common.testUtils.TestSupport
import hub.models.penalties.appealInformation.AppealLevelEnum.{HmrcAppealLevel, ThirdAppealLevel, TribunalAppealLevel}
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class AppealLevelEnumSpec extends TestSupport {

  "AppealLevelEnum" should {

    "be writeable to JSON for appeal level '01'" in {
      val result = Json.toJson(HmrcAppealLevel)(AppealLevelEnum.writes.writes(_))
      result shouldBe JsString("01")
    }

    "be writeable to JSON for appeal level '02'" in {
      val result = Json.toJson(TribunalAppealLevel)(AppealLevelEnum.writes.writes(_))
      result shouldBe JsString("02")
    }

    "be writeable to JSON for appeal level '03'" in {
      val result = Json.toJson(ThirdAppealLevel)(AppealLevelEnum.writes.writes(_))
      result shouldBe JsString("03")
    }

    "be readable from JSON for appeal level '01'" in {
      val result = Json.fromJson(JsString("01"))(AppealLevelEnum.format)
      result shouldBe JsSuccess(HmrcAppealLevel)
    }

    "be readable from JSON for appeal level '02'" in {
      val result = Json.fromJson(JsString("02"))(AppealLevelEnum.format)
      result shouldBe JsSuccess(TribunalAppealLevel)
    }

    "be readable from JSON for appeal level '03'" in {
      val result = Json.fromJson(JsString("03"))(AppealLevelEnum.format)
      result shouldBe JsSuccess(ThirdAppealLevel)
    }

    "throw a JsError when unable to read the JSON returned" in {
      val result = Json.fromJson(JsString("10"))(AppealLevelEnum.format)
      result shouldBe JsError("10 not recognised as appeal level value")
    }
  }

}
