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

import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS}
import play.api.libs.json._

sealed trait SubTransactionType extends TransactionType {
  val key: String
}

case object Nics2 extends SubTransactionType {
  override val key = "Nics2"
}

case object Accepted extends SubTransactionType {
  override val key = "Accepted"
}

case object Cancelled extends SubTransactionType {
  override val key = "Cancelled"
}

//
//case object Nics4 extends SubTransactionType {
//  override val key = "POA1"
//}
//
//case object StudentLoan extends SubTransactionType {
//  override val key = "POA1"
//}
//
//case object CapitalGainsTax extends SubTransactionType {
//  override val key = "POA1"
//}


object SubTransactionType {

  def fromDocumentText(documentText: String): Option[SubTransactionType] = {
    documentText match {
      case CODING_OUT_CLASS2_NICS.name =>
        Some(Nics2)
      case CODING_OUT_ACCEPTED.name =>
        Some(Accepted)
      case CODING_OUT_CANCELLED.name =>
        Some(Cancelled)
      case _ => None
    }
  }

  def fromCode(subTransaction: String): Option[ChargeType] = {
//    mainTransaction match {
//      case ChargeType.paymentOnAccountOne =>
//        Some(PaymentOnAccountOne)
//      case ChargeType.paymentOnAccountTwo =>
//        Some(PaymentOnAccountTwo)
//      case ChargeType.balancingCharge =>
//        Some(BalancingCharge)
//      case x if ChargeType.mfaDebit.contains(x) =>
//        Some(MfaDebitCharge)
//      case _ => None
//    }
    None
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
