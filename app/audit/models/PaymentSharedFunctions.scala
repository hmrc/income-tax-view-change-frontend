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

package audit.models

import models.financialDetails.{Accepted, BalancingCharge, Cancelled, MfaDebitCharge, Nics2, PoaOneDebit, PoaTwoDebit, TransactionItem}
import services.DateServiceInterface

trait PaymentSharedFunctions {

  def isLatePaymentCharge(chargeItem: TransactionItem)(implicit dateService: DateServiceInterface): Boolean = {
    chargeItem.isOverdue() && chargeItem.isLatePaymentInterest
  }

  def getChargeType(docDetail: TransactionItem, latePaymentCharge: Boolean): Option[String] =
    (docDetail.transactionType, docDetail.codedOutStatus) match {
      case (MfaDebitCharge, _)        => Some("MFADebit")
      case (_, Some(Nics2))           => Some("Class 2 National Insurance")
      case (_, Some(Cancelled))       => Some("Cancelled PAYE Self Assessment (through your PAYE tax code)")
      case (_, Some(Accepted))        => Some("Balancing payment collected through PAYE tax code")
      case (PoaOneDebit,  _)  => if (latePaymentCharge) Some("Late payment interest on first payment on account") else Some("First payment on account")
      case (PoaTwoDebit,  _)  => if (latePaymentCharge) Some("Late payment interest on second payment on account") else Some("Second payment on account")
      case (BalancingCharge, None )   => if (latePaymentCharge) Some("Late payment interest for remaining balance") else Some("Remaining balance")
      case (_, _)                     => Some(docDetail.transactionType.key)
    }

}
