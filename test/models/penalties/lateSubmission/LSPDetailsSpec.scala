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
import testConstants.PenaltiesTestConstants.lspDetails
import testUtils.TestSupport

class LSPDetailsSpec extends TestSupport {

  val lspDetailsJson: JsValue = Json.parse("""{
                                    |  "penaltyNumber" : "12345678901234",
                                    |  "penaltyOrder" : "01",
                                    |  "penaltyCategory" : "P",
                                    |  "penaltyStatus" : "ACTIVE",
                                    |  "FAPIndicator" : "X",
                                    |  "penaltyCreationDate" : "2022-10-30",
                                    |  "triggeringProcess" : "P123",
                                    |  "penaltyExpiryDate" : "2022-10-30",
                                    |  "expiryReason" : "EXP",
                                    |  "communicationsDate" : "2022-10-30",
                                    |  "lateSubmissions" : [ {
                                    |    "lateSubmissionID" : "001",
                                    |    "taxPeriod" : "23AA",
                                    |    "taxReturnStatus" : "Fulfilled",
                                    |    "taxPeriodStartDate" : "2022-01-01",
                                    |    "taxPeriodEndDate" : "2022-12-31",
                                    |    "taxPeriodDueDate" : "2023-02-01",
                                    |    "returnReceiptDate" : "2023-02-01"
                                    |  } ],
                                    |  "appealInformation" : [ {
                                    |    "appealStatus" : "99",
                                    |    "appealLevel" : "01",
                                    |    "appealDescription" : "Late"
                                    |  } ],
                                    |  "chargeReference" : "CHARGEREF1",
                                    |  "chargeAmount" : 200,
                                    |  "chargeOutstandingAmount" : 200,
                                    |  "chargeDueDate" : "2022-10-30"
                                    |}
                                    |""".stripMargin)

  "LSPDetails" should {

    "write to JSON" in {
      val result = Json.toJson(lspDetails)(LSPDetails.format)
      result shouldBe lspDetailsJson
    }

    "read from JSON" in {
      val result = lspDetailsJson.as[LSPDetails](LSPDetails.format)
      result shouldBe lspDetails
    }
  }

}
