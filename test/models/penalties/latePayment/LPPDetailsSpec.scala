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

package models.penalties.latePayment

import play.api.libs.json.Json
import testConstants.PenaltiesTestConstants.{lppDetailsFull, lppDetailsJson}
import testUtils.TestSupport

class LPPDetailsSpec extends TestSupport {

  "LPPDetails" should {
    "write to JSON" in {
      val result = Json.toJson(lppDetailsFull)(LPPDetails.format)
      result shouldBe lppDetailsJson
    }

    "read from JSON" in {
      val result = lppDetailsJson.as[LPPDetails](LPPDetails.format)
      result shouldBe lppDetailsFull
    }
  }

}
