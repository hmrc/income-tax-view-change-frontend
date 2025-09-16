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

import exceptions.{CouldNotCreateChargeItemException, MissingFieldException}
import models.financialDetails.ChargeType.{poaOneReconciliationDebit, poaTwoReconciliationDebit}
import models.financialDetails.YourSelfAssessmentChargesViewModel.getDisplayDueDate
import models.incomeSourceDetails.TaxYear
import play.api.libs.json.{Format, Json}
import services.DateServiceInterface

import java.time.LocalDate

case class ChargeItem (
                        transactionId: String,
                        taxYear: TaxYear,
                        transactionType: TransactionType,
                        codedOutStatus: Option[CodedOutStatusType],
                        documentDate: LocalDate,
                        dueDate: Option[LocalDate],
                        originalAmount: BigDecimal,
                        outstandingAmount: BigDecimal,
                        interestOutstandingAmount: Option[BigDecimal],
                        accruingInterestAmount: Option[BigDecimal],
                        interestFromDate: Option[LocalDate],
                        interestEndDate: Option[LocalDate],
                        interestRate: Option[BigDecimal],
                        lpiWithDunningLock: Option[BigDecimal],
                        amountCodedOut: Option[BigDecimal],
                        dunningLock: Boolean,
                        poaRelevantAmount: Option[BigDecimal],
                        dueDateForFinancialDetail: Option[LocalDate] = None,
                        paymentLotItem: Option[String] = None,
                        paymentLot: Option[String] = None,
                        creationDate: Option[LocalDate] = None,
                        chargeReference: Option[String]
                      ) extends TransactionItem {

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean =
    dueDate.exists(_ isBefore dateService.getCurrentDate)

  // => TODO: to clarify / raise with the BA / why we have two way of identifying credits charge?
  val isCredit = originalAmount < 0

  def credit: Option[BigDecimal] = originalAmount match {
    case _ if (paymentLotItem.isDefined && paymentLot.isDefined) => None
    case _ if (originalAmount >= 0) => None
    case credit => Some(credit * -1)
  }
  // <=

  val hasLpiWithDunningLock: Boolean =
    lpiWithDunningLock.isDefined && lpiWithDunningLock.getOrElse[BigDecimal](0) > 0

  def hasAccruingInterest: Boolean =
    interestOutstandingAmount.isDefined && accruingInterestAmount.getOrElse[BigDecimal](0) <= 0

  def isNotPaidAndNotOverduePoaReconciliationDebit()(implicit dateService: DateServiceInterface): Boolean = {
    Seq(PoaOneReconciliationDebit, PoaTwoReconciliationDebit).contains(transactionType) && !isPaid && !isOverdue()
  }

  def getDueDateForNonZeroBalancingCharge: Option[LocalDate] = {
    if(transactionType == BalancingCharge && (codedOutStatus.isEmpty) && originalAmount == 0.0) {
      None
    } else {
      dueDate
    }
  }

  def getDueDate: LocalDate = dueDate.getOrElse(throw MissingFieldException("documentDueDate"))

  def getInterestFromDate: LocalDate = interestFromDate.getOrElse(throw MissingFieldException("documentInterestFromDate"))

  def getInterestEndDate: LocalDate = interestEndDate.getOrElse(throw MissingFieldException("documentInterestEndDate"))

  def getInterestRate: BigDecimal = interestRate.getOrElse(throw MissingFieldException("documentInterestRate"))

  def getInterestOutstandingAmount: BigDecimal = interestOutstandingAmount.getOrElse(throw MissingFieldException("documentInterestOutstandingAmount"))

  def getAmountCodedOut: BigDecimal = amountCodedOut.getOrElse(throw MissingFieldException("documentAmountCodedOut"))

  def isPaid: Boolean = outstandingAmount match {
    case amount if amount == 0 => true
    case _ => false
  }

  val isAccruingInterest: Boolean = accruingInterestAmount match {
    case Some(amount) if amount <= 0 => false
    case Some(_) => true
    case _ => false
  }

  def isOnlyInterest(implicit dateService: DateServiceInterface): Boolean = {(isOverdue() && isAccruingInterest) || (interestRemainingToPay > 0 && isPaid)}

  def remainingToPayOnCharge(implicit dateServiceInterface: DateServiceInterface): BigDecimal =
    if (isOnlyInterest) interestRemainingToPay
    else remainingToPay

  def getChargeDueDate: LocalDate =
    if (isAccruingInterest && isPaid) getInterestEndDate
    else getDueDate

  def isCodingOut: Boolean = {
    val codingOutSubTypes = Seq(Accepted, Cancelled, FullyCollected)
    codedOutStatus.exists(subType => codingOutSubTypes.contains(subType))
  }

  def isCodingOutAcceptedOrFullyCollected: Boolean = {
    val codingOutSubTypes = Seq(Accepted, FullyCollected)
    codedOutStatus.exists(subType => codingOutSubTypes.contains(subType))
  }

  def interestIsPaid: Boolean = interestOutstandingAmount.exists(_ <= 0)

  def remainingToPay: BigDecimal = {
    if (isPaid) BigDecimal(0)
    else outstandingAmount
  }

  def remainingToPayByChargeOrInterest: BigDecimal = {
    if (isAccruingInterest) interestRemainingToPay
    else remainingToPay
  }

  def remainingToPayByChargeOrInterestWhenChargeIsPaid: Boolean = {
    if (isAccruingInterest || !interestIsPaid) true
    else false
  }

  // this method is used to filter charges down to those currently allowed for the
  // new Your Self Assessment Charge Summary feature
  def isIncludedInSACSummary: Boolean = {

    val validCharge = (transactionType, codedOutStatus) match {
      case (BalancingCharge, Some(Nics2)) => true
      case (BalancingCharge, Some(Accepted)) => true
      case (BalancingCharge, None       ) => true
      case (PoaOneDebit, None           ) => true
      case (PoaTwoDebit, None           ) => true
      case (LateSubmissionPenalty,     _) => true
      case (FirstLatePaymentPenalty,   _) => true
      case _                              => false
    }

    validCharge && !isAccruingInterest
  }

  val isPoaDebit: Boolean = transactionType == PoaOneDebit || transactionType == PoaTwoDebit
  val isPartPaid: Boolean = outstandingAmount != originalAmount

  val interestIsPartPaid: Boolean = interestOutstandingAmount.getOrElse[BigDecimal](0) != accruingInterestAmount.getOrElse[BigDecimal](0)

  val isPoaReconciliationCredit: Boolean = transactionType == PoaOneReconciliationCredit ||
    transactionType == PoaTwoReconciliationCredit

  val isPoaReconciliationDebit: Boolean = transactionType == PoaOneReconciliationDebit ||
    transactionType == PoaTwoReconciliationDebit

  val isPoaDebit: Boolean = transactionType == PoaOneDebit || transactionType == PoaTwoDebit

  val isReviewAndReconcileCharge: Boolean = isPoaReconciliationCredit || isPoaReconciliationDebit

  val isBalancingCharge: Boolean = transactionType == BalancingCharge

  val isPenalty: Boolean = List(LateSubmissionPenalty, FirstLatePaymentPenalty, SecondLatePaymentPenalty).contains(this.transactionType)

  val isLPP2: Boolean = transactionType == SecondLatePaymentPenalty

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
    else interestOutstandingAmount.getOrElse(accruingInterestAmount.getOrElse(0))
  }

  def checkIfEitherChargeOrLpiHasRemainingToPay: Boolean = {
    if (isAccruingInterest) interestRemainingToPay > 0
    else remainingToPay > 0
  }

  def poaLinkForDrilldownPage: String = transactionType match {
    case PoaOneDebit => poaOneReconciliationDebit
    case PoaTwoDebit => poaTwoReconciliationDebit
    case _ => "no valid case"
  }
}

object ChargeItem {

  implicit val format: Format[ChargeItem] = Json.format[ChargeItem]

  def filterAllowedCharges(isChargeTypeEnabled: Boolean, chargeType: ChargeType*)
                          (chargeItem: TransactionItem): Boolean = {
    (isChargeTypeEnabled, chargeItem.transactionType) match {
      case (false, transactionType) if chargeType.toList.contains(transactionType) => false
      case _ => true
    }
  }

  def overdueOrAccruingInterestChargeList(whatYouOweChargesList: WhatYouOweChargesList)(implicit dateServiceInterface: DateServiceInterface): List[ChargeItem] = whatYouOweChargesList.chargesList.filter(x => x.isOverdue() || x.hasAccruingInterest)

  def chargesDueWithin30DaysList(whatYouOweChargesList: WhatYouOweChargesList)(implicit dateService: DateServiceInterface): List[ChargeItem] = whatYouOweChargesList.chargesList.filter(x => !x.isOverdue() && !x.hasAccruingInterest && dateService.isWithin30Days(x.dueDate.getOrElse(LocalDate.MAX)))

  def chargesDueAfter30DaysList(whatYouOweChargesList: WhatYouOweChargesList)(implicit dateService: DateServiceInterface): List[ChargeItem] = whatYouOweChargesList.chargesList.filter(x => !x.isOverdue() && !x.hasAccruingInterest && !dateService.isWithin30Days(x.dueDate.getOrElse(LocalDate.MAX)))


  def sortedOverdueOrAccruingInterestChargeList(whatYouOweChargesList: WhatYouOweChargesList)(implicit dateServiceInterface: DateServiceInterface): List[ChargeItem] = overdueOrAccruingInterestChargeList(whatYouOweChargesList).sortWith((charge1, charge2) =>
    getDisplayDueDate(charge2).isAfter(getDisplayDueDate(charge1))
  )

  private val validChargeTypes = List(PoaOneDebit, PoaTwoDebit, PoaOneReconciliationDebit, PoaTwoReconciliationDebit,
    BalancingCharge, LateSubmissionPenalty, FirstLatePaymentPenalty, SecondLatePaymentPenalty, MfaDebitCharge)

  val isAKnownTypeOfCharge: ChargeItem => Boolean = chargeItem => {
    (chargeItem.transactionType, chargeItem.codedOutStatus) match {
      case (_, Some(Nics2)) => true
      case (x, _) if validChargeTypes.contains(x) => true
      case (_, _) => false
    }
  }

  def fromDocumentPair(documentDetail: DocumentDetail, financialDetails: List[FinancialDetail]): ChargeItem = {

    val financialDetail = financialDetails.find(_.transactionId.contains(documentDetail.transactionId)) match {
      case Some(fd) => fd
      case _ => throw CouldNotCreateChargeItemException(s"Financial detail is not defined for charge ${documentDetail.transactionId}")
    }

    val mainTransaction = financialDetail.mainTransaction match {
      case Some(mt) => mt
      case _ => throw CouldNotCreateChargeItemException(s"Main transaction is not defined for charge ${documentDetail.transactionId}")
    }

    val chargeType = TransactionType.fromCode(mainTransaction) match {
      case Some(ct) => ct
      case _ => throw CouldNotCreateChargeItemException(s"Could not identify charge type from $mainTransaction for charge ${documentDetail.transactionId}")
    }

    val dunningLockExists =
      financialDetails.exists(financialDetail => financialDetail.transactionId.contains(documentDetail.transactionId) && financialDetail.dunningLockExists)

    val codedOutStatus = financialDetail.items.getOrElse(List()).find(_.codedOutStatus.isDefined).flatMap(_.codedOutStatus)

    ChargeItem(
      transactionId = documentDetail.transactionId,
      taxYear = TaxYear.forYearEnd(documentDetail.taxYear),
      transactionType = chargeType,
      codedOutStatus = CodedOutStatusType.fromCodedOutStatusAndDocumentText(documentDetail.documentText, codedOutStatus),
      documentDate = documentDetail.documentDate,
      dueDate = documentDetail.documentDueDate,
      originalAmount = documentDetail.originalAmount,
      outstandingAmount = documentDetail.outstandingAmount,
      interestOutstandingAmount = documentDetail.interestOutstandingAmount,
      accruingInterestAmount = documentDetail.accruingInterestAmount,
      interestFromDate = documentDetail.interestFromDate,
      interestEndDate = documentDetail.interestEndDate,
      interestRate = documentDetail.interestRate,
      lpiWithDunningLock = documentDetail.lpiWithDunningLock,
      amountCodedOut = documentDetail.amountCodedOut,
      dunningLock = dunningLockExists,
      poaRelevantAmount = documentDetail.poaRelevantAmount,
      dueDateForFinancialDetail = FinancialDetailsModel.getDueDateForFinancialDetail(financialDetail),
      paymentLotItem = documentDetail.paymentLotItem,
      paymentLot = documentDetail.paymentLot,
      chargeReference = financialDetail.chargeReference
    )
  }

}