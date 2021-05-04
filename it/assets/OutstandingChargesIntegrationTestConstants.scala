/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json}

object OutstandingChargesIntegrationTestConstants {

  val dueDate = LocalDate.now().minusMonths(13).toString

  val validOutStandingChargeResponseJsonWithAciAndBcdCharges: JsValue = Json.parse(
    s"""
      |{
      |  "outstandingCharges": [{
      |         "chargeName": "BCD",
      |         "relevantDueDate": "$dueDate",
      |         "chargeAmount": 123456789012345.67,
      |         "tieBreaker": 1234
      |       },
      |       {
      |         "chargeName": "ACI",
      |         "relevantDueDate": "$dueDate",
      |         "chargeAmount": 12.67,
      |         "tieBreaker": 1234
      |       }
      |  ]
      |}
      |""".stripMargin)

  val validOutStandingChargeResponseJsonWithoutAciAndBcdCharges: JsValue = Json.parse(
    s"""
      |{
      |  "outstandingCharges": [{
      |         "chargeName": "LATE",
      |         "relevantDueDate": "$dueDate",
      |         "chargeAmount": 123456789012345.67,
      |         "tieBreaker": 1234
      |       }
      |  ]
      |}
      |""".stripMargin)

  val testOutstandingChargesErrorModelJson: JsValue = Json.obj(
    "code" -> 500,
    "message" -> "test error"
  )

}
