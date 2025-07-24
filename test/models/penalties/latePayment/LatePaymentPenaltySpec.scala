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

import play.api.libs.json.{JsValue, Json}
import testConstants.PenaltiesTestConstants.lppDetailsFull
import testUtils.TestSupport

class LatePaymentPenaltySpec extends TestSupport {

  val lppDetails: Seq[LPPDetails] = Seq(lppDetailsFull)
  val latePaymentPenaltyModel: LatePaymentPenalty =
    LatePaymentPenalty(
      details = Some(lppDetails),
      manualLPPIndicator = Some(false))

  val latePaymentPenaltyJson: JsValue = Json.parse("""{
                     |  "details" : [ {
                     |    "principalChargeReference" : "12345678901234",
                     |    "penaltyCategory" : "LPP1",
                     |    "penaltyStatus" : "P",
                     |    "penaltyAmountAccruing" : 99.99,
                     |    "penaltyAmountPosted" : 1001.45,
                     |    "penaltyAmountPaid" : 1001.45,
                     |    "penaltyAmountOutstanding" : 99.99,
                     |    "LPP1LRCalculationAmount" : 99.99,
                     |    "LPP1LRDays" : "15",
                     |    "LPP1LRPercentage" : 2.21,
                     |    "LPP1HRCalculationAmount" : 99.99,
                     |    "LPP1HRDays" : "31",
                     |    "LPP1HRPercentage" : 2.21,
                     |    "LPP2Days" : "31",
                     |    "LPP2Percentage" : 4.59,
                     |    "penaltyChargeCreationDate" : "2069-10-30",
                     |    "communicationsDate" : "2069-10-30",
                     |    "penaltyChargeReference" : "1234567890",
                     |    "penaltyChargeDueDate" : "2069-10-30",
                     |    "appealInformation" : [ {
                     |      "appealStatus" : "99",
                     |      "appealLevel" : "01",
                     |      "appealDescription" : "Late"
                     |    } ],
                     |    "principalChargeDocNumber" : "123456789012",
                     |    "principalChargeMainTransaction" : "4700",
                     |    "principalChargeSubTransaction" : "1174",
                     |    "principalChargeBillingFrom" : "2069-10-30",
                     |    "principalChargeBillingTo" : "2069-10-30",
                     |    "principalChargeDueDate" : "2069-10-30",
                     |    "timeToPay" : [ {
                     |      "TTPStartDate" : "2023-10-12",
                     |      "TTPEndDate" : "2024-10-12"
                     |    } ]
                     |  } ],
                     |  "manualLPPIndicator" : false
                     |}
                     |""".stripMargin)

  "LatePaymentPenalty" should {

    "write to JSON" in {
      val result = Json.toJson(latePaymentPenaltyModel)(LatePaymentPenalty.format)
      result shouldBe latePaymentPenaltyJson
    }

    "read from JSON" in {
      val modelToJson = Json.toJson(latePaymentPenaltyJson)
      val result = modelToJson.as[LatePaymentPenalty](LatePaymentPenalty.format)
      result shouldBe latePaymentPenaltyModel
    }
  }

}
