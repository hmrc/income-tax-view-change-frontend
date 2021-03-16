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

import play.api.libs.json.{JsValue, Json}

object OutstandingChargesIntegrationTestConstants {

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

  val validOutStandingChargeResponseJsonWithoutAciAndBcdCharges: JsValue = Json.parse(
    """
      |{
      |  "outstandingCharges": [{
      |         "chargeName": "LATE",
      |         "relevantDueDate": "2021-01-31",
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
