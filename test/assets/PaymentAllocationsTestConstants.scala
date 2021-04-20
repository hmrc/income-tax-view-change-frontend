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
import models.paymentAllocations.{AllocationDetail, PaymentAllocations, PaymentAllocationsError}
import play.api.libs.json.{JsValue, Json}

object PaymentAllocationsTestConstants {

  val testValidPaymentAllocationsModelJson: JsValue = Json.obj(
    "amount" -> 110.10,
    "method" -> "Payment by Card",
    "transactionDate" -> "2019-05-27",
    "reference" -> "reference",
    "allocations" -> Json.arr(
      Json.obj(
        "transactionId" -> "1040000872",
        "from" -> "2019-06-27",
        "to" -> "2019-08-27",
        "type" -> "1481",
        "amount" -> 10.10,
        "clearedAmount" -> 5.50
      ),
      Json.obj(
        "transactionId" -> "1040000873",
        "from" -> "2019-07-28",
        "to" -> "2019-09-28",
        "type" -> "1482",
        "amount" -> 10.90,
        "clearedAmount" -> 5.90
      )
    )
  )

  val testValidPaymentAllocationsModel: PaymentAllocations = PaymentAllocations(
    Some(110.10), Some("Payment by Card"), Some("2019-05-27"), Some("reference"),
    Seq(
      AllocationDetail(Some("1040000872"), Some("2019-06-27"), Some("2019-08-27"), Some("1481"), Some(10.10), Some(5.50)),
      AllocationDetail(Some("1040000873"), Some("2019-07-28"), Some("2019-09-28"), Some("1482"), Some(10.90), Some(5.90))
    )
  )
  val testInvalidPaymentAllocationsModelJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testPaymentAllocationsErrorModelParsing: PaymentAllocationsError = PaymentAllocationsError(
    testErrorStatus, "Json Validation Error. Parsing Payment Allocations Data Response")

  val testPaymentAllocationsErrorModel: PaymentAllocationsError = PaymentAllocationsError(testErrorStatus, testErrorMessage)
  val testPaymentAllocationsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )
}
