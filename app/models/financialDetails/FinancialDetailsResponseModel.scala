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

import auth.MtdItUser
import enums.{Poa1Charge, Poa1ReconciliationDebit, Poa2Charge, Poa2ReconciliationDebit, TRMAmendCharge, TRMNewCharge}
import models.chargeSummary.{PaymentHistoryAllocation, PaymentHistoryAllocations}
import models.financialDetails.ReviewAndReconcileUtils.{isReviewAndReconcilePoaOne, isReviewAndReconcilePoaTwo}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import play.api.libs.json.{Format, Json}
import services.DateServiceInterface
import services.claimToAdjustPoa.ClaimToAdjustHelper.poaDocumentDescriptions

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

sealed trait FinancialDetailsResponseModel

case class FinancialDetailsModel(balanceDetails: BalanceDetails,
                                 private val documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {

  def getDueDateForFinancialDetail(financialDetail: FinancialDetail): Option[LocalDate] = {
    financialDetail.items.flatMap(_.headOption.flatMap(_.dueDate))
  }

  def getAllDueDates: List[LocalDate] = {
    documentDetails.flatMap(_.getDueDate())
  }

  def dunningLockExists(documentId: String): Boolean = {
    documentDetails.filter(_.transactionId == documentId)
      .exists { documentDetail =>
        financialDetails.exists(financialDetail => financialDetail.transactionId.contains(documentDetail.transactionId) && financialDetail.dunningLockExists)
      }
  }

  def dunningLockExists: Boolean = {
    documentDetails.exists { documentDetail =>
      financialDetails.exists(financialDetail => financialDetail.transactionId.contains(documentDetail.transactionId) && financialDetail.dunningLockExists)
    }
  }

  def isMFADebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && MfaDebitUtils.isMFADebitMainType(fd.mainType)
    }
  }

  def isReviewAndReconcilePoaOneDebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && isReviewAndReconcilePoaOne(fd.mainTransaction)
    }
  }

  def isReviewAndReconcilePoaTwoDebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && isReviewAndReconcilePoaTwo(fd.mainTransaction)
    }
  }

  def isReviewAndReconcilePoaOneDebit(documentId: String, reviewAndReconcileIsEnabled: Boolean): Boolean = {
    reviewAndReconcileIsEnabled &&
      financialDetails.exists { fd =>
        fd.transactionId.contains(documentId) && isReviewAndReconcilePoaOne(fd.mainTransaction)
      }
  }

  def isReviewAndReconcilePoaTwoDebit(documentId: String, reviewAndReconcileIsEnabled: Boolean): Boolean = {
    reviewAndReconcileIsEnabled &&
      financialDetails.exists { fd =>
        fd.transactionId.contains(documentId) && isReviewAndReconcilePoaTwo(fd.mainTransaction)
      }
  }

  def isReviewAndReconcileDebit(documentId: String): Boolean = {
    isReviewAndReconcilePoaOneDebit(documentId) ||
      isReviewAndReconcilePoaTwoDebit(documentId)
  }

  def findDocumentDetailForTaxYear(taxYear: Int): Option[DocumentDetail] = documentDetails.find(_.taxYear == taxYear)



  def findDocumentDetailForYearWithDueDate(taxYear: Int)(implicit dateService: DateServiceInterface): Option[DocumentDetailWithDueDate] = {
    findDocumentDetailForTaxYear(taxYear)
      .map(documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.getDueDate()))
  }

  def findDocumentDetailByIdWithDueDate(id: String)(implicit dateService: DateServiceInterface): Option[DocumentDetailWithDueDate] = {
    documentDetails.find(_.transactionId == id)
      .map(documentDetail => DocumentDetailWithDueDate(
        documentDetail, documentDetail.getDueDate(), dunningLock = dunningLockExists(documentDetail.transactionId)))
  }

  def findDueDateByDocumentDetails(documentDetail: DocumentDetail): Option[LocalDate] = {
    financialDetails.find { fd =>
      fd.transactionId.contains(documentDetail.transactionId) &&
        fd.taxYear.toInt == documentDetail.taxYear
    } flatMap (_ => documentDetail.documentDueDate)
  }


  def getAllDocumentDetailsWithDueDates(reviewAndReconcileEnabled: Boolean = false)(implicit dateService: DateServiceInterface): List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail =>
      DocumentDetailWithDueDate(documentDetail, documentDetail.getDueDate(),
        documentDetail.isLatePaymentInterest, dunningLockExists(documentDetail.transactionId),
        isMFADebit = isMFADebit(documentDetail.transactionId),
        isReviewAndReconcilePoaOneDebit = isReviewAndReconcilePoaOneDebit(documentDetail.transactionId, reviewAndReconcileEnabled),
        isReviewAndReconcilePoaTwoDebit = isReviewAndReconcilePoaTwoDebit(documentDetail.transactionId, reviewAndReconcileEnabled)))
  }

  def getAllDocumentDetailsWithDueDatesAndFinancialDetails()(implicit dateService: DateServiceInterface): List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    documentDetails.map(documentDetail =>
      (DocumentDetailWithDueDate(documentDetail, documentDetail.getDueDate(),
        documentDetail.isLatePaymentInterest, dunningLockExists(documentDetail.transactionId),
        isMFADebit = isMFADebit(documentDetail.transactionId)),
        financialDetails.find(_.transactionId.get == documentDetail.transactionId)
          .getOrElse(throw new Exception("no financialDetail found for documentDetail" + documentDetail)))
    )
  }

  def getPairedDocumentDetails(): List[(DocumentDetail, FinancialDetail)] = {
    documentDetails.map(documentDetail =>
      (documentDetail, financialDetails.find(_.transactionId.get == documentDetail.transactionId)
        .getOrElse(throw new Exception("no financialDetail found for documentDetail" + documentDetail)))
    )
  }


  def isAllPaid()(implicit user: MtdItUser[_]): Boolean = documentDetails.forall(_.isPaid)

  def isAllInterestPaid()(implicit user: MtdItUser[_]): Boolean = documentDetails.forall(_.interestIsPaid)

  def validChargeTypeCondition: DocumentDetail => Boolean = documentDetail => {
    (documentDetail.documentText, documentDetail.getDocType) match {
      case (Some(documentText), _) if documentText.contains("Class 2 National Insurance") => true
      case (_, Poa1Charge | Poa2Charge | Poa1ReconciliationDebit | Poa2ReconciliationDebit | TRMNewCharge | TRMAmendCharge) => true
      case (_, _) => false
    }
  }

  def validChargesWithRemainingToPay: FinancialDetailsModel = {
    val filteredDocuments = documentDetails.filterNot(document => document.paymentLot.isDefined && document.paymentLotItem.isDefined)
      .filter(documentDetail => documentDetail.documentDescription.isDefined && documentDetail.checkIfEitherChargeOrLpiHasRemainingToPay
        && validChargeTypeCondition(documentDetail)).filterNot(_.isPayeSelfAssessment)

    FinancialDetailsModel(
      balanceDetails,
      filteredDocuments,
      financialDetails.filter(financial => filteredDocuments.map(_.transactionId).contains(financial.transactionId.get))
    )
  }

  def filterPayments(): FinancialDetailsModel = {
    val filteredDocuments = documentDetails.filter(document => document.paymentLot.isDefined && document.paymentLotItem.isDefined)
    FinancialDetailsModel(
      balanceDetails,
      filteredDocuments,
      financialDetails.filter(financial => filteredDocuments.map(_.transactionId).contains(financial.transactionId.get))
    )
  }

  // TODO: Update to support allocated credits
  def getAllocationsToCharge(charge: FinancialDetail): Option[PaymentHistoryAllocations] = {

    def hasDocumentDetailForPayment(transactionId: String): Boolean = {
      documentDetails
        .find(_.transactionId == transactionId)
        .exists(documentDetail => {
          documentDetail.paymentLot.isDefined && documentDetail.paymentLotItem.isDefined
        })
    }

    def findIdOfClearingPayment(clearingSAPDocument: Option[String]): Option[String] = {

      def hasMatchingSapCode(subItem: SubItem): Boolean = clearingSAPDocument.exists(id => subItem.clearingSAPDocument.contains(id))

      financialDetails
        .filter(_.transactionId.exists(id => hasDocumentDetailForPayment(id)))
        .find(_.items.exists(_.exists(hasMatchingSapCode)))
        .flatMap(_.transactionId)
    }

    charge.items
      .map { subItems =>
        subItems.collect {
            case subItem if subItem.clearingSAPDocument.isDefined =>
              PaymentHistoryAllocation(
                dueDate = subItem.dueDate,
                amount = subItem.amount,
                clearingSAPDocument = subItem.clearingSAPDocument,
                clearingId = findIdOfClearingPayment(subItem.clearingSAPDocument))
          }
          // only return payments for now
          .filter(_.clearingId.exists(id => hasDocumentDetailForPayment(id)))
      }
      .collect {
        case payments if payments.nonEmpty => PaymentHistoryAllocations(payments, charge.mainType, charge.chargeType)
      }
  }

  def mergeLists(financialDetailsModel: FinancialDetailsModel): FinancialDetailsModel = {
    FinancialDetailsModel(balanceDetails, documentDetails ++ financialDetailsModel.documentDetails,
      financialDetails ++ financialDetailsModel.financialDetails)
  }

  def documentDetailsExist(id: String): Boolean = {
    documentDetails.exists(_.transactionId == id)
  }

  def documentDetailsFilter(predicate: DocumentDetail => Boolean): Option[DocumentDetail] = {
    this.documentDetails.find(predicate)
  }

  def documentDetailsFilterByTaxYear(taxYear: Int): List[DocumentDetail] = {
    this.documentDetails.filter(_.taxYear == taxYear)
  }

  def documentDetailsWithTransactionId(id: String): Option[DocumentDetail] = {
    documentDetails.find(_.transactionId == id)
  }

  def documentDetailsWithLpiId(chargeReference: Option[String]): Option[DocumentDetail] = {
    documentDetails.find(_.latePaymentInterestId == chargeReference)
  }

  def arePoaPaymentsPresent(): Option[TaxYear] = {
    documentDetails.filter(_.documentDescription.exists(description => poaDocumentDescriptions.contains(description)))
      .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
  }

  def toChargeItem(): List[ChargeItem] = {
    Try {
      this.documentDetails
        .map( documentDetail =>
          ChargeItem.fromDocumentPair(documentDetail, financialDetails)
        )
    } match {
      case Success(res) =>
        res
      case Failure(ex) =>
        Logger("application").warn(ex.getMessage)
        List[ChargeItem]()
    }
  }

  def unpaidDocumentDetails(): List[DocumentDetail] = {
    this.documentDetails.collect {
      case documentDetail: DocumentDetail if documentDetail.isCodingOutDocumentDetail => documentDetail
      case documentDetail: DocumentDetail if documentDetail.latePaymentInterestAmount.isDefined && !documentDetail.interestIsPaid => documentDetail
      case documentDetail: DocumentDetail if documentDetail.interestOutstandingAmount.isDefined && !documentDetail.interestIsPaid => documentDetail
      case documentDetail: DocumentDetail if documentDetail.isNotCodingOutDocumentDetail && !documentDetail.isPaid => documentDetail
    }
  }

  def docDetailsNotDueWithInterest(currentDate: LocalDate): List[DocumentDetail] = {
    this.documentDetails.filter(
      x => !x.isPaid && x.hasAccruingInterest && x.documentDueDate.getOrElse(LocalDate.MIN).isAfter(currentDate)
    )
  }

}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
