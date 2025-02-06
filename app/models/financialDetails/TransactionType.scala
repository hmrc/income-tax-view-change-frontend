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

sealed trait TransactionType {
  val key: String
}

sealed trait ChargeType extends TransactionType {
  val key: String
}

sealed trait CreditType extends TransactionType {
  val key: String
}

// Charge Types

case object PoaOneDebit extends ChargeType {
  override val key = "POA1"
}

case object PoaTwoDebit extends ChargeType {
  override val key = "POA2"
}

case object PoaOneReconciliationDebit extends ChargeType {
  override val key = "POA1RR-debit"
}

case object PoaTwoReconciliationDebit extends ChargeType {
  override val key = "POA2RR-debit"
}

case object PoaOneReconciliationCredit extends CreditType {
  override val key = "POA1RR-credit"
}

case object PoaTwoReconciliationCredit extends CreditType {
  override val key = "POA2RR-credit"
}

case object BalancingCharge extends ChargeType {
  override val key = "BCD"
}

case object MfaDebitCharge extends ChargeType {
  override val key = "MfaDebit"
}

// Credit Types

case object MfaCreditType extends CreditType {
  override val key = "mfa"
}

case object CutOverCreditType extends CreditType {
  override val key = "cutOver"
}

case object BalancingChargeCreditType extends CreditType {
  override val key = "balancingCharge"
}

case object RepaymentInterest extends CreditType {
  override val key = "repaymentInterest"
}

case object PaymentType extends CreditType {
  override val key = "payment"
}

case object Repayment extends CreditType {
  override val key = "refund"
}

object TransactionType {
  implicit val write: Writes[TransactionType] = new Writes[TransactionType] {
    def writes(transactionType: TransactionType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[TransactionType] =
    (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
      case MfaCreditType.key              => MfaCreditType
      case CutOverCreditType.key          => CutOverCreditType
      case BalancingChargeCreditType.key  => BalancingChargeCreditType
      case RepaymentInterest.key          => RepaymentInterest
      case PoaOneReconciliationCredit.key => PoaOneReconciliationCredit
      case PoaTwoReconciliationCredit.key => PoaTwoReconciliationCredit
      case PaymentType.key                => PaymentType
      case Repayment.key                  => Repayment
      case PoaOneDebit.key                => PoaOneDebit
      case PoaTwoDebit.key                => PoaTwoDebit
      case PoaOneReconciliationDebit.key  => PoaOneReconciliationDebit
      case PoaTwoReconciliationDebit.key  => PoaTwoReconciliationDebit
      case BalancingCharge.key            => BalancingCharge
      case MfaDebitCharge.key             => MfaDebitCharge
    }

  implicit val format: Format[TransactionType] = Format(read, write)
}

object ChargeType {

  //The following are mainTransaction values:
  // values come from EPID #1138
  private val balancingCharge = "4910"

  lazy val poaOneReconciliationDebit = "4911"
  lazy val poaTwoReconciliationDebit = "4913"

  lazy val poaOneReconciliationCredit = "4912"
  lazy val poaTwoReconciliationCredit = "4914"

  private val poaOneDebit = "4920"
  private val poaTwoDebit = "4930"

  private val mfaDebit = Range
    .inclusive(4000, 4003)
    .map(_.toString)
    .toList

  def fromCode(mainTransaction: String): Option[TransactionType] = {

    mainTransaction match {
      case ChargeType.poaOneDebit =>
        Some(PoaOneDebit)
      case ChargeType.poaTwoDebit =>
        Some(PoaTwoDebit)
      case ChargeType.balancingCharge =>
        Some(BalancingCharge)
      case ChargeType.poaOneReconciliationDebit =>
        Some(PoaOneReconciliationDebit)
      case ChargeType.poaTwoReconciliationDebit =>
        Some(PoaTwoReconciliationDebit)
      case ChargeType.poaOneReconciliationCredit =>
        Some(PoaOneReconciliationCredit)
      case ChargeType.poaTwoReconciliationCredit =>
        Some(PoaTwoReconciliationCredit)
      case x if ChargeType.mfaDebit.contains(x) =>
        Some(MfaDebitCharge)
      case _ => None
    }
  }

  implicit val write: Writes[ChargeType] = new Writes[ChargeType] {
    def writes(transactionType: ChargeType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[ChargeType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case PoaOneDebit.key               => PoaOneDebit
    case PoaTwoDebit.key               => PoaTwoDebit
    case PoaOneReconciliationDebit.key => PoaOneReconciliationDebit
    case PoaTwoReconciliationDebit.key => PoaTwoReconciliationDebit
    case BalancingCharge.key           => BalancingCharge
    case MfaDebitCharge.key            => MfaDebitCharge
  }

  implicit val format: Format[ChargeType] = Format(read, write)

}

object CreditType {

  // values come from EPID #1138
  private val cutOver           = "6110"
  private val balancingCharge   = "4905"
  private val repaymentInterest = "6020"
  private val mfaCredit = Range
    .inclusive(4004, 4025)
    .filterNot(_ == 4010)
    .filterNot(_ == 4020)
    .map(_.toString)
    .toList
  private val poaOneReconciliationCredit = "4912"
  private val poaTwoReconciliationCredit = "4914"

  private val payment = List("0060")

  def fromCode(mainTransaction: String): Option[CreditType] = {
    mainTransaction match {
      case CreditType.cutOver =>
        Some(CutOverCreditType)
      case CreditType.balancingCharge =>
        Some(BalancingChargeCreditType)
      case CreditType.repaymentInterest =>
        Some(RepaymentInterest)
      case CreditType.poaOneReconciliationCredit =>
        Some(PoaOneReconciliationCredit)
      case CreditType.poaTwoReconciliationCredit =>
        Some(PoaTwoReconciliationCredit)
      case x if mfaCredit.contains(x) =>
        Some(MfaCreditType)
      case x if payment.contains(x) =>
        Some(PaymentType)
      case _ => None
    }
  }

  implicit val write: Writes[CreditType] = new Writes[CreditType] {
    def writes(transactionType: CreditType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[CreditType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case MfaCreditType.key              => MfaCreditType
    case CutOverCreditType.key          => CutOverCreditType
    case BalancingChargeCreditType.key  => BalancingChargeCreditType
    case RepaymentInterest.key          => RepaymentInterest
    case PoaOneReconciliationCredit.key => PoaOneReconciliationCredit
    case PoaTwoReconciliationCredit.key => PoaTwoReconciliationCredit
    case PaymentType.key                => PaymentType
    case Repayment.key                  => Repayment
  }

  implicit val format: Format[CreditType] = Format(read, write)

}
