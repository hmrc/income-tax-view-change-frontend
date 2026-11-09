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

package testConstants

import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object OutstandingChargesIntegrationTestConstants {

  val currentDate = LocalDate.of(2023, 4, 5)
  val dueDate = currentDate.minusYears(1).minusMonths(1).toString

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
