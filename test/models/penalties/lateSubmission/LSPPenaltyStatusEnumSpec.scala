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

import play.api.libs.json.{JsString, Json}
import testUtils.TestSupport

class LSPPenaltyStatusEnumSpec extends TestSupport {
  "be writable to JSON for 'ACTIVE'" in {
    val result = Json.toJson(LSPPenaltyStatusEnum.Active)
    result shouldBe JsString("ACTIVE")
  }

  "be writable to JSON for 'INACTIVE'" in {
    val result = Json.toJson(LSPPenaltyStatusEnum.Inactive)
    result shouldBe JsString("INACTIVE")
  }

  "be readable from JSON for 'ACTIVE'" in {
    val result = Json.fromJson(JsString("ACTIVE"))(LSPPenaltyStatusEnum.format)
    result.get shouldBe LSPPenaltyStatusEnum.Active
  }

  "be readable from JSON for 'INACTIVE'" in {
    val result = Json.fromJson(JsString("INACTIVE"))(LSPPenaltyStatusEnum.format)
    result.get shouldBe LSPPenaltyStatusEnum.Inactive
  }

  "return JsError when the enum is not readable" in {
    val result = Json.fromJson(JsString("unknown"))(LSPPenaltyStatusEnum.format)
    result.isError shouldBe true
  }

}
