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

//sealed trait TransactionType {
//  val key: String
//}
//
//case object Payment extends TransactionType {
//  override val key: String = "payment"
//}
//
//sealed trait Charge extends TransactionType
//
//case object PaymentOnAccount1 extends Charge {
//  override val key = "mfa"
//}
//
//case object PaymentOnAccount2 extends Charge {
//  override val key = "mfa"
//}
//
//case object PaymentOnAccount1 extends Charge {
//  override val key = "mfa"
//}

object TransactionType {
//
//  // values come from EPID #1138
//  private val cutOver = "6110"
//  private val balancingCharge = "4905"
//  private val repaymentInterest = "6020"
//  private val mfaCredit = Range.inclusive(4004, 4025)
//    .filterNot(_ == 4010).filterNot(_ == 4020).map(_.toString)
//    .toList
//  private val payment = List("4920", "4930")
//
//  def fromCode(mainTransaction: String): Option[CreditType] = {
//    mainTransaction match {
//      case CreditType.cutOver =>
//        Some(CutOverCreditType)
//      case CreditType.balancingCharge =>
//        Some(BalancingChargeCreditType)
//      case CreditType.repaymentInterest =>
//        Some(RepaymentInterest)
//      case x if mfaCredit.contains(x) =>
//        Some(MfaCreditType)
//      case x if payment.contains(x) =>
//        Some(PaymentType)
//      case _ => None
//    }
//  }
}
