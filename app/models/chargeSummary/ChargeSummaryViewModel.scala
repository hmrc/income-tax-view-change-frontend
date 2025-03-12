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
                                   whatYouOweUrl: String,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   latePaymentInterestCharge: Boolean,
                                   reviewAndReconcileEnabled: Boolean,
                                   reviewAndReconcileCredit: Option[ChargeItem],
                                   isAgent: Boolean = false,
                                   btaNavPartial: Option[Html] = None,
                                   origin: Option[String] = None,
                                   gatewayPage: Option[GatewayPage] = None,
                                   adjustmentHistory: AdjustmentHistoryModel,
                                   poaExtraChargeLink: Option[String] = None,
                                   poaOneChargeUrl: String,
                                   poaTwoChargeUrl: String
                                 ) {

  val dueDate = chargeItem.dueDate
  val hasDunningLocks = paymentBreakdown.exists(_.dunningLockExists)
  val hasInterestLocks = paymentBreakdown.exists(_.interestLockExists)
  val hasAccruedInterest = paymentBreakdown.exists(_.hasAccruedInterest)

  val isCredit = chargeItem.originalAmount < 0

  val currentTaxYearEnd = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val hasPaymentBreakdown = {
    chargeItem.hasLpiWithDunningLock || (paymentBreakdown.nonEmpty && hasDunningLocks) || (paymentBreakdown.nonEmpty && hasInterestLocks)
  }

  val taxYearFrom = chargeItem.taxYear.startYear.toString
  val taxYearTo = chargeItem.taxYear.endYear.toString

  val taxYearFromCodingOut = s"${chargeItem.taxYear.endYear + 1}"
  val taxYearToCodingOut = s"${chargeItem.taxYear.endYear + 2}"

  val messagePrefix = if(latePaymentInterestCharge)"lpi."
  else ""
  val pageTitle: String =
    s"chargeSummary.$messagePrefix${chargeItem.getChargeTypeKey(reviewAndReconcileEnabled)}"

  val isBalancingChargeZero: Boolean = chargeItem.transactionType match {
    case _ if chargeItem.subTransactionType.isDefined => false
    case BalancingCharge if chargeItem.originalAmount == 0 => true
    case _ => false
  }

  val codingOutEnabledAndIsClass2NicWithNoIsPayeSelfAssessment: Boolean =
    chargeItem.subTransactionType.contains(Nics2)

  val chargeHistoryEnabledOrPaymentAllocationWithNoIsBalancingChargeZeroAndIsNotCredit: Boolean =
    (chargeHistoryEnabled || paymentAllocations.nonEmpty) && !isBalancingChargeZero && !isCredit

  val noInterestChargeAndNoCodingOutEnabledWithIsPayeSelfAssessment: Boolean = !latePaymentInterestCharge && !chargeItem.subTransactionType.contains(Accepted)

  val poaChargeUrl: String =
    if (chargeItem.transactionType.equals(PoaOneReconciliationCredit)) poaOneChargeUrl
    else                                                               poaTwoChargeUrl

  val poaChargeLinkTextMessageKey: String =
    if (chargeItem.transactionType.equals(PoaOneReconciliationCredit)) "chargeSummary.paymentOnAccount1.text"
    else                                                               "chargeSummary.paymentOnAccount2.text"

}


