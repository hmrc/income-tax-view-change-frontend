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

class TransactionTypeSpec extends UnitSpec{

  "write" should {
    "return correct String value based on the type passed" in {
      val json = Json.toJson[TransactionType](BalancingCharge)
      json shouldBe JsString("BCD")
    }
  }

  "read" should {
    "return correct type based on the String passed" in {
      Json.fromJson[TransactionType](JsString("mfa")) shouldBe(JsSuccess(MfaCreditType))
      Json.fromJson[TransactionType](JsString("cutOver")) shouldBe(JsSuccess(CutOverCreditType))
      Json.fromJson[TransactionType](JsString("balancingCharge")) shouldBe(JsSuccess(BalancingChargeCreditType))
      Json.fromJson[TransactionType](JsString("repaymentInterest")) shouldBe(JsSuccess(RepaymentInterest))
      Json.fromJson[TransactionType](JsString("POA1RR-credit")) shouldBe(JsSuccess(PoaOneReconciliationCredit))
      Json.fromJson[TransactionType](JsString("POA2RR-credit")) shouldBe(JsSuccess(PoaTwoReconciliationCredit))
      Json.fromJson[TransactionType](JsString("payment")) shouldBe(JsSuccess(PaymentType))
      Json.fromJson[TransactionType](JsString("refund")) shouldBe(JsSuccess(Repayment))
      Json.fromJson[TransactionType](JsString("POA1")) shouldBe(JsSuccess(PoaOneDebit))
      Json.fromJson[TransactionType](JsString("POA2")) shouldBe(JsSuccess(PoaTwoDebit))
      Json.fromJson[TransactionType](JsString("POA1RR-debit")) shouldBe(JsSuccess(PoaOneReconciliationDebit))
      Json.fromJson[TransactionType](JsString("POA2RR-debit")) shouldBe(JsSuccess(PoaTwoReconciliationDebit))
      Json.fromJson[TransactionType](JsString("BCD")) shouldBe(JsSuccess(BalancingCharge))
      Json.fromJson[TransactionType](JsString("LSP")) shouldBe(JsSuccess(LateSubmissionPenalty))
      Json.fromJson[TransactionType](JsString("LPP1")) shouldBe(JsSuccess(FirstLatePaymentPenalty))
      Json.fromJson[TransactionType](JsString("LPP2")) shouldBe(JsSuccess(SecondLatePaymentPenalty))
      Json.fromJson[TransactionType](JsString("MfaDebit")) shouldBe(JsSuccess(MfaDebitCharge))
    }
  }
}
