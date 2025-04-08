/*
 * Copyright 2023 HM Revenue & Customs
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

package views.helpers

import models.chargeSummary.ChargeSummaryViewModel
import models.financialDetails.{BalancingCharge, ChargeItem, FirstLatePaymentPenalty, LateSubmissionPenalty, Nics2, PoaOneDebit, PoaTwoDebit, TransactionType}
import play.api.i18n.Messages


object ChargeNameLangHelper {

  def apply(chargeItem: ChargeItem)(implicit messages: Messages): String = {

    (chargeItem.transactionType, chargeItem.subTransactionType) match {
      case (PoaOneDebit, _)                   => messages("yourSelfAssessmentChargeSummary.paymentOnAccount1.heading")
      case (PoaTwoDebit,_)                    => messages("yourSelfAssessmentChargeSummary.paymentOnAccount2.heading")
      case (BalancingCharge, Some(Nics2))     => messages("yourSelfAssessmentChargeSummary.class2Nic.heading")
      case (BalancingCharge, _)               => messages("yourSelfAssessmentChargeSummary.balancingPayment.heading")
      case (LateSubmissionPenalty, _)         => messages("yourSelfAssessmentChargeSummary.lateSubmissionPenalty.heading")
      case (FirstLatePaymentPenalty, _)       => messages("yourSelfAssessmentChargeSummary.firstLatePaymentPenalty.heading")
      case _                                  => messages("yourSelfAssessmentChargeSummary.unknown.heading")
    }
  }

  def chargeHistoryHeading(chargeItem: ChargeItem): String = {
    (chargeItem.transactionType, chargeItem.subTransactionType) match {
      case (PoaOneDebit, _)               => "chargeHistory.paymentOnAccount1.heading"
      case (PoaTwoDebit, _)               => "chargeHistory.paymentOnAccount2.heading"
      case (BalancingCharge, Some(Nics2)) => "chargeHistory.unknown.heading"
      case (BalancingCharge, _)           => "chargeHistory.balancingPayment.heading"
      case (LateSubmissionPenalty, _)     => "chargeHistory.lateSubmissionPenalty.heading"
      case (FirstLatePaymentPenalty, _)   => "chargeHistory.firstLatePaymentPenalty.heading"
      case _                              => "chargeHistory.unknown.heading"
    }
  }
}
