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

package models.chargeSummary

import enums.DocumentType
import enums.GatewayPage._
import models.chargeHistory.AdjustmentHistoryModel
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel}
import play.twirl.api.Html
import views.html.partials.chargeSummary.{ChargeSummaryHasDunningLocksOrLpiWithDunningLock, ChargeSummaryPaymentAllocation}

import java.time.LocalDate

case class ChargeSummaryViewModel(
                                   currentDate: LocalDate,
                                   documentDetailWithDueDate: DocumentDetailWithDueDate,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   paymentAllocationEnabled: Boolean,
                                   latePaymentInterestCharge: Boolean,
                                   codingOutEnabled: Boolean,
                                   isAgent: Boolean = false,
                                   btaNavPartial: Option[Html] = None,
                                   origin: Option[String] = None,
                                   gatewayPage: Option[GatewayPage] = None,
                                   isMFADebit: Boolean,
                                   isReviewAndReconcilePoaOneDebit: Boolean,
                                   isReviewAndReconcilePoaTwoDebit: Boolean,
                                   documentType: DocumentType,
                                   adjustmentHistory: AdjustmentHistoryModel
                                 ) {

  val documentDetail: DocumentDetail = documentDetailWithDueDate.documentDetail
  val dueDate = documentDetailWithDueDate.dueDate
  val hasDunningLocks = paymentBreakdown.exists(_.dunningLockExists)
  val hasInterestLocks = paymentBreakdown.exists(_.interestLockExists)
  val hasAccruedInterest = paymentBreakdown.exists(_.hasAccruedInterest)



  val currentTaxYearEnd = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val whatYouOweUrl = {
    if (isAgent) controllers.routes.WhatYouOweController.showAgent.url
    else controllers.routes.WhatYouOweController.show(origin).url
  }

  val hasPaymentBreakdown = {
    documentDetail.hasLpiWithDunningLock || (paymentBreakdown.nonEmpty && hasDunningLocks) || (paymentBreakdown.nonEmpty && hasInterestLocks)
  }

  val taxYearFrom = documentDetail.taxYear - 1
  val taxYearTo = documentDetail.taxYear

  val taxYearFromCodingOut = s"${documentDetail.taxYear.toInt + 1}"
  val taxYearToCodingOut = s"${documentDetail.taxYear.toInt + 2}"

  val pageTitle: String = {
    val key = (latePaymentInterestCharge, isMFADebit, isReviewAndReconcilePoaOneDebit, isReviewAndReconcilePoaTwoDebit) match {
      case (true, false, false, false) => s"chargeSummary.lpi.${documentDetail.getChargeTypeKey()}"
      case (false, true, false, false) => s"chargeSummary.hmrcAdjustment.text"
      case (false, false, true, false) => s"chargeSummary.paymentOnAccount1.extraAmount.text"
      case (false, false, false, true) => s"chargeSummary.paymentOnAccount2.extraAmount.text"
      case (_, _, _, _) => s"chargeSummary.${documentDetail.getChargeTypeKey(codingOutEnabled)}"
    }
    key
  }

  val isBalancingChargeZero = documentDetail.isBalancingChargeZero(codingOutEnabled)
  val codingOutEnabledAndIsClass2NicWithNoIsPayeSelfAssessment: Boolean = codingOutEnabled && documentDetail.isClass2Nic && !documentDetail.isPayeSelfAssessment
  val noIsAgentAndRemainingToPayWithNoCodingOutEnabledAndIsPayeSelfAssessment: Boolean = !isAgent && documentDetail.remainingToPay > 0 && !(codingOutEnabled && documentDetail.isPayeSelfAssessment)
  val isAgentAndRemainingToPayWithNoCodingOutEnabledAndIsPayeSelfAssessment: Boolean = isAgent && documentDetail.remainingToPay > 0 && !(codingOutEnabled && documentDetail.isPayeSelfAssessment)
  val chargeHistoryEnabledOrPaymentAllocationWithNoIsBalancingChargeZero: Boolean = (chargeHistoryEnabled || (paymentAllocationEnabled && paymentAllocations.nonEmpty)) && !isBalancingChargeZero
  val noLatePaymentInterestChargeAndNoCodingOutEnabledWithIsPayeSelfAssessment: Boolean = !latePaymentInterestCharge && !(codingOutEnabled && documentDetail.isPayeSelfAssessment)



}


