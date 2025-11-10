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

case object BalancingCharge extends ChargeType {
  override val key = "BCD"
}

case object LateSubmissionPenalty extends ChargeType {
  override val key: String = "LSP"
}

case object FirstLatePaymentPenalty extends ChargeType {
  override val key: String = "LPP1"
}

case object SecondLatePaymentPenalty extends ChargeType {
  override val key: String = "LPP2"
}

case object ITSAReturnAmendment extends ChargeType {
  override val key: String = "IRA"
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

case object PoaOneReconciliationCredit extends CreditType {
  override val key = "POA1RR-credit"
}

case object PoaTwoReconciliationCredit extends CreditType {
  override val key = "POA2RR-credit"
}

case object ITSAReturnAmendmentCredit extends CreditType {
  override val key = "IRA-credit"
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
    case PoaOneReconciliationCredit.key => PoaOneReconciliationCredit
    case PoaTwoReconciliationCredit.key => PoaTwoReconciliationCredit
    case PaymentType.key => PaymentType
    case Repayment.key => Repayment
    case PoaOneDebit.key => PoaOneDebit
    case PoaTwoDebit.key => PoaTwoDebit
    case PoaOneReconciliationDebit.key => PoaOneReconciliationDebit
    case PoaTwoReconciliationDebit.key => PoaTwoReconciliationDebit
    case ITSAReturnAmendmentCredit.key => ITSAReturnAmendmentCredit
    case BalancingCharge.key => BalancingCharge
    case LateSubmissionPenalty.key => LateSubmissionPenalty
    case FirstLatePaymentPenalty.key => FirstLatePaymentPenalty
    case SecondLatePaymentPenalty.key => SecondLatePaymentPenalty
    case ITSAReturnAmendment.key => ITSAReturnAmendment
    case MfaDebitCharge.key => MfaDebitCharge
  }

  implicit val format: Format[TransactionType] = Format( read, write)

  private lazy val codeToTransactionType: Map[String, TransactionType] = {
    val fixed = Map[String, TransactionType](
      ChargeType.poaOneDebit                -> PoaOneDebit,
      ChargeType.poaTwoDebit                -> PoaTwoDebit,
      ChargeType.balancingCharge            -> BalancingCharge,
      ChargeType.poaOneReconciliationDebit  -> PoaOneReconciliationDebit,
      ChargeType.poaTwoReconciliationDebit  -> PoaTwoReconciliationDebit,
      ChargeType.lateSubmissionPenalty      -> LateSubmissionPenalty,
      ChargeType.poaOneReconciliationCredit -> PoaOneReconciliationCredit,
      ChargeType.poaTwoReconciliationCredit -> PoaTwoReconciliationCredit,
      ChargeType.itsaReturnAmendment        -> ITSAReturnAmendment,
      CreditType.cutOver                    -> CutOverCreditType,
      CreditType.balancingCharge            -> BalancingChargeCreditType,
      CreditType.repaymentInterest          -> RepaymentInterest,
      CreditType.itsaReturnAmendmentCredit  -> ITSAReturnAmendmentCredit
    )
    val penalties1 = ChargeType.firstLatePaymentPenalty.map(_ -> FirstLatePaymentPenalty)
    val penalties2 = ChargeType.secondLatePaymentPenalty.map(_ -> SecondLatePaymentPenalty)
    val mfaDebits  = ChargeType.mfaDebit.map(_                  -> MfaDebitCharge)
    val mfaCredits = CreditType.mfaCredit.map(_                 -> MfaCreditType)
    val payments   = CreditType.payment.map(_                   -> PaymentType)

    fixed ++ penalties1 ++ penalties2 ++ mfaDebits ++ mfaCredits ++ payments
  }

  def fromCode(mainTransaction: String): Option[TransactionType] =
    codeToTransactionType.get(mainTransaction)
}

object ChargeType {

  //The following are mainTransaction values:
  // values come from EPID #1138
  val balancingCharge = "4910"

  lazy val poaOneReconciliationDebit = "4911"
  lazy val poaTwoReconciliationDebit = "4913"

  lazy val poaOneReconciliationCredit = "4912"
  lazy val poaTwoReconciliationCredit = "4914"

  val poaOneDebit = "4920"
  val poaTwoDebit = "4930"

  val lateSubmissionPenalty = "4027"
  val firstLatePaymentPenalty: List[String] = List("4028", "4031")
  val secondLatePaymentPenalty: List[String] = List("4029", "4032")

  val itsaReturnAmendment = "4915"

  val mfaDebit = Range.inclusive(4000, 4003)
    .map(_.toString).toList



  implicit val write: Writes[ChargeType] = new Writes[ChargeType] {
    def writes(transactionType: ChargeType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[ChargeType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case PoaOneDebit.key => PoaOneDebit
    case PoaTwoDebit.key => PoaTwoDebit
    case PoaOneReconciliationDebit.key => PoaOneReconciliationDebit
    case PoaTwoReconciliationDebit.key => PoaTwoReconciliationDebit
    case BalancingCharge.key => BalancingCharge
    case LateSubmissionPenalty.key => LateSubmissionPenalty
    case FirstLatePaymentPenalty.key => FirstLatePaymentPenalty
    case SecondLatePaymentPenalty.key => SecondLatePaymentPenalty
    case ITSAReturnAmendment.key => ITSAReturnAmendment
    case MfaDebitCharge.key => MfaDebitCharge
  }

  implicit val format: Format[ChargeType] = Format( read, write)

}

object CreditType {

  // values come from EPID #1138
  val cutOver = "6110"
  val balancingCharge = "4905"
  val repaymentInterest = "6020"
  val mfaCredit = Range.inclusive(4004, 4025)
    .filterNot(_ == 4010).filterNot(_ == 4020).map(_.toString)
    .toList

  val itsaReturnAmendmentCredit = "4916"

  val payment = List("0060")

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
    case PoaOneReconciliationCredit.key => PoaOneReconciliationCredit
    case PoaTwoReconciliationCredit.key => PoaTwoReconciliationCredit
    case ITSAReturnAmendmentCredit.key => ITSAReturnAmendmentCredit
    case PaymentType.key => PaymentType
    case Repayment.key => Repayment
  }

  implicit val format: Format[CreditType] = Format( read, write)

}

