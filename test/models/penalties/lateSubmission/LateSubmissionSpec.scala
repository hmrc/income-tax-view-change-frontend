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
import testConstants.PenaltiesTestConstants.lateSubmission
import testUtils.TestSupport

class LateSubmissionSpec extends TestSupport {

  val lateSubmissionJson: JsValue = Json.parse("""{
                                        |  "lateSubmissionID" : "001",
                                        |  "taxPeriod" : "23AA",
                                        |  "taxReturnStatus" : "Fulfilled",
                                        |  "taxPeriodStartDate" : "2022-01-01",
                                        |  "taxPeriodEndDate" : "2022-12-31",
                                        |  "taxPeriodDueDate" : "2023-02-01",
                                        |  "returnReceiptDate" : "2023-02-01"
                                        |}
                                        |""".stripMargin)

  "LateSubmission" should {

    "write to JSON" in {
      val result = Json.toJson(lateSubmission)(LateSubmission.writes)
      result shouldBe lateSubmissionJson
    }

    "read from JSON" in {
      val result = lateSubmissionJson.as[LateSubmission](LateSubmission.reads)
      result shouldBe lateSubmission
    }
  }

}
