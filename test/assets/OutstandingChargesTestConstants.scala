/*
 * Copyright 2021 HM Revenue & Customs
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

package assets

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesErrorModel, OutstandingChargesModel}
import play.api.libs.json.{JsValue, Json}

object OutstandingChargesTestConstants {


  val testValidOutstandingChargesModelJson: JsValue = Json.parse(
    """
      |  {
      |    "chargeName": "LATE",
      |    "relevantDueDate": "2021-01-31",
      |    "chargeAmount": 123456789012345.67,
      |    "tieBreaker": 1234
      |  }
      |""".stripMargin)


  val testValidOutstandingChargesModelJson1: JsValue = Json.obj(
    "outstandingCharges" -> Json.arr(
      Json.obj(
        "chargeName" -> "LATE"
      )))

  val testValidOutstandingChargesModel: OutstandingChargesModel = OutstandingChargesModel(List(
    OutstandingChargeModel("LATE", Some("2021-01-31"), 123456789012345.67, 1234)))


  val testOutstandingChargesErrorModelParsing: OutstandingChargesErrorModel = OutstandingChargesErrorModel(
    testErrorStatus, "Json Validation Error. Parsing OutstandingCharges Data Response")

  val testOutstandingChargesErrorModel: OutstandingChargesErrorModel = OutstandingChargesErrorModel(testErrorStatus, testErrorMessage)
  val testOutstandingChargesErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )


  val testInvalidOutstandingChargesJson: JsValue = Json.obj(
    "chargeName" -> "LATE",
    "relevantDueDate" -> "2021-01-31",
    "chargeAmount" -> 123456789012345.67,
  )

  val testInvalidBadOutstandingChargesJson: JsValue = Json.obj(
    "chargeName" -> "LATE"

  )

  val testValidOutStandingChargeModelJson: JsValue = Json.obj(
    "outstandingCharges" -> Json.arr(
      Json.obj(
        "chargeName" -> "LATE",
        "relevantDueDate" -> "2021-01-31",
        "chargeAmount" -> 123456789012345.67,
        "tieBreaker" -> 1234
      )))

  val validOutStandingChargeJson: JsValue = Json.parse(
    """
      |  {
      |    "chargeName": "LATE",
      |    "relevantDueDate": "2021-01-31",
      |    "chargeAmount": 123456789012345.67,
      |    "tieBreaker": 1234
      |  }
      |""".stripMargin)

  val invalidOutStandingChargeJson: JsValue = Json.parse(
    """
      |  {
      |    "chargeName": "LATE"
      |  }
      |""".stripMargin)

  val testBadOutstandingChargesJson: JsValue = Json.parse(
    """
      |  {
      |    "chargeName": "LATE",
      |    "relevantDueDate": "2021-01-31",
      |    "chargeAmount": 123456789012345.67
      |  }
      |""".stripMargin)

  val validOutStandingChargeResponseJsonWithAciAndBcdCharges: JsValue = Json.parse(
    """
      |{
      |  "outstandingCharges": [{
      |         "chargeName": "BCD",
      |         "relevantDueDate": "2021-01-31",
      |         "chargeAmount": 123456789012345.67,
      |         "tieBreaker": 1234
      |       },
      |       {
      |         "chargeName": "ACI",
      |         "relevantDueDate": "2021-01-31",
      |         "chargeAmount": 12.67,
      |         "tieBreaker": 1234
      |       }
      |  ]
      |}
      |""".stripMargin)


}
