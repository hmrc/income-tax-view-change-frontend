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
import play.api.libs.json.{Format, Json}
import services.{DateServiceInterface}

import java.time.LocalDate

sealed trait FinancialDetailsResponseModel

case class FinancialDetailsModel(balanceDetails: BalanceDetails,
                                 documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {

  def getDueDateFor(documentDetail: DocumentDetail): Option[LocalDate] = {
    if (documentDetail.isLatePaymentInterest) {
      documentDetail.interestEndDate
    } else {
      financialDetails.find { fd =>
        fd.transactionId.contains(documentDetail.transactionId) &&
          fd.taxYear.toInt == documentDetail.taxYear
      } flatMap (_.items.flatMap(_.headOption.flatMap(_.dueDate)))
    }
  }

  def getDueDateForFinancialDetail(financialDetail: FinancialDetail): Option[LocalDate] = {
    financialDetail.items.flatMap(_.headOption.flatMap(_.dueDate))
  }

  def getAllDueDates: List[LocalDate] = {
    documentDetails.flatMap(getDueDateFor)
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

  def findDocumentDetailForTaxYear(taxYear: Int): Option[DocumentDetail] = documentDetails.find(_.taxYear == taxYear)

  def findDueDateByDocumentDetails(documentDetail: DocumentDetail): Option[LocalDate] = {
    financialDetails.find { fd =>
      fd.transactionId.contains(documentDetail.transactionId) &&
        fd.taxYear.toInt == documentDetail.taxYear
    } flatMap (_.items.flatMap(_.headOption.flatMap(_.dueDate)))
  }

  def findDocumentDetailForYearWithDueDate(taxYear: Int)(implicit dateService: DateServiceInterface): Option[DocumentDetailWithDueDate] = {
    findDocumentDetailForTaxYear(taxYear)
      .map(documentDetail => DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail)))
  }

  def findDocumentDetailByIdWithDueDate(id: String)(implicit dateService: DateServiceInterface): Option[DocumentDetailWithDueDate] = {
    documentDetails.find(_.transactionId == id)
      .map(documentDetail => DocumentDetailWithDueDate(
        documentDetail, getDueDateFor(documentDetail), dunningLock = dunningLockExists(documentDetail.transactionId)))
  }

  def getAllDocumentDetailsWithDueDates(codingOutEnabled: Boolean = false)(implicit dateService: DateServiceInterface): List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail =>
      DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail),
        documentDetail.isLatePaymentInterest, dunningLockExists(documentDetail.transactionId),
        codingOutEnabled = codingOutEnabled, isMFADebit = isMFADebit(documentDetail.transactionId)))
  }

  def getAllDocumentDetailsWithDueDatesAndFinancialDetails(codingOutEnabled: Boolean = false)(implicit dateService: DateServiceInterface): List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    documentDetails.map(documentDetail =>
      (DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail),
        documentDetail.isLatePaymentInterest, dunningLockExists(documentDetail.transactionId),
        codingOutEnabled = codingOutEnabled, isMFADebit = isMFADebit(documentDetail.transactionId)),
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
    (documentDetail.documentText, documentDetail.documentDescription) match {
      case (Some(documentText), _) if documentText.contains("Class 2 National Insurance") => true
      case (_, Some("ITSA- POA 1") | Some("ITSA - POA 2") | Some("TRM New Charge") | Some("TRM Amend Charge")) => true
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

  def findMatchingPaymentDocument(paymentLot: String, paymentLotItem: String): Option[DocumentDetail] = {
    documentDetails.find(document => document.paymentLot.contains(paymentLot) && document.paymentLotItem.contains(paymentLotItem))
  }

  def mergeLists(financialDetailsModel: FinancialDetailsModel): FinancialDetailsModel = {
    FinancialDetailsModel(balanceDetails, documentDetails ++ financialDetailsModel.documentDetails,
      financialDetails ++ financialDetailsModel.financialDetails)
  }

}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
