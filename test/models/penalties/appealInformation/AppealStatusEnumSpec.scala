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

    def writeableTest(appealStatus: AppealStatusEnum, expectedValue: String): Unit = {
      s"be writeable to JSON for appeal status $appealStatus" in {
        val result = Json.toJson(appealStatus)(AppealStatusEnum.writes.writes(_))
        result shouldBe JsString(expectedValue)
      }
    }

    def readableTest(jsonValue: String, expectedValue: AppealStatusEnum): Unit = {
      s"be readable from JSON for appeal status $jsonValue" in {
        val result = Json.fromJson(JsString(jsonValue))(AppealStatusEnum.format)
        result shouldBe JsSuccess(expectedValue)
      }
    }

    writeableTest(UnderAppeal, "A")
    writeableTest(Upheld, "B")
    writeableTest(Rejected, "C")
    writeableTest(Unappealable, "99")
    writeableTest(AppealRejectedChargeAlreadyReversed, "91")
    writeableTest(AppealUpheldPointAlreadyRemoved, "92")
    writeableTest(AppealUpheldChargeAlreadyReversed, "93")
    writeableTest(AppealRejectedPointAlreadyRemoved, "94")

    readableTest("A", UnderAppeal)
    readableTest("B", Upheld)
    readableTest("C", Rejected)
    readableTest("99", Unappealable)
    readableTest("91", AppealRejectedChargeAlreadyReversed)
    readableTest("92", AppealUpheldPointAlreadyRemoved)
    readableTest("93", AppealUpheldChargeAlreadyReversed)
    readableTest("94", AppealRejectedPointAlreadyRemoved)

    "throw a JsError when unable to read from JSON" in {
      val result = Json.fromJson(JsString("100"))(AppealStatusEnum.format)
      result shouldBe JsError("100 not recognised as appeal status value")
    }
  }

}
