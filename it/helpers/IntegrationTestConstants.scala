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

package helpers

import models.{ErrorResponse, ObligationsModel, SuccessResponse}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import utils.ImplicitDateFormatter.localDate

object IntegrationTestConstants {

  val testDate = localDate("2018-05-05")

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"

  val testSelfEmploymentId = "ABC123456789"

  val testErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "Internal Server Error Message")

  object GetLastCalculation {
    def successResponse(calcId: String, calcTimestamp: String, calcAmount: BigDecimal): JsValue =
      Json.parse(s"""
         |{
         |   "calcId": "$calcId",
         |   "calcTimestamp": "$calcTimestamp",
         |   "calcAmount": $calcAmount
         |}
         |""".stripMargin)

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(s"""
         |{
         |   "code": "$code",
         |   "reason":"$reason"
         |}
      """.stripMargin)
  }

  object GetBusinessDetails {
    def successResponse(selfEmploymentId: String): JsValue =
      Json.parse(
        s"""
          {
            "business":[
            {
              "id": "$selfEmploymentId",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
              ]
            }""".stripMargin)
    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(s"""
                    |{
                    |   "code": "$code",
                    |   "reason":"$reason"
                    |}
      """.stripMargin)
  }

  object GetObligationsData {
    def successResponse(obligationsModel: ObligationsModel): JsValue = {
      Json.toJson(obligationsModel)
    }

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(
        s"""
           |{
           |  "code": $code,
           |  "reason": $reason
           |}
         """.stripMargin)
  }
}
