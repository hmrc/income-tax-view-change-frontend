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

package financials.models

import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import play.api.Logging
import shared.enums.ChargeClassificationType
import shared.enums.ChargeClassificationType.*

trait TransactionItem extends Logging {

  val transactionId: String

  val transactionType: TransactionType

  val codedOutStatus: Option[CodedOutStatusType]

  val taxYear: TaxYear

  val outstandingAmount: BigDecimal

  val isAccruingInterest: Boolean

  val amountCodedOut: Option[BigDecimal]

  val chargeClassification: Option[String]

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean

  def notCodedOutPoa: Boolean = {
    transactionType match {
      case PoaOneDebit | PoaTwoDebit if amountCodedOut.getOrElse[BigDecimal](0) > 0 => false
      case _ => true
    }
  }

  def getChargeTypeKey: String =
    (transactionType, codedOutStatus, chargeClassification.flatMap(fromString)) match {
      case (PoaOneDebit, Some(Accepted), _)                                        => "poa1CodedOut.text"
      case (PoaOneDebit, Some(FullyCollected), _)                                  => "poa1CodedOut.text"
      case (PoaTwoDebit, Some(Accepted), _)                                        => "poa2CodedOut.text"
      case (PoaTwoDebit, Some(FullyCollected), _)                                  => "poa2CodedOut.text"
      case (PoaOneDebit, Some(Cancelled), _)                                       => "cancelledPayeSelfAssessment.text"
      case (PoaTwoDebit, Some(Cancelled), _)                                       => "cancelledPayeSelfAssessment.text"
      case (PoaOneDebit, _, _)                                                     => "paymentOnAccount1.text"
      case (PoaTwoDebit, _, _)                                                     => "paymentOnAccount2.text"
      case (MfaDebitCharge, _, _)                                                  => "hmrcAdjustment.text"
      case (BalancingCharge, Some(Nics2), _)                                       => "class2Nic.text"
      case (BalancingCharge, Some(Accepted), _)                                    => "codingOut.text"
      case (BalancingCharge, Some(Cancelled), _)                                   => "cancelledPayeSelfAssessment.text"
      case (BalancingCharge, _, _)                                                 => "balancingCharge.text"
      case (PoaOneReconciliationDebit, _, _)                                       => "reviewAndReconcilePoa1.text"
      case (PoaTwoReconciliationDebit, _, _)                                       => "reviewAndReconcilePoa2.text"
      case (PoaOneReconciliationCredit, _, _)                                      => "reviewAndReconcilePoa1Credit.text"
      case (PoaTwoReconciliationCredit, _, _)                                      => "reviewAndReconcilePoa2Credit.text"
      case (LateSubmissionPenalty, _, _)                                           => "lateSubmissionPenalty.text"
      case (FirstLatePaymentPenalty, _, _)                                         => "firstLatePaymentPenalty.text"
      case (SecondLatePaymentPenalty, _, _)                                        => "secondLatePaymentPenalty.text"
      case (ITSAReturnAmendment, _, Some(RevenueAmendments))                       => "enquiryAmendment.text"
      case (ITSAReturnAmendment, _, Some(AutoCorrection | ManualCorrection))       => "hmrcCorrection.text"
      case (ITSAReturnAmendment, _, _)                                             => "itsaReturnAmendment.text"
      case (ITSAReturnAmendmentCredit, _, Some(RevenueAmendments))                 => "enquiryAmendmentCredit.text"
      case (ITSAReturnAmendmentCredit, _, Some(AutoCorrection | ManualCorrection)) => "correctionCredit.text"
      case (ITSAReturnAmendmentCredit, _, _)                                       => "itsaReturnAmendmentCredit.text"
      case error =>
        logger.error(s"Missing or non-matching charge type: $error found")
        "unknownCharge"
    }
}
