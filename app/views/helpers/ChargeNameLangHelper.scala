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
import models.financialDetails.{BalancingCharge, LateSubmissionPenalty, FirstLatePaymentPenalty, PoaOneDebit, PoaTwoDebit, TransactionType}
import play.api.i18n.Messages


object ChargeNameLangHelper {

  def apply(transactionType: TransactionType)(implicit messages: Messages): String = {

    transactionType match {
      case PoaOneDebit     => messages("yourSelfAssessmentChargeSummary.paymentOnAccount1.heading")
      case PoaTwoDebit     => messages("yourSelfAssessmentChargeSummary.paymentOnAccount2.heading")
      case BalancingCharge => messages("yourSelfAssessmentChargeSummary.balancingPayment.heading")
      case LateSubmissionPenalty => messages("yourSelfAssessmentChargeSummary.lateSubmissionPenalty.heading")
      case FirstLatePaymentPenalty => messages("yourSelfAssessmentChargeSummary.firstLatePaymentPenalty.heading")
      case _               => messages("yourSelfAssessmentChargeSummary.unknown.heading")
    }
  }

  def chargeHistoryHeading(viewModel: ChargeSummaryViewModel): String = {
    viewModel.chargeItem.transactionType match {
      case PoaOneDebit     => "chargeHistory.paymentOnAccount1.heading"
      case PoaTwoDebit     => "chargeHistory.paymentOnAccount2.heading"
      case BalancingCharge => "chargeHistory.balancingPayment.heading"
      case LateSubmissionPenalty => "chargeHistory.lateSubmissionPenalty.heading"
      case FirstLatePaymentPenalty => "chargeHistory.firstLatePaymentPenalty.heading"
      case _               => "chargeHistory.unknown.heading"
    }
  }
}
