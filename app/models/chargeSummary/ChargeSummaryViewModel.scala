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
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel}
import models.financialDetails._
import play.twirl.api.Html
import controllers.routes._
import play.api.i18n.Messages

import java.time.LocalDate

case class ChargeSummaryViewModel(
                                   currentDate: LocalDate,
                                   chargeItem: ChargeItem,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   latePaymentInterestCharge: Boolean,
                                   penaltiesEnabled: Boolean,
                                   reviewAndReconcileCredit: Option[ChargeItem],
                                   isAgent: Boolean = false,
                                   btaNavPartial: Option[Html] = None,
                                   origin: Option[String] = None,
                                   gatewayPage: Option[GatewayPage] = None,
                                   adjustmentHistory: AdjustmentHistoryModel,
                                   poaExtraChargeLink: Option[String] = None,
                                   poaOneChargeUrl: String,
                                   poaTwoChargeUrl: String,
                                   LSPUrl: String,
                                   LPPUrl: String
                                 )(implicit messages: Messages) {

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

  val taxYearFrom = chargeItem.taxYear.startYear
  val taxYearTo = chargeItem.taxYear.endYear

  val taxYearFromBCD = chargeItem.taxYear.previousYear.startYear
  val taxYearToBCD = chargeItem.taxYear.previousYear.endYear

  val taxYearFromCodingOut = s"${chargeItem.taxYear.nextYear.startYear}"
  val taxYearToCodingOut = s"${chargeItem.taxYear.nextYear.endYear}"

  val taxYearEndToCheckCode = currentTaxYearEnd + 1

  val messagePrefix = if(latePaymentInterestCharge)"lpi."
  else ""
  val pageTitle: String =
    s"chargeSummary.$messagePrefix${chargeItem.getChargeTypeKey}"

  val isBalancingChargeZero: Boolean = chargeItem.transactionType match {
    case _ if chargeItem.codedOutStatus.isDefined => false
    case BalancingCharge if chargeItem.originalAmount == 0 => true
    case _ => false
  }

  val codingOutEnabledAndIsClass2NicWithNoIsPayeSelfAssessment: Boolean =
    chargeItem.codedOutStatus.contains(Nics2)

  val chargeHistoryEnabledOrPaymentAllocationWithNoIsBalancingChargeZeroAndIsNotCredit: Boolean =
    (chargeHistoryEnabled || paymentAllocations.nonEmpty) && !isBalancingChargeZero && !isCredit

  val isCodedOut: Boolean = chargeItem.codedOutStatus.exists(Seq(Accepted, FullyCollected).contains)

  val noInterestChargeAndNoCodingOutEnabledWithIsPayeSelfAssessment: Boolean = !latePaymentInterestCharge && !isCodedOut

  val chargeHistoriesWithDates: List[ChargeHistoryItem] =
    for {
      adjustment <- adjustmentHistory.adjustments
      if chargeHistoryEnabled
    } yield {
       ChargeHistoryItem(
         date = adjustment.adjustmentDate.getOrElse(chargeItem.documentDate),
         description = Html(messages(s"chargeSummary.chargeHistory.${adjustment.reasonCode}.${chargeItem.getChargeTypeKey}", taxYearFromCodingOut, taxYearToCodingOut)),
         amount = adjustment.amount
       )
    }

  val paymentAllocationsWithDates: List[ChargeHistoryItem] =
    for {
      allocation <- paymentAllocations
      payment <- allocation.allocations
      if !latePaymentInterestCharge
  } yield {
      ChargeHistoryItem(
        date = payment.getDueDateOrThrow,
        description = getPaymentAllocationDescription(payment, allocation),
        amount = payment.getAmountOrThrow.abs
      )
    }

  def getPaymentAllocationDescription(payment: PaymentHistoryAllocation, allocation: PaymentHistoryAllocations): Html = {

    val matchingPayment = payment.clearingId

    matchingPayment.fold(
      Html(
        messages(allocation.getPaymentAllocationTextInChargeSummary, taxYearFromCodingOut, taxYearToCodingOut)
      )
    ) { paymentId =>

      val link = {
        if (isAgent) PaymentAllocationsController.viewPaymentAllocationAgent(paymentId)
        else         PaymentAllocationsController.viewPaymentAllocation(paymentId)
      }.url

      val linkText =
        if (chargeItem.transactionType == MfaDebitCharge)
          messages("chargeSummary.paymentAllocations.mfaDebit")
        else
          messages(allocation.getPaymentAllocationTextInChargeSummary, taxYearFromCodingOut, taxYearToCodingOut)

      Html(
        s"""
           |<a class="govuk-link" href="$link">
           |  $linkText
           |  <span class="govuk-visually-hidden">$taxYearTo</span>
           |</a>
           |""".stripMargin
      )
    }
  }

  val sortedChargeHistoriesAndPaymentAllocationsWithDates: List[ChargeHistoryItem] = {
    chargeHistoriesWithDates ++ paymentAllocationsWithDates
  }.sortBy(_.date)

}


