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

package returns.models

import common.exceptions.CouldNotCreateChargeItemException
import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class ChargeItem(
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
                       latePaymentInterestAmount: Option[BigDecimal],
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
    dueDate.exists(_.isBefore(dateService.getCurrentDate))
  
  val isAccruingInterest: Boolean = accruingInterestAmount.exists(_ > 0)
  
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
      latePaymentInterestAmount = documentDetail.latePaymentInterestAmount,
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