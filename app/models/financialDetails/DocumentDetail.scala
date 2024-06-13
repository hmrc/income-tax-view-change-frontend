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
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, Reads, Writes, __}
import services.DateServiceInterface

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
    interestOutstandingAmount.isDefined && latePaymentInterestAmount.getOrElse[BigDecimal](0) <= 0

  def originalAmountIsNotNegative: Boolean = originalAmount match {
    case amount if amount < 0 => false
    case _ => true
  }

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

  def remainingToPayByChargeOrLpi: BigDecimal = {
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

  def isCodingOutDocumentDetail(codingOutEnabled: Boolean = false): Boolean =
    (codingOutEnabled, isPayeSelfAssessment, isCancelledPayeSelfAssessment, isClass2Nic) match {
      case (false, _, _, _) => false
      case (true, true, _, _) => true
      case (true, _, true, _) => true
      case (true, _, _, true) => true
      case _ => false
    }

  def isNotCodingOutDocumentDetail: Boolean = !isClass2Nic && !isPayeSelfAssessment && !isCancelledPayeSelfAssessment

  def isClass2Nic: Boolean = documentText match {
    case Some(documentText) if documentText == CODING_OUT_CLASS2_NICS.name => true
    case _ => false
  }

  def isPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case (Some("TRM New Charge") | Some("TRM Amend Charge"), Some(CODING_OUT_ACCEPTED.name)) => true
    case _ => false
  }

  def isCancelledPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case ((Some("TRM New Charge") | Some("TRM Amend Charge")), Some(CODING_OUT_CANCELLED.name)) => true
    case _ => false
  }

  def isBalancingCharge(codedOutEnabled: Boolean = false): Boolean = getChargeTypeKey(codedOutEnabled) == "balancingCharge.text"

  def isBalancingChargeZero(codedOutEnabled: Boolean = false): Boolean = {
    (isBalancingCharge(codedOutEnabled), this.originalAmount) match {
      case (true, value) => value == BigDecimal(0.0)
      case _ => false
    }
  }

  def getBalancingChargeDueDate(codedOutEnabled: Boolean = false): Option[LocalDate] = {
    isBalancingChargeZero(codedOutEnabled) match {
      case true => None
      case _ => documentDueDate
    }
  }

  def getChargeTypeKey(codedOutEnabled: Boolean = false): String = documentDescription match {
    case Some("ITSA- POA 1") => "paymentOnAccount1.text"
    case Some("ITSA - POA 2") => "paymentOnAccount2.text"
    case Some("ITSA BCD") => "balancingCharge.text"
    case Some("TRM New Charge") | Some("TRM Amend Charge") => (codedOutEnabled, isClass2Nic, isPayeSelfAssessment, isCancelledPayeSelfAssessment) match {
      case (true, true, false, false) => "class2Nic.text"
      case (true, false, true, false) => "codingOut.text"
      case (true, false, false, true) => "cancelledPayeSelfAssessment.text"
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

  def isPoa(): Boolean = {
    documentDescription match {
      case Some("ITSA- POA 1") => true
      case Some("ITSA - POA 2") => true
      case None => false
    }
  }

}


case class DocumentDetailWithDueDate(documentDetail: DocumentDetail, dueDate: Option[LocalDate],
                                     isLatePaymentInterest: Boolean = false, dunningLock: Boolean = false,
                                     codingOutEnabled: Boolean = false, isMFADebit: Boolean = false)(implicit val dateService: DateServiceInterface) {

  val isOverdue: Boolean = documentDetail.documentDueDate.exists(_ isBefore dateService.getCurrentDate)
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
