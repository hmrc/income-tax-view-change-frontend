/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testPaymentRedirectUrl}
import models.{PaymentDataModel, PaymentErrorModel}
import play.api.libs.json.{JsValue, Json}

object PaymentDataTestConstants {

  val testTaxType = "mtdfb-itsa"
  val testAmountInPence = 10000000

  val testPaymentDataModel: PaymentDataModel = PaymentDataModel(testTaxType, testMtditid, testAmountInPence, testPaymentRedirectUrl)

  val testPaymentDataJson: JsValue =
    Json.obj(
      "taxType" -> testTaxType,
      "taxReference" -> testMtditid,
      "amountInPence" -> testAmountInPence,
      "returnUrl" -> testPaymentRedirectUrl
    )

  val testPaymentErrorModel: PaymentErrorModel = PaymentErrorModel(testErrorStatus,testErrorMessage)

  val testPaymentErrorJson: JsValue =
    Json.obj(
      "code" -> testErrorStatus,
      "message" -> testErrorMessage
    )

}
