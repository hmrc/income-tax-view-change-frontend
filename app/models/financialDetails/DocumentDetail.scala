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

package models.financialDetails

import enums.CodingOutType._
import enums.{BalancingCharge, DocumentType, OtherCharge, Poa1Charge, Poa1ReconciliationDebit, Poa2Charge, Poa2ReconciliationDebit, TRMAmendCharge, TRMNewCharge}
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, Reads, Writes, __}
import services.DateServiceInterface
import services.claimToAdjustPoa.ClaimToAdjustHelper.{POA1, POA2}

import java.time.LocalDate

case class DocumentDetail(taxYear: Int,
                          transactionId: String,
                          documentDescription: Option[String],
                          documentText: Option[String],
                          outstandingAmount: BigDecimal,
                          originalAmount: BigDecimal,
                          documentDate: LocalDate,
                          interestOutstandingAmount: Option[BigDecimal] = None,
                          interestRate: Option[BigDecimal] = None,
                          latePaymentInterestId: Option[String] = None,
                          interestFromDate: Option[LocalDate] = None,
                          interestEndDate: Option[LocalDate] = None,
                          latePaymentInterestAmount: Option[BigDecimal] = None,
                          lpiWithDunningLock: Option[BigDecimal] = None,
                          paymentLotItem: Option[String] = None,
                          paymentLot: Option[String] = None,
                          effectiveDateOfPayment: Option[LocalDate] = None,
                          amountCodedOut: Option[BigDecimal] = None,
                          documentDueDate: Option[LocalDate] = None,
                          poaRelevantAmount: Option[BigDecimal] = None
                         ) {

  def credit: Option[BigDecimal] = originalAmount match {
    case _ if (paymentLotItem.isDefined && paymentLot.isDefined) => None
    case _ if (originalAmount >= 0) => None
    case credit => Some(credit * -1)
  }

  def paymentOrChargeCredit: Option[BigDecimal] = outstandingAmount match {
    case _ if (outstandingAmount >= 0) => None
    case credit => Some(credit * -1)
  }


  def outstandingAmountZero: Boolean =
    outstandingAmount == 0

  def hasLpiWithDunningLock: Boolean =
    lpiWithDunningLock.isDefined && lpiWithDunningLock.getOrElse[BigDecimal](0) > 0

  def hasAccruingInterest: Boolean =
    interestOutstandingAmount.isDefined && latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0 && !isPaid

  def originalAmountIsNotZeroOrNegative: Boolean = originalAmount match {
    case amount if amount <= 0 => false
    case _ => true
  }

  def isLatePaymentInterest: Boolean = latePaymentInterestAmount match {
    case Some(amount) if amount <= 0 => false
    case Some(_) => true
    case _ => false
  }

  def isPaid: Boolean = outstandingAmount match {
    case amount if amount == 0 => true
    case _ => false
  }

  def interestIsPaid: Boolean = interestOutstandingAmount match {
    case Some(amount) if amount == 0 => true
    case _ => false
  }

  val interestIsPartPaid: Boolean = interestOutstandingAmount.getOrElse[BigDecimal](0) != latePaymentInterestAmount.getOrElse[BigDecimal](0)

  def getInterestPaidStatus: String = {
    if (interestIsPaid) "paid"
    else if (interestIsPartPaid) "part-paid"
    else "unpaid"
  }

  def checkIsPaid(isInterestCharge: Boolean): Boolean = {
    if (isInterestCharge) interestIsPaid
    else isPaid
  }

  val isPartPaid: Boolean = outstandingAmount != originalAmount

  def remainingToPay: BigDecimal = {
    if (isPaid) BigDecimal(0)
    else outstandingAmount
  }

  def remainingToPayByChargeOrInterest: BigDecimal = {
    if (isLatePaymentInterest) interestRemainingToPay
    else remainingToPay
  }

  def interestRemainingToPay: BigDecimal = {
    if (interestIsPaid) BigDecimal(0)
    else interestOutstandingAmount.getOrElse(latePaymentInterestAmount.get)
  }


  def checkIfEitherChargeOrLpiHasRemainingToPay: Boolean = {
    if (isLatePaymentInterest) interestRemainingToPay > 0
    else remainingToPay > 0
  }

  def getChargePaidStatus: String = {
    if (isPaid) "paid"
    else if (isPartPaid) "part-paid"
    else "unpaid"
  }

  def isCodingOutDocumentDetail: Boolean =
    (isPayeSelfAssessment, isCancelledPayeSelfAssessment, isClass2Nic) match {
      case (true, _, _) => true
      case (_, true, _) => true
      case (_, _, true) => true
      case _ => false
    }

  def isNotCodingOutDocumentDetail: Boolean = !isClass2Nic && !isPayeSelfAssessment && !isCancelledPayeSelfAssessment

  def isClass2Nic: Boolean = documentText match {
    case Some(documentText) if documentText == CODING_OUT_CLASS2_NICS.name => true
    case _ => false
  }

  def isPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case (Some(TRMNewCharge.key) | Some(TRMAmendCharge.key), Some(CODING_OUT_ACCEPTED.name)) => true
    case _ => false
  }

  def isCancelledPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case (Some(TRMNewCharge.key) | Some(TRMAmendCharge.key), Some(CODING_OUT_CANCELLED.name)) => true
    case _ => false
  }

  def isBalancingCharge: Boolean = getChargeTypeKey == "balancingCharge.text"

  def isBalancingChargeZero: Boolean = {
    (isBalancingCharge, this.originalAmount) match {
      case (true, value) => value == BigDecimal(0.0)
      case _ => false
    }
  }

  def getBalancingChargeDueDate(codedOutEnabled: Boolean = false): Option[LocalDate] = {
    isBalancingChargeZero match {
      case true => None
      case _ => documentDueDate
    }
  }

  // TODO: duplicate logic, in scope of => https://jira.tools.tax.service.gov.uk/browse/MISUV-8557
  def getChargeTypeKey: String = documentDescription match {
    case Some(Poa1Charge.key) => "paymentOnAccount1.text"
    case Some(Poa2Charge.key) => "paymentOnAccount2.text"
    case Some(Poa1ReconciliationDebit.key) => "poa1ExtraCharge.text"
    case Some(Poa2ReconciliationDebit.key) => "poa2ExtraCharge.text"
    case Some(BalancingCharge.key) => "balancingCharge.text"
    case Some(TRMNewCharge.key) | Some(TRMAmendCharge.key) => (isClass2Nic, isPayeSelfAssessment, isCancelledPayeSelfAssessment) match {
      case (true, false, false) => "class2Nic.text"
      case (false, true, false) => "codingOut.text"
      case (false, false, true) => "cancelledPayeSelfAssessment.text"
      case _ => "balancingCharge.text"
    }
    case error =>
      Logger("application").error(s"Missing or non-matching charge type: $error found")
      "unknownCharge"
  }

  def getDueDate(): Option[LocalDate] = {
    if (isLatePaymentInterest) {
      interestEndDate
    } else {
      documentDueDate
    }
  }

  def getDocType: DocumentType = {
    documentDescription match {
      case Some(Poa1Charge.key) => Poa1Charge
      case Some(Poa2Charge.key) => Poa2Charge
      case Some(Poa1ReconciliationDebit.key) => Poa1ReconciliationDebit
      case Some(Poa2ReconciliationDebit.key) => Poa2ReconciliationDebit
      case Some(BalancingCharge.key) => BalancingCharge
      case Some(TRMNewCharge.key) => TRMNewCharge
      case Some(TRMAmendCharge.key) => TRMAmendCharge
      case _ => OtherCharge
    }
  }

}


case class DocumentDetailWithDueDate(documentDetail: DocumentDetail, dueDate: Option[LocalDate],
                                     isLatePaymentInterest: Boolean = false, dunningLock: Boolean = false,
                                     isMFADebit: Boolean = false,
                                     isReviewAndReconcilePoaOneDebit: Boolean = false,
                                     isReviewAndReconcilePoaTwoDebit: Boolean = false
                                    )(implicit val dateService: DateServiceInterface) {

  val isOverdue: Boolean = documentDetail.documentDueDate.exists(_ isBefore dateService.getCurrentDate)

  def isReviewAndReconcileDebit: Boolean = isReviewAndReconcilePoaOneDebit || isReviewAndReconcilePoaTwoDebit

  def isAccruingInterest: Boolean = {
    isReviewAndReconcileDebit && !documentDetail.isPaid && !isOverdue
  }

  def isOnlyInterest: Boolean = {(isOverdue && isLatePaymentInterest) || (documentDetail.interestRemainingToPay > 0 && documentDetail.isPaid)}
}

object DocumentDetail {
  implicit val writes: Writes[DocumentDetail] = Json.writes[DocumentDetail]
  implicit val reads: Reads[DocumentDetail] = (
    (__ \ "taxYear").read[Int] and
      (__ \ "transactionId").read[String] and
      (__ \ "documentDescription").readNullable[String] and
      (__ \ "documentText").readNullable[String] and
      (__ \ "outstandingAmount").read[BigDecimal] and
      (__ \ "originalAmount").read[BigDecimal] and
      (__ \ "documentDate").read[LocalDate] and
      (__ \ "interestOutstandingAmount").readNullable[BigDecimal] and
      (__ \ "interestRate").readNullable[BigDecimal] and
      (__ \ "latePaymentInterestId").readNullable[String] and
      (__ \ "interestFromDate").readNullable[LocalDate] and
      (__ \ "interestEndDate").readNullable[LocalDate] and
      (__ \ "latePaymentInterestAmount").readNullable[BigDecimal] and
      (__ \ "lpiWithDunningLock").readNullable[BigDecimal] and
      (__ \ "paymentLotItem").readNullable[String] and
      (__ \ "paymentLot").readNullable[String] and
      (__ \ "effectiveDateOfPayment").readNullable[LocalDate] and
      (__ \ "amountCodedOut").readNullable[BigDecimal] and
      (__ \ "documentDueDate").readNullable[LocalDate] and
      (__ \ "poaRelevantAmount").readNullable[BigDecimal]
    )(DocumentDetail.apply _)
}
