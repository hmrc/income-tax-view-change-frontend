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

import play.api.libs.json.{JsValue, Json}
import testConstants.PenaltiesTestConstants.lspSummary
import testUtils.TestSupport

class LSPSummarySpec extends TestSupport {

  val lspSummaryJson: JsValue = Json.parse("""{
                                    |  "activePenaltyPoints" : 2,
                                    |  "inactivePenaltyPoints" : 0,
                                    |  "PoCAchievementDate" : "2020-05-07",
                                    |  "regimeThreshold" : 2,
                                    |  "penaltyChargeAmount" : 145.33
                                    |}
                                    |""".stripMargin)

  "LSPSummary" should {

    "write to JSON" in {
      val result = Json.toJson(lspSummary)(LSPSummary.format)
      result shouldBe lspSummaryJson
    }

    "read from JSON" in {
      val result = lspSummaryJson.as[LSPSummary](LSPSummary.format)
      result shouldBe lspSummary
    }
  }

}
