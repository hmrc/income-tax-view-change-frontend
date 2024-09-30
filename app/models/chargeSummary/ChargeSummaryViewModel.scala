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

import enums.GatewayPage._
import models.chargeHistory.AdjustmentHistoryModel
import models.financialDetails._
import play.twirl.api.Html

import java.time.LocalDate

case class ChargeSummaryViewModel(
                                   currentDate: LocalDate,
                                   chargeItem: ChargeItem,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   paymentAllocationEnabled: Boolean,
                                   latePaymentInterestCharge: Boolean,
                                   otherInterestCharge: Boolean,
                                   codingOutEnabled: Boolean,
                                   reviewAndReconcileEnabled: Boolean,
                                   isAgent: Boolean = false,
                                   btaNavPartial: Option[Html] = None,
                                   origin: Option[String] = None,
                                   gatewayPage: Option[GatewayPage] = None,
                                   adjustmentHistory: AdjustmentHistoryModel,
                                   poaOneChargeUrl: String,
                                   poaTwoChargeUrl: String
                                 ) {

  val dueDate = chargeItem.dueDate
  val hasDunningLocks = paymentBreakdown.exists(_.dunningLockExists)
  val hasInterestLocks = paymentBreakdown.exists(_.interestLockExists)
  val hasAccruedInterest = paymentBreakdown.exists(_.hasAccruedInterest)
  val isInterestCharge = latePaymentInterestCharge || otherInterestCharge

  val currentTaxYearEnd = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val whatYouOweUrl = {
    if (isAgent) controllers.routes.WhatYouOweController.showAgent.url
    else controllers.routes.WhatYouOweController.show(origin).url
  }

  val hasPaymentBreakdown = {
    chargeItem.hasLpiWithDunningLock || (paymentBreakdown.nonEmpty && hasDunningLocks) || (paymentBreakdown.nonEmpty && hasInterestLocks)
  }

  val taxYearFrom = chargeItem.taxYear - 1
  val taxYearTo = chargeItem.taxYear

  val taxYearFromCodingOut = s"${chargeItem.taxYear.toInt + 1}"
  val taxYearToCodingOut = s"${chargeItem.taxYear.toInt + 2}"

  val messagePrefix = if(latePaymentInterestCharge)"lpi."
  else if (otherInterestCharge) "interest."
  else ""
  val pageTitle: String =
    s"chargeSummary.$messagePrefix${chargeItem.getChargeTypeKey(codingOutEnabled, reviewAndReconcileEnabled)}"

//  val pageTitle: String = {
//    val key = (latePaymentInterestCharge, otherInterestCharge, isMFADebit, isReviewAndReconcilePoaOneDebit, isReviewAndReconcilePoaTwoDebit) match {
//      case (true, false, false, false, false) => s"chargeSummary.lpi.${documentDetail.getChargeTypeKey()}"
//      case (false, false, true, false, false) => s"chargeSummary.hmrcAdjustment.text"
//      case (false, false, false, true, false) => s"chargeSummary.paymentOnAccount1.extraAmount.text"
//      case (false, false, false, false, true) => s"chargeSummary.paymentOnAccount2.extraAmount.text"
//      case (false, true, false, true, false) => s"chargeSummary.poa1ExtraChargeInterest.text"
//      case (false, true, false, false, true) => s"chargeSummary.poa2ExtraChargeInterest.text"
//      case (_, _, _, _, _) => s"chargeSummary.${documentDetail.getChargeTypeKey(codingOutEnabled)}"
//    }
//    key
//  }

  val isBalancingChargeZero: Boolean = chargeItem.transactionType match {
    case _ if codingOutEnabled && chargeItem.subTransactionType.isDefined => false
    case BalancingCharge if chargeItem.originalAmount == 0 => true
    case _ => false
  }

  val codingOutEnabledAndIsClass2NicWithNoIsPayeSelfAssessment: Boolean =
    codingOutEnabled && chargeItem.subTransactionType.contains(Nics2)

  val remainingToPayWithNoCodingOutEnabledAndIsPayeSelfAssessment: Boolean = chargeItem.remainingToPay > 0 &&
    !(codingOutEnabled && chargeItem.subTransactionType.contains(Accepted)) &&
    !Seq(PaymentOnAccountOneReviewAndReconcile, PaymentOnAccountTwoReviewAndReconcile).contains(chargeItem.transactionType)

  val chargeHistoryEnabledOrPaymentAllocationWithNoIsBalancingChargeZero: Boolean =
    (chargeHistoryEnabled || (paymentAllocationEnabled && paymentAllocations.nonEmpty)) && !isBalancingChargeZero

  val noInterestChargeAndNoCodingOutEnabledWithIsPayeSelfAssessment: Boolean = !latePaymentInterestCharge && !otherInterestCharge && !(codingOutEnabled && chargeItem.subTransactionType.contains(Accepted))

}


