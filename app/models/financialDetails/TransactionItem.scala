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

  def notCodedOutPoa: Boolean = {
    transactionType match {
      case PaymentOnAccountOne | PaymentOnAccountTwo if amountCodedOut.getOrElse[BigDecimal](0) > 0 => false
      case _ => true
    }
  }

  // TODO: duplicate logic, in scope of => https://jira.tools.tax.service.gov.uk/browse/MISUV-8557
  def getChargeTypeKey(codedOutEnabled: Boolean = false, reviewAndReconcileEnabled: Boolean = false): String =
    (transactionType, subTransactionType) match {
      case (PaymentOnAccountOne, _) => "paymentOnAccount1.text"
      case (PaymentOnAccountTwo, _) => "paymentOnAccount2.text"
      case (MfaDebitCharge, _) => "hmrcAdjustment.text"
      case (BalancingCharge, Some(Nics2)) if codedOutEnabled => "class2Nic.text"
      case (BalancingCharge, Some(Accepted)) if codedOutEnabled => "codingOut.text"
      case (BalancingCharge, Some(Cancelled)) if codedOutEnabled => "cancelledPayeSelfAssessment.text"
      case (BalancingCharge, _) => "balancingCharge.text"
      case (PaymentOnAccountOneReviewAndReconcile, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa1.text"
      case (PaymentOnAccountTwoReviewAndReconcile, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa2.text"
      case (PaymentOnAccountOneReviewAndReconcileCredit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa1Credit.text"
      case (PaymentOnAccountTwoReviewAndReconcileCredit, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa2Credit.text"
      case error =>
        Logger("application").error(s"Missing or non-matching charge type: $error found")
        "unknownCharge"
    }
}
