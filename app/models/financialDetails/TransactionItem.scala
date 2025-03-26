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

import models.incomeSourceDetails.TaxYear
import play.api.Logger
import services.DateServiceInterface

trait TransactionItem {

  val transactionId: String

  val transactionType: TransactionType

  val subTransactionType: Option[SubTransactionType]

  val taxYear: TaxYear

  val outstandingAmount: BigDecimal

  val isLatePaymentInterest: Boolean

  val amountCodedOut: Option[BigDecimal]

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean

  def notCodedOutPoa(filterCodedOutPoasEnabled: Boolean): Boolean = {
    (filterCodedOutPoasEnabled, transactionType) match {
      case (false, _) => true
      case (_, PoaOneDebit | PoaTwoDebit) if amountCodedOut.getOrElse[BigDecimal](0) > 0 => false
      case _ => true
    }
  }

  // TODO: duplicate logic, in scope of => https://jira.tools.tax.service.gov.uk/browse/MISUV-8557
  def getChargeTypeKey(reviewAndReconcileEnabled: Boolean = false, penaltiesEnabled: Boolean = false): String =
    (transactionType, subTransactionType) match {
      case (PoaOneDebit, _) => "paymentOnAccount1.text"
      case (PoaTwoDebit, _) => "paymentOnAccount2.text"
      case (MfaDebitCharge, _) => "hmrcAdjustment.text"
      case (BalancingCharge, Some(Nics2)) => "class2Nic.text"
      case (BalancingCharge, Some(Accepted)) => "codingOut.text"
      case (BalancingCharge, Some(Cancelled)) => "cancelledPayeSelfAssessment.text"
      case (BalancingCharge, _) => "balancingCharge.text"
      case (PoaOneReconciliationDebit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa1.text"
      case (PoaTwoReconciliationDebit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa2.text"
      case (PoaOneReconciliationCredit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa1Credit.text"
      case (PoaTwoReconciliationCredit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa2Credit.text"
      case (LateSubmissionPenalty, _) if penaltiesEnabled => "lateSubmissionPenalty.text"
      case (FirstLatePaymentPenalty, _) if penaltiesEnabled => "firstLatePaymentPenalty.text"
      case (SecondLatePaymentPenalty, _) if penaltiesEnabled => "secondLatePaymentPenalty.text"
      case error =>
        Logger("application").error(s"Missing or non-matching charge type: $error found")
        "unknownCharge"
    }
}
