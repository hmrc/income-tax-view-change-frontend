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

package financials.models.audit

import financials.enums.ChargeClassificationType
import financials.models.*

trait PaymentSharedFunctions {

  def getChargeType(docDetail: TransactionItem, latePaymentCharge: Boolean): Option[String] =
    (docDetail.transactionType, docDetail.codedOutStatus, docDetail.chargeClassification) match {
      case (MfaDebitCharge, _, _)        => Some("MFADebit")
      case (_, Some(Nics2), _)           => Some("Class 2 National Insurance")
      case (_, Some(Cancelled), _)       => Some("Cancelled PAYE Self Assessment (through your PAYE tax code)")
      case (BalancingCharge, Some(Accepted), _)        => Some("Balancing payment collected through PAYE tax code")
      case (PoaOneDebit, Some(Accepted), _)            => Some("First payment on account collected through PAYE tax code")
      case (PoaTwoDebit, Some(Accepted), _)            => Some("Second payment on account collected through PAYE tax code")
      case (PoaOneDebit,  _, _)  => if (latePaymentCharge) Some("Late payment interest on first payment on account") else Some("First payment on account")
      case (PoaTwoDebit,  _, _)  => if (latePaymentCharge) Some("Late payment interest on second payment on account") else Some("Second payment on account")
      case (BalancingCharge, None, _)   => if (latePaymentCharge) Some("Late payment interest for remaining balance") else Some("Remaining balance")
      case (FirstLatePaymentPenalty, _, _) => if (latePaymentCharge) Some("Late payment interest on first late payment penalty") else Some("First late payment penalty")
      case (SecondLatePaymentPenalty, _, _) => if (latePaymentCharge) Some("Late payment interest on second late payment penalty") else Some("Second late payment penalty")
      case (LateSubmissionPenalty, _, _) => if (latePaymentCharge) Some("Late payment interest on late submission penalty") else Some("Late submission penalty")
      case (ITSAReturnAmendment, _, chargeClassification) => if ChargeClassificationType.isRevenueAmendment(chargeClassification) then Some("Extra amount to pay due to HMRC enquiry amendment") 
                                                             else if (latePaymentCharge) Some("Late payment interest on balancing payment: extra amount due to amended return") 
                                                             else Some("Balancing payment: extra amount due to amended return")
      case (PoaOneReconciliationDebit, _, _) => if (latePaymentCharge) Some("Interest for first payment on account: extra amount") else Some("First payment on account: extra amount from your tax return")
      case (PoaTwoReconciliationDebit, _, _) => if (latePaymentCharge) Some("Interest for second payment on account: extra amount") else Some("Second payment on account: extra amount from your tax return")
      case (_, _, _)                     => Some(docDetail.transactionType.key)
    }

}
