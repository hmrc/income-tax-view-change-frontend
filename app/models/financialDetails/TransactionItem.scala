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

import exceptions.CouldNotCreateChargeItem
import play.api.Logger
import play.api.libs.json.{Format, Json}
import services.DateServiceInterface

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

trait TransactionItem {

  val transactionId: String

  val transactionType: TransactionType

  val taxYear: Int
}

case class ChargeItem (
  transactionId: String,
  taxYear: Int,
  transactionType: ChargeType,
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
  dunningLock: Boolean) extends TransactionItem {

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean =
    dueDate.exists(_ isBefore dateService.getCurrentDate)

  val hasLpiWithDunningLock: Boolean =
    lpiWithDunningLock.isDefined && lpiWithDunningLock.getOrElse[BigDecimal](0) > 0

  def hasAccruingInterest: Boolean =
    interestOutstandingAmount.isDefined && latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0

  def isAccruingInterest()(implicit dateService: DateServiceInterface): Boolean = {
    Seq(PaymentOnAccountOneReviewAndReconcile, PaymentOnAccountTwoReviewAndReconcile).contains(transactionType) && !isPaid && !isOverdue()
  }

  def getDueDateForNonZeroBalancingCharge(codedOutEnabled: Boolean = false): Option[LocalDate] = {
    if(transactionType == BalancingCharge && (!codedOutEnabled || subTransactionType.isEmpty) && originalAmount == 0.0) {
      None
    } else {
      dueDate
    }
  }

  def isPaid: Boolean = outstandingAmount match {
    case amount if amount == 0 => true
    case _ => false
  }

  def isLatePaymentInterest: Boolean = latePaymentInterestAmount match {
    case Some(amount) if amount <= 0 => false
    case Some(_) => true
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

  def remainingToPayByChargeOrLpi: BigDecimal = {
    if (isLatePaymentInterest) interestRemainingToPay
    else remainingToPay
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

  def getChargeTypeKey(codedOutEnabled: Boolean = false, reviewAndReconcileEnabled: Boolean = false): String =
    (transactionType, subTransactionType) match {
      case (PaymentOnAccountOne, _)                                 => "paymentOnAccount1.text"
      case (PaymentOnAccountTwo, _)                                 => "paymentOnAccount2.text"
      case (MfaDebitCharge,      _)                                 => "hmrcAdjustment.text"
      case (BalancingCharge, Some(Nics2))     if codedOutEnabled    => "class2Nic.text"
      case (BalancingCharge, Some(Accepted))  if codedOutEnabled    => "codingOut.text"
      case (BalancingCharge, Some(Cancelled)) if codedOutEnabled    => "cancelledPayeSelfAssessment.text"
      case (BalancingCharge, _)                                     => "balancingCharge.text"
      case (PaymentOnAccountOneReviewAndReconcile, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa1.text"
      case (PaymentOnAccountTwoReviewAndReconcile, _) if reviewAndReconcileEnabled => "reviewAndReconcilePoa2.text"
      case error =>
        Logger("application").error(s"Missing or non-matching charge type: $error found")
        "unknownCharge"
  }
}

object ChargeItem {

  implicit val format: Format[ChargeItem] = Json.format[ChargeItem]

  def tryGetChargeItem(codingOutEnabled: Boolean, reviewAndReconcileEnabled: Boolean)
                      (financialDetails: List[FinancialDetail])
                      (documentDetail: DocumentDetail): Option[ChargeItem] = {
    Try(ChargeItem.fromDocumentPair(
      documentDetail,
      financialDetails,
      codingOutEnabled,
      reviewAndReconcileEnabled)
    ) match {
      case Failure(exception) =>
        Logger("application").warn(exception.getMessage)
        None
      case Success(value) if value.isCodingOut && !codingOutEnabled =>
        None
      case Success(value) =>
        Some(value)
    }
  }

  def fromDocumentPair(documentDetail: DocumentDetail, financialDetails: List[FinancialDetail],
                       codingOut: Boolean, reviewAndReconcile: Boolean): ChargeItem = {

    val financialDetail = financialDetails.find(_.transactionId.contains(documentDetail.transactionId)) match {
      case Some(fd) => fd
      case _ => throw CouldNotCreateChargeItem(s"Financial detail is not defined for charge ${documentDetail.transactionId}")
    }

    val mainTransaction = financialDetail.mainTransaction match {
      case Some(mt) => mt
      case _ => throw CouldNotCreateChargeItem(s"Main transaction is not defined for charge ${documentDetail.transactionId}")
    }

    val chargeType = ChargeType.fromCode(mainTransaction, reviewAndReconcile) match {
      case Some(ct) => ct
      case _ => throw CouldNotCreateChargeItem(s"Could not identify charge type from $mainTransaction for charge ${documentDetail.transactionId}")
    }

    val dunningLockExists =
      financialDetails.exists(financialDetail => financialDetail.transactionId.contains(documentDetail.transactionId) && financialDetail.dunningLockExists)

    ChargeItem(
      transactionId = documentDetail.transactionId,
      taxYear = documentDetail.taxYear,
      transactionType = chargeType,
      subTransactionType = documentDetail.documentText
        .flatMap(SubTransactionType.fromDocumentText),
      documentDate = documentDetail.documentDate,
      dueDate = documentDetail.documentDueDate,
      originalAmount = documentDetail.originalAmount,
      outstandingAmount = documentDetail.outstandingAmount,
      interestOutstandingAmount = documentDetail.interestOutstandingAmount,
      latePaymentInterestAmount = documentDetail.latePaymentInterestAmount,
      interestFromDate = documentDetail.interestFromDate,
      interestEndDate = documentDetail.interestEndDate,
      interestRate = documentDetail.interestRate,
      lpiWithDunningLock = documentDetail.lpiWithDunningLock,
      amountCodedOut = documentDetail.amountCodedOut,
      dunningLock = dunningLockExists
    )
  }

  def fromFinancialDetailModel(transactionId: String, financialDetailsModel: FinancialDetailsModel, codingOut: Boolean,
                               reviewAndReconcile: Boolean): Option[ChargeItem] = {

    for {
      dd <- financialDetailsModel.documentDetails.find(_.transactionId == transactionId)
      fd <- financialDetailsModel.financialDetails.find(_.transactionId.contains(transactionId))
      mainTransaction <- fd.mainTransaction
      chargeType <- ChargeType.fromCode(mainTransaction, reviewAndReconcile)
    } yield {

      val dunningLockExists =
        financialDetailsModel.financialDetails
          .exists(financialDetail => financialDetail.transactionId.contains(dd.transactionId) && financialDetail.dunningLockExists)

      ChargeItem(
        transactionId = transactionId,
        taxYear = dd.taxYear,
        transactionType = chargeType,
        subTransactionType = dd.documentText.flatMap(SubTransactionType.fromDocumentText)
          .filter(_ => codingOut),
        documentDate = dd.documentDate,
        dueDate = dd.documentDueDate,
        originalAmount = dd.originalAmount,
        outstandingAmount = dd.outstandingAmount,
        interestOutstandingAmount = dd.interestOutstandingAmount,
        latePaymentInterestAmount = dd.latePaymentInterestAmount,
        interestFromDate = dd.interestFromDate,
        interestEndDate = dd.interestEndDate,
        interestRate = dd.interestRate,
        lpiWithDunningLock = dd.lpiWithDunningLock,
        amountCodedOut = dd.amountCodedOut,
        dunningLock = dunningLockExists
      )
    }
  }

}

trait CreditItem extends TransactionItem {

  override val transactionType: CreditType

}