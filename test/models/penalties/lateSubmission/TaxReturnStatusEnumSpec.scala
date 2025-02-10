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

class TaxReturnStatusEnumSpec extends TestSupport {

  "TaxReturnStatusEnum" should {

    "be writeable to JSON for a tax return status of 'Open'" in {
      val result = Json.toJson(OpenTaxReturnStatus)(TaxReturnStatusEnum.writes.writes(_))
      result shouldBe JsString("Open")
    }

    "be writeable to JSON for a tax return status of 'Fulfilled'" in {
      val result = Json.toJson(FulfilledTaxReturnStatus)(TaxReturnStatusEnum.writes.writes(_))
      result shouldBe JsString("Fulfilled")
    }

    "be writeable to JSON for a tax return status of 'Reversed'" in {
      val result = Json.toJson(ReversedTaxReturnStatus)(TaxReturnStatusEnum.writes.writes(_))
      result shouldBe JsString("Reversed")
    }

    "be readable from JSON for a tax return status of 'Open'" in {
      val result = Json.fromJson(JsString("Open"))(TaxReturnStatusEnum.format)
      result shouldBe JsSuccess(OpenTaxReturnStatus)
    }

    "be readable from JSON for a tax return status of 'Fulfilled'" in {
      val result = Json.fromJson(JsString("Fulfilled"))(TaxReturnStatusEnum.format)
      result shouldBe JsSuccess(FulfilledTaxReturnStatus)
    }

    "be readable from JSON for a tax return status of 'Reversed'" in {
      val result = Json.fromJson(JsString("Reversed"))(TaxReturnStatusEnum.format)
      result shouldBe JsSuccess(ReversedTaxReturnStatus)
    }

    "throw a JsError when the JSON returned is unrecognised" in {
      val result = Json.fromJson(JsString("String"))(TaxReturnStatusEnum.format)
      result shouldBe JsError("STRING not recognised as a tax return status")
    }
  }
}
