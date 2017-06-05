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

import models.ErrorResponse
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}

object IntegrationTestConstants {

  val testEnrolmentKey = "HMRC-MTD-IT"
  val testEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"
  val testErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "Internal Server Error Message")

  object GetFinancialDataResponse {
    def successResponse(total: BigDecimal, incomeTax: BigDecimal, nic2: BigDecimal, nic4: BigDecimal): JsValue =
      Json.parse(s"""
         |{
         |   "total": $total,
         |   "nic2": $nic2,
         |   "nic4": $nic4,
         |   "incomeTax": $incomeTax
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
}
