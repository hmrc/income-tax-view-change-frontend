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

// TODO-[1]: make balanceDetails private val -> apply re-design and fix test failures where needed
// TODO-[2]: make financialDetails private val -> ~
case class FinancialDetailsModel(balanceDetails: BalanceDetails,
                                 codingDetails: List[CodingDetails] = List(),
                                 private val documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {

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

  // TODO: to be removed as we should rely on the chargeType available in ChargeItem type;
  def isMFADebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && MfaDebitUtils.isMFADebitMainType(fd.mainType)
    }
  }

  // TODO: we need to identify this on the chargeItem level -> mark as deprecated
  def isReviewAndReconcilePoaOneDebit(documentId: String): Boolean = {
      financialDetails.exists { fd =>
        fd.transactionId.contains(documentId) && isReviewAndReconcilePoaOne(fd.mainTransaction)
      }
  }

  // TODO: we need to identify this on the chargeItem level -> mark as deprecated
  def isReviewAndReconcilePoaTwoDebit(documentId: String): Boolean = {
      financialDetails.exists { fd =>
        fd.transactionId.contains(documentId) && isReviewAndReconcilePoaTwo(fd.mainTransaction)
      }
  }

  // TODO: drop usage of DocumentDetailWithDueDate / and use ChargeItem/TransactionItem instead
  def findDocumentDetailByIdWithDueDate(id: String)(implicit dateService: DateServiceInterface): Option[DocumentDetailWithDueDate] = {
    documentDetails.find(_.transactionId == id)
      .map(documentDetail => DocumentDetailWithDueDate(
        documentDetail, documentDetail.getDueDate(), dunningLock = dunningLockExists(documentDetail.transactionId)))
  }

  // TODO: method possibly is not required at all: TaxYearSummaryController -> withTaxYearFinancials
  def findDueDateByDocumentDetails(documentDetail: DocumentDetail): Option[LocalDate] = {
    financialDetails.find { fd =>
      fd.transactionId.contains(documentDetail.transactionId) &&
        fd.taxYear.toInt == documentDetail.taxYear
    } flatMap (_ => documentDetail.documentDueDate)
  }

  // TODO: drop usage of DocumentDetailWithDueDate / and use ChargeItem/TransactionItem instead
  def getAllDocumentDetailsWithDueDates()(implicit dateService: DateServiceInterface): List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail =>
      DocumentDetailWithDueDate(documentDetail, documentDetail.getDueDate(),
        documentDetail.isAccruingInterest, dunningLockExists(documentDetail.transactionId),
        isMFADebit = isMFADebit(documentDetail.transactionId),
        isReviewAndReconcilePoaOneDebit = isReviewAndReconcilePoaOneDebit(documentDetail.transactionId),
        isReviewAndReconcilePoaTwoDebit = isReviewAndReconcilePoaTwoDebit(documentDetail.transactionId)))
  }

  def filterPayments(): FinancialDetailsModel = {
    val filteredDocuments = documentDetails.filter(document => document.paymentLot.isDefined && document.paymentLotItem.isDefined)
    FinancialDetailsModel(
      balanceDetails,
      codingDetails,
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
    FinancialDetailsModel(balanceDetails, codingDetails, documentDetails ++ financialDetailsModel.documentDetails,
      financialDetails ++ financialDetailsModel.financialDetails)
  }

  def documentDetailsWithLpiId(chargeReference: Option[String]): Option[DocumentDetail] = {
    documentDetails.find(_.latePaymentInterestId == chargeReference)
  }

  def arePoaPaymentsPresent(): Option[TaxYear] = {
    documentDetails.filter(_.documentDescription.exists(description => poaDocumentDescriptions.contains(description)))
      .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
  }

  def unpaidDocumentDetails(): List[DocumentDetail] = {
    this.documentDetails.collect {
      case documentDetail: DocumentDetail if documentDetail.isCodingOutDocumentDetail => documentDetail
      case documentDetail: DocumentDetail if documentDetail.accruingInterestAmount.isDefined && !documentDetail.interestIsPaid => documentDetail
      case documentDetail: DocumentDetail if documentDetail.interestOutstandingAmount.isDefined && !documentDetail.interestIsPaid => documentDetail
      case documentDetail: DocumentDetail if documentDetail.isNotCodingOutDocumentDetail && !documentDetail.isPaid => documentDetail
    }
  }

  def docDetailsNotDueWithInterest(currentDate: LocalDate): Int = {
    this.documentDetails.count(x => !x.isPaid && x.hasAccruingInterest && x.documentDueDate.getOrElse(LocalDate.MIN).isAfter(currentDate))
  }

  /////////////////////////// to ChargeItem conversion methods: START //////////////////////////////////////////////////
  // TODO: we might need a single conversion method instead
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def asChargeItems: List[ChargeItem] = {
    documentDetails.map(documentDetail =>
      Try {
        ChargeItem.fromDocumentPair(documentDetail = documentDetail,
          financialDetails = financialDetails
            .filter(_.transactionId.isDefined)
            .filter(_.transactionId.get == documentDetail.transactionId))
      } match {
        case Success(res) =>
          Some(res)
        case Failure(ex) =>
          Logger("application").warn(s"Failed conversion - asChargeItems - ${ex.getMessage}")
          None
      }
    ).flatMap(x => x.map(y => List(y)).getOrElse(List[ChargeItem]()))
  }

  def toChargeItem: List[ChargeItem] = {
    Try {
      this.documentDetails
        .map(documentDetail =>
          ChargeItem.fromDocumentPair(documentDetail, financialDetails)
        )
    } match {
      case Success(res) =>
        res
      case Failure(ex) =>
        Logger("application").warn(s"Failed conversion - toChargeItem - ${ex.getMessage}")
        List[ChargeItem]()
    }
  }
  /////////////////////////// to ChargeItem conversion method: END /////////////////////////////////////////////////////

}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]

  def getDueDateForFinancialDetail(financialDetail: FinancialDetail): Option[LocalDate] = {
    financialDetail.items.flatMap(_.headOption.flatMap(_.dueDate))
  }
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
