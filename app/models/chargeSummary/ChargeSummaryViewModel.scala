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
import models.ChargeHistoryItem
import models.chargeHistory.AdjustmentHistoryModel
import models.financialDetails._
import play.api.i18n.Messages
import play.twirl.api.Html
import controllers.routes._
import models.repaymentHistory.RepaymentHistoryUtils

import java.time.LocalDate

case class ChargeSummaryViewModel(
                                   currentDate: LocalDate,
                                   chargeItem: ChargeItem,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   creditsRefundRepayEnabled: Boolean = true,
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

  val taxYearFromCodingOut = s"${chargeItem.taxYear.addYears(2).startYear}"
  val taxYearToCodingOut = s"${chargeItem.taxYear.addYears(2).endYear}"

  val messagePrefix = if (latePaymentInterestCharge) "lpi."
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

  val creationEventNoInterestChargeAndNotCodedOut: Option[ChargeHistoryItem] = Option.when(chargeHistoryEnabled && noInterestChargeAndNoCodingOutEnabledWithIsPayeSelfAssessment) {
      ChargeHistoryItem(
        date = adjustmentHistory.creationEvent.adjustmentDate.getOrElse(chargeItem.documentDate),
        description = Html(messages(s"chargeSummary.chargeHistory.created.${chargeItem.getChargeTypeKey}")),
        amount = adjustmentHistory.creationEvent.amount
      )
    }

  val creationEventCodedOut: Option[ChargeHistoryItem] = Option.when(chargeHistoryEnabled && isCodedOut) {
      ChargeHistoryItem(
        date = adjustmentHistory.creationEvent.adjustmentDate.getOrElse(chargeItem.documentDate),
        description = Html(messages(s"chargeSummary.chargeHistory.created.${chargeItem.getChargeTypeKey}",taxYearFromCodingOut, taxYearToCodingOut)),
        amount = adjustmentHistory.creationEvent.amount
      )
    }

  val chargeItemList: Option[ChargeHistoryItem] = Option.when(chargeHistoryEnabled && latePaymentInterestCharge) {
    ChargeHistoryItem(
      date = chargeItem.interestEndDate.get,
      description = Html(messages(s"chargeSummary.lpi.chargeHistory.created.${chargeItem.getChargeTypeKey}")),
      amount = chargeItem.accruingInterestAmount.get
    )
  }

  val reviewAndReconcileCreditList: Option[ChargeHistoryItem] = reviewAndReconcileCredit.collect {
      case charge if chargeHistoryEnabled =>
        ChargeHistoryItem(
          date = charge.getDueDate,
          description = getReviewAndReconcileCreditDescription(charge),
          amount = charge.originalAmount.abs
        )
    }

  private def getReviewAndReconcileCreditDescription(charge: ChargeItem): Html = {
    val link: String = RepaymentHistoryUtils.getChargeLinkUrl(isAgent, charge.taxYear.endYear, charge.transactionId)
    val linkText: String = messages(s"chargeSummary.chargeHistory.${charge.transactionType.key}")
    Html(
      s"""
         |<a class="govuk-link" id="rar-charge-link" href="$link">
         |    $linkText
         |</a>
         |""".stripMargin
    )
  }

  val chargeHistoriesFormattedList: List[ChargeHistoryItem] = for {
    adjustment <- adjustmentHistory.adjustments
    if chargeHistoryEnabled
  } yield {
    ChargeHistoryItem(
      date = adjustment.adjustmentDate.getOrElse(chargeItem.documentDate),
      description = Html(messages(s"chargeSummary.chargeHistory.${adjustment.reasonCode}.${chargeItem.getChargeTypeKey}", taxYearFromCodingOut, taxYearToCodingOut)),
      amount = adjustment.amount
    )
  }

  val paymentAllocationsFormattedList: List[ChargeHistoryItem] = for {
    allocation <- paymentAllocations
    payment <- allocation.allocations
    if !latePaymentInterestCharge
  } yield {
    ChargeHistoryItem(
      date = payment.getDueDateOrThrow,
      description = getPaymentAllocationDescription(allocation, payment),
      amount = payment.getAmountOrThrow.abs)
  }

  private def getPaymentAllocationDescription(allocation: PaymentHistoryAllocations, payment: PaymentHistoryAllocation): Html = {
    val matchingPayment = payment.clearingId
    matchingPayment match {
      case Some(paymentId) => {
        val link: String = (isAgent, payment.isCutoverCredit) match {
          case (true, false) => PaymentAllocationsController.viewPaymentAllocationAgent(paymentId).url
          case (false, false) => PaymentAllocationsController.viewPaymentAllocation(paymentId, origin).url
          case _ => ""
        }

        val linkText: String = if (chargeItem.transactionType == MfaDebitCharge) messages("chargeSummary.paymentAllocations.mfaDebit")
        else if (payment.isCutoverCredit) messages("paymentHistory.cutOver")
        else messages(allocation.getPaymentAllocationTextInChargeSummary, taxYearFromCodingOut, taxYearToCodingOut)
        if (payment.isCutoverCredit) {
          Html(
            s"""
               |<p class="govuk-body">
               |    $linkText
               |    <span class="govuk-visually-hidden">${chargeItem.taxYear.endYear}</span>
               |</p>
            """.stripMargin)
        } else {
          Html(
            s"""
               |<a class="govuk-link" href="$link">
               |    $linkText
               |    <span class="govuk-visually-hidden">${chargeItem.taxYear.endYear}</span>
               |</a>
             """.stripMargin)
        }
      }
      case None => {
        Html(messages(allocation.getPaymentAllocationTextInChargeSummary, taxYearFromCodingOut, taxYearToCodingOut))
      }
    }
  }

  val sortedChargeHistoryTableEntries: List[ChargeHistoryItem] = {
      creationEventNoInterestChargeAndNotCodedOut.toList ++
      creationEventCodedOut.toList ++
      reviewAndReconcileCreditList.toList ++
      chargeHistoriesFormattedList ++
      chargeItemList ++
      paymentAllocationsFormattedList
  }.sortBy(_.date)
}


