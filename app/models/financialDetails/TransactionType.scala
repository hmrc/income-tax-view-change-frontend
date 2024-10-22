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

case object PaymentOnAccountOneReviewAndReconcileCredit extends CreditType {
  override val key = "POA1RR-credit"
}

case object PaymentOnAccountTwoReviewAndReconcileCredit extends CreditType {
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

  val read: Reads[TransactionType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case MfaCreditType.key => MfaCreditType
    case CutOverCreditType.key => CutOverCreditType
    case BalancingChargeCreditType.key => BalancingChargeCreditType
    case RepaymentInterest.key => RepaymentInterest
    case PaymentOnAccountOneReviewAndReconcileCredit.key => PaymentOnAccountOneReviewAndReconcileCredit
    case PaymentOnAccountTwoReviewAndReconcileCredit.key => PaymentOnAccountTwoReviewAndReconcileCredit
    case PaymentType.key => PaymentType
    case Repayment.key => Repayment
    case PaymentOnAccountOne.key => PaymentOnAccountOne
    case PaymentOnAccountTwo.key => PaymentOnAccountTwo
    case PaymentOnAccountOneReviewAndReconcile.key => PaymentOnAccountOneReviewAndReconcile
    case PaymentOnAccountTwoReviewAndReconcile.key => PaymentOnAccountTwoReviewAndReconcile
    case BalancingCharge.key => BalancingCharge
    case MfaDebitCharge.key => MfaDebitCharge
  }

  implicit val format: Format[TransactionType] = Format( read, write)
}

object ChargeType {

  // values come from EPID #1138
  private val balancingCharge = "4910"

  lazy val paymentOnAccountOneReviewAndReconcileDebit = "4911"
  lazy val paymentOnAccountTwoReviewAndReconcileDebit = "4913"

  lazy val paymentOnAccountOneReviewAndReconcileCredit = "4912"
  lazy val paymentOnAccountTwoReviewAndReconcileCredit = "4914"

  private val paymentOnAccountOne = "4920"
  private val paymentOnAccountTwo = "4930"

  private val mfaDebit = Range.inclusive(4000, 4003)
    .map(_.toString).toList

  def fromCode(mainTransaction: String, reviewAndReconcileEnabled: Boolean): Option[TransactionType] = {

    (mainTransaction, reviewAndReconcileEnabled) match {
      case (ChargeType.paymentOnAccountOne, _) =>
        Some(PaymentOnAccountOne)
      case (ChargeType.paymentOnAccountTwo, _) =>
        Some(PaymentOnAccountTwo)
      case (ChargeType.balancingCharge, _) =>
        Some(BalancingCharge)
      case (ChargeType.paymentOnAccountOneReviewAndReconcileDebit, true) =>
        Some(PaymentOnAccountOneReviewAndReconcile)
      case (ChargeType.paymentOnAccountTwoReviewAndReconcileDebit, true) =>
        Some(PaymentOnAccountTwoReviewAndReconcile)
      case (ChargeType.paymentOnAccountOneReviewAndReconcileCredit, true) =>
        Some(PaymentOnAccountOneReviewAndReconcileCredit)
      case (ChargeType.paymentOnAccountTwoReviewAndReconcileCredit, true) =>
        Some(PaymentOnAccountTwoReviewAndReconcileCredit)
      case (x, _) if ChargeType.mfaDebit.contains(x) =>
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
    case PaymentOnAccountOne.key => PaymentOnAccountOne
    case PaymentOnAccountTwo.key => PaymentOnAccountTwo
    case PaymentOnAccountOneReviewAndReconcile.key => PaymentOnAccountOneReviewAndReconcile
    case PaymentOnAccountTwoReviewAndReconcile.key => PaymentOnAccountTwoReviewAndReconcile
    case BalancingCharge.key => BalancingCharge
    case MfaDebitCharge.key => MfaDebitCharge
  }

  implicit val format: Format[ChargeType] = Format( read, write)

}

object CreditType {

  // values come from EPID #1138
  private val cutOver = "6110"
  private val balancingCharge = "4905"
  private val repaymentInterest = "6020"
  private val mfaCredit = Range.inclusive(4004, 4025)
    .filterNot(_ == 4010).filterNot(_ == 4020).map(_.toString)
    .toList
  private val paymentOnAccountOneReviewAndReconcileCredit = "4912"
  private val paymentOnAccountTwoReviewAndReconcileCredit = "4914"

  private val payment = List("0060")

  def fromCode(mainTransaction: String): Option[CreditType] = {
    mainTransaction match {
      case CreditType.cutOver =>
        Some(CutOverCreditType)
      case CreditType.balancingCharge =>
        Some(BalancingChargeCreditType)
      case CreditType.repaymentInterest =>
        Some(RepaymentInterest)
      case CreditType.paymentOnAccountOneReviewAndReconcileCredit =>
        Some(PaymentOnAccountOneReviewAndReconcileCredit)
      case CreditType.paymentOnAccountTwoReviewAndReconcileCredit =>
        Some(PaymentOnAccountTwoReviewAndReconcileCredit)
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
    case MfaCreditType.key => MfaCreditType
    case CutOverCreditType.key => CutOverCreditType
    case BalancingChargeCreditType.key => BalancingChargeCreditType
    case RepaymentInterest.key => RepaymentInterest
    case PaymentOnAccountOneReviewAndReconcileCredit.key => PaymentOnAccountOneReviewAndReconcileCredit
    case PaymentOnAccountTwoReviewAndReconcileCredit.key => PaymentOnAccountTwoReviewAndReconcileCredit
    case PaymentType.key => PaymentType
    case Repayment.key => Repayment
  }

  implicit val format: Format[CreditType] = Format( read, write)

}

