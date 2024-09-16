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
import services.DateServiceInterface

import java.time.LocalDate

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
  lpiWithDunningLock: Option[BigDecimal],
  isOverdue: Boolean) extends TransactionItem {


  val hasLpiWithDunningLock: Boolean =
    lpiWithDunningLock.isDefined && lpiWithDunningLock.getOrElse[BigDecimal](0) > 0

  def getDueDate: Option[LocalDate] = {
    dueDate.filterNot(_ => originalAmount == 0.0)
  }

  def getBalancingChargeDueDate(codedOutEnabled: Boolean = false): Option[LocalDate] = {
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

  def interestIsPaid: Boolean = interestOutstandingAmount match {
    case Some(amount) if amount == 0 => true
    case _ => false
  }

  def remainingToPay: BigDecimal = {
    if (isPaid) BigDecimal(0)
    else outstandingAmount
  }

  def checkIsPaid(isInterestCharge: Boolean): Boolean = {
    if (isInterestCharge) interestIsPaid
    else isPaid
  }
  def interestRemainingToPay: BigDecimal = {
    if (interestIsPaid) BigDecimal(0)
    else interestOutstandingAmount.getOrElse(latePaymentInterestAmount.get)
  }

  def getChargeTypeKey(codedOutEnabled: Boolean = false): String = (transactionType, subTransactionType) match {
    case (PaymentOnAccountOne, _) => "paymentOnAccount1.text"
    case (PaymentOnAccountTwo, _) => "paymentOnAccount2.text"
    case (MfaDebitCharge,      _) => "hmrcAdjustment.text"
    case (BalancingCharge, Some(Nics2)) if codedOutEnabled => "class2Nic.text"
    case (BalancingCharge, Some(Accepted)) if codedOutEnabled => "codingOut.text"
    case (BalancingCharge, Some(Cancelled)) if codedOutEnabled => "cancelledPayeSelfAssessment.text"
    case (BalancingCharge, _) => "balancingCharge.text"
    case error =>
      Logger("application").error(s"Missing or non-matching charge type: $error found")
      "unknownCharge"
  }
}

object ChargeItem {

  def fromDocumentPair(documentDetail: DocumentDetail, financialDetailOpt: Option[FinancialDetail], codingOut: Boolean)
                      (implicit dateService: DateServiceInterface): ChargeItem = {

    val isOverdue: Boolean = documentDetail.documentDueDate.exists(_ isBefore dateService.getCurrentDate)

    val financialDetail = financialDetailOpt match {
      case Some(fd) => fd
      case _ => throw CouldNotCreateChargeItem(s"Financial detail is not defined for charge ${documentDetail.transactionId}")
    }

    val mainTransaction = financialDetail.mainTransaction match {
      case Some(mt) => mt
      case _ => throw CouldNotCreateChargeItem(s"Main transaction is not defined for charge ${documentDetail.transactionId}")
    }

    val chargeType = ChargeType.fromCode(mainTransaction) match {
      case Some(ct) => ct
      case _ => throw CouldNotCreateChargeItem(s"Could not identify charge type from $mainTransaction for charge ${documentDetail.transactionId}")
    }

    ChargeItem(
      transactionId = documentDetail.transactionId,
      taxYear = documentDetail.taxYear,
      transactionType = chargeType,
      subTransactionType = documentDetail.documentText
        .flatMap(SubTransactionType.fromDocumentText).filter(_ => codingOut),
      documentDate = documentDetail.documentDate,
      dueDate = documentDetail.documentDueDate,
      originalAmount = documentDetail.originalAmount,
      outstandingAmount = documentDetail.outstandingAmount,
      interestOutstandingAmount = documentDetail.interestOutstandingAmount,
      latePaymentInterestAmount = documentDetail.latePaymentInterestAmount,
      interestFromDate = documentDetail.interestFromDate,
      interestEndDate = documentDetail.interestEndDate,
      lpiWithDunningLock = documentDetail.lpiWithDunningLock,
      isOverdue = isOverdue
    )
  }

  def fromFinancialDetailModel(transactionId: String, financialDetailsModel: FinancialDetailsModel, codingOut: Boolean)
                              (implicit dateService: DateServiceInterface): Option[ChargeItem] = {


    for {
      dd <- financialDetailsModel.documentDetails.find(_.transactionId == transactionId)
      fd <- financialDetailsModel.financialDetails.find(_.transactionId.contains(transactionId))
      mainTransaction <- fd.mainTransaction
      chargeType <- ChargeType.fromCode(mainTransaction)
    } yield {

      val isOverdue: Boolean = dd.documentDueDate.exists(_ isBefore dateService.getCurrentDate)

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
        lpiWithDunningLock = dd.lpiWithDunningLock,
        isOverdue = isOverdue
      )
    }
  }

}

trait CreditItem extends TransactionItem {

  override val transactionType: CreditType

}