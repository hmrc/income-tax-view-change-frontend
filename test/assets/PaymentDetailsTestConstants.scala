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
import models.paymentAllocations.{PaymentAllocationsError, PaymentDetails}
import play.api.libs.json.{JsValue, Json}

object PaymentDetailsTestConstants {

  val testValidPaymentDetailsJson: JsValue = Json.obj(
    "paymentDetails" -> Json.arr(PaymentAllocationsTestConstants.testValidPaymentAllocationsModelJson)
  )

  val testValidPaymentDetails: PaymentDetails = PaymentDetails(
    paymentDetails = Seq(PaymentAllocationsTestConstants.testValidPaymentAllocationsModel)
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
