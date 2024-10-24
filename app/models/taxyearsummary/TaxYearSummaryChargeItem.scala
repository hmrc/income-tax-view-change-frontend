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

package models.taxyearsummary

import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface

import java.time.LocalDate

object TaxYearSummaryChargeItem {

  def fromChargeItem(chargeItem: ChargeItem): TaxYearSummaryChargeItem = {
    TaxYearSummaryChargeItem(
      transactionId = chargeItem.transactionId,
      taxYear = chargeItem.taxYear,
      transactionType = chargeItem.transactionType,
      subTransactionType = chargeItem.subTransactionType,
      documentDate = chargeItem.documentDate,
      dueDate = chargeItem.dueDate,
      originalAmount = chargeItem.originalAmount,
      outstandingAmount = chargeItem.outstandingAmount,
      interestOutstandingAmount = chargeItem.interestOutstandingAmount,
      latePaymentInterestAmount = chargeItem.latePaymentInterestAmount,
      interestFromDate = chargeItem.interestFromDate,
      interestEndDate = chargeItem.interestEndDate,
      interestRate = chargeItem.interestRate,
      lpiWithDunningLock = chargeItem.lpiWithDunningLock,
      amountCodedOut = chargeItem.amountCodedOut,
      isLatePaymentInterest = chargeItem.isLatePaymentInterest,
      dunningLock = chargeItem.dunningLock
    )
  }

  def fromChargeItem(chargeItem: ChargeItem, dueDate: Option[LocalDate], isLatePaymentInterest: Boolean = false): TaxYearSummaryChargeItem = {
    TaxYearSummaryChargeItem(
      transactionId = chargeItem.transactionId,
      taxYear = chargeItem.taxYear,
      transactionType = chargeItem.transactionType,
      subTransactionType = chargeItem.subTransactionType,
      documentDate = chargeItem.documentDate,
      dueDate = if (dueDate.isDefined) dueDate else chargeItem.dueDate,
      originalAmount = chargeItem.originalAmount,
      outstandingAmount = chargeItem.outstandingAmount,
      interestOutstandingAmount = chargeItem.interestOutstandingAmount,
      latePaymentInterestAmount = chargeItem.latePaymentInterestAmount,
      interestFromDate = chargeItem.interestFromDate,
      interestEndDate = chargeItem.interestEndDate,
      interestRate = chargeItem.interestRate,
      lpiWithDunningLock = chargeItem.lpiWithDunningLock,
      amountCodedOut = chargeItem.amountCodedOut,
      isLatePaymentInterest = isLatePaymentInterest,
      dunningLock = chargeItem.dunningLock
    )
  }

}

case class TaxYearSummaryChargeItem(
                                     transactionId: String,
                                     taxYear: TaxYear,
                                     transactionType: TransactionType,
                                     subTransactionType: Option[SubTransactionType],
                                     documentDate: LocalDate,
                                     dueDate: Option[LocalDate],
                                     originalAmount: BigDecimal,
                                     outstandingAmount: BigDecimal,
                                     interestOutstandingAmount: Option[BigDecimal],
                                     latePaymentInterestAmount: Option[BigDecimal],
                                     interestFromDate: Option[LocalDate],
                                     interestEndDate: Option[LocalDate],
                                     interestRate: Option[BigDecimal],
                                     lpiWithDunningLock: Option[BigDecimal],
                                     amountCodedOut: Option[BigDecimal],
                                     isLatePaymentInterest: Boolean = false,
                                     dunningLock: Boolean) extends TransactionItem {

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean = dueDate.exists(_ isBefore dateService.getCurrentDate)

  val hasLpiWithDunningLock: Boolean =
    lpiWithDunningLock.isDefined && lpiWithDunningLock.getOrElse[BigDecimal](0) > 0

  def hasAccruingInterest: Boolean =
    interestOutstandingAmount.isDefined && latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0

  def isAccruingInterest()(implicit dateService: DateServiceInterface): Boolean = {
    Seq(PaymentOnAccountOneReviewAndReconcile, PaymentOnAccountTwoReviewAndReconcile).contains(transactionType) && !isPaid && !isOverdue()
  }

  def getDueDateForNonZeroBalancingCharge(codedOutEnabled: Boolean = false): Option[LocalDate] = {
    if (transactionType == BalancingCharge && (!codedOutEnabled || subTransactionType.isEmpty) && originalAmount == 0.0) {
      None
    } else {
      dueDate
    }
  }

  def isPaid: Boolean = outstandingAmount match {
    case amount if amount == 0 => true
    case _ => false
  }

  def isCodingOut: Boolean = {
    val codingOutSubTypes = Seq(Nics2, Accepted, Cancelled)
    subTransactionType.exists(subType => codingOutSubTypes.contains(subType))
  }

  def interestIsPaid: Boolean = interestOutstandingAmount match {
    case Some(amount) if amount == 0 => true
    case _ => false
  }

  def remainingToPay: BigDecimal = {
    if (isPaid) BigDecimal(0)
    else outstandingAmount
  }

  val isPartPaid: Boolean = outstandingAmount != originalAmount

  val interestIsPartPaid: Boolean = interestOutstandingAmount.getOrElse[BigDecimal](0) != latePaymentInterestAmount.getOrElse[BigDecimal](0)

  def getInterestPaidStatus: String = {
    if (interestIsPaid) "paid"
    else if (interestIsPartPaid) "part-paid"
    else "unpaid"
  }

  def getChargePaidStatus: String = {
    if (isPaid) "paid"
    else if (isPartPaid) "part-paid"
    else "unpaid"
  }

  def checkIsPaid(isInterestCharge: Boolean): Boolean = {
    if (isInterestCharge) interestIsPaid
    else isPaid
  }

  def interestRemainingToPay: BigDecimal = {
    if (interestIsPaid) BigDecimal(0)
    else interestOutstandingAmount.getOrElse(latePaymentInterestAmount.get)
  }


}
