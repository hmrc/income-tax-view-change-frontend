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

class AppealStatusEnumSpec extends TestSupport {

  "AppealStatusEnum" should {

    def writeableTest(appealStatus: AppealStatusEnum.Value, expectedValue: String): Unit = {
      s"be writeable to JSON for appeal status $appealStatus" in {
        val result = Json.toJson(appealStatus)(AppealStatusEnum.format)
        result shouldBe JsString(expectedValue)
      }
    }

    def readableTest(jsonValue: String, expectedValue: AppealStatusEnum.Value): Unit = {
      s"be readable from JSON for appeal status $jsonValue" in {
        val result = Json.fromJson(JsString(jsonValue))(AppealStatusEnum.format)
        result shouldBe JsSuccess(expectedValue)
      }
    }

    writeableTest(AppealStatusEnum.Under_Appeal, "A")
    writeableTest(AppealStatusEnum.Upheld, "B")
    writeableTest(AppealStatusEnum.Rejected, "C")
    writeableTest(AppealStatusEnum.Unappealable, "99")
    writeableTest(AppealStatusEnum.AppealRejectedChargeAlreadyReversed, "91")
    writeableTest(AppealStatusEnum.AppealUpheldPointAlreadyRemoved, "92")
    writeableTest(AppealStatusEnum.AppealUpheldChargeAlreadyReversed, "93")
    writeableTest(AppealStatusEnum.AppealRejectedPointAlreadyRemoved, "94")

    readableTest("A", AppealStatusEnum.Under_Appeal)
    readableTest("B", AppealStatusEnum.Upheld)
    readableTest("C", AppealStatusEnum.Rejected)
    readableTest("99", AppealStatusEnum.Unappealable)
    readableTest("91", AppealStatusEnum.AppealRejectedChargeAlreadyReversed)
    readableTest("92", AppealStatusEnum.AppealUpheldPointAlreadyRemoved)
    readableTest("93", AppealStatusEnum.AppealUpheldChargeAlreadyReversed)
    readableTest("94", AppealStatusEnum.AppealRejectedPointAlreadyRemoved)

    "throw a JsError when unable to read from JSON" in {
      val result = Json.fromJson(JsString("100"))(AppealStatusEnum.format)
      result shouldBe JsError("100 not recognised")
    }
  }

}
