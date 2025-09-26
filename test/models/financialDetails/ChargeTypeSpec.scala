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

package models.financialDetails

import play.api.libs.json.{JsString, JsSuccess, Json}
import testUtils.UnitSpec

class ChargeTypeSpec extends UnitSpec{

  "write" should {
    "return correct String based on the type passed" in {
      val json = Json.toJson[ChargeType](MfaDebitCharge)
      json shouldBe JsString("MfaDebit")
    }
  }

  "read" should {
    "return correct type based on the String passed" in {
      Json.fromJson[ChargeType](JsString("POA1")) shouldBe(JsSuccess(PoaOneDebit))
      Json.fromJson[ChargeType](JsString("POA2")) shouldBe(JsSuccess(PoaTwoDebit))
      Json.fromJson[ChargeType](JsString("POA1RR-debit")) shouldBe(JsSuccess(PoaOneReconciliationDebit))
      Json.fromJson[ChargeType](JsString("POA2RR-debit")) shouldBe(JsSuccess(PoaTwoReconciliationDebit))
      Json.fromJson[ChargeType](JsString("BCD")) shouldBe(JsSuccess(BalancingCharge))
      Json.fromJson[ChargeType](JsString("LSP")) shouldBe(JsSuccess(LateSubmissionPenalty))
      Json.fromJson[ChargeType](JsString("LPP1")) shouldBe(JsSuccess(FirstLatePaymentPenalty))
      Json.fromJson[ChargeType](JsString("LPP2")) shouldBe(JsSuccess(SecondLatePaymentPenalty))
      Json.fromJson[ChargeType](JsString("MfaDebit")) shouldBe(JsSuccess(MfaDebitCharge))

    }
  }
}
