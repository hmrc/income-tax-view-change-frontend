/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._

sealed trait ChargeType extends TransactionType {
  val key: String
}

case object PaymentOnAccountOne extends ChargeType {
  override val key = "POA1"
}

case object PaymentOnAccountTwo extends ChargeType {
  override val key = "POA2"
}

case object PaymentOnAccountOneReviewAndReconcile extends ChargeType {
  override val key = "POA1RR"
}

case object PaymentOnAccountTwoReviewAndReconcile extends ChargeType {
  override val key = "POA2RR"
}

case object BalancingCharge extends ChargeType {
  override val key = "BCD"
}

case object MfaDebitCharge extends ChargeType {
  override val key = "MfaDebit"
}

object ChargeType {

  // values come from EPID #1138
  private val balancingCharge = "4910"

  lazy val paymentOnAccountOneReviewAndReconcile = "4911"
  lazy val paymentOnAccountTwoReviewAndReconcile = "4913"
  
  private val paymentOnAccountOne = "4920"
  private val paymentOnAccountTwo = "4930"

  private val mfaDebit = Range.inclusive(4000, 4003)
    .map(_.toString).toList

  def fromCode(mainTransaction: String, reviewAndReconcileEnabled: Boolean): Option[ChargeType] = {

    (mainTransaction, reviewAndReconcileEnabled) match {
      case (ChargeType.paymentOnAccountOne, _) =>
        Some(PaymentOnAccountOne)
      case (ChargeType.paymentOnAccountTwo, _) =>
        Some(PaymentOnAccountTwo)
      case (ChargeType.balancingCharge, _) =>
        Some(BalancingCharge)
      case (ChargeType.paymentOnAccountOneReviewAndReconcile, true) =>
        Some(PaymentOnAccountOneReviewAndReconcile)
      case (ChargeType.paymentOnAccountTwoReviewAndReconcile, true) =>
        Some(PaymentOnAccountTwoReviewAndReconcile)
      case (x, _) if ChargeType.mfaDebit.contains(x) =>
        Some(MfaDebitCharge)
      case _ => {
        None
      }
    }
  }

  implicit val write: Writes[ChargeType] = new Writes[ChargeType] {
    def writes(transactionType: ChargeType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[ChargeType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case PaymentOnAccountOne.key => PaymentOnAccountOne
    case PaymentOnAccountTwo.key => PaymentOnAccountTwo
    case BalancingCharge.key => BalancingCharge
    case MfaDebitCharge.key => MfaDebitCharge
  }

  implicit val format: Format[ChargeType] = Format( read, write)

}
