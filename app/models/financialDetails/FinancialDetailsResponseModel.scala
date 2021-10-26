/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

sealed trait FinancialDetailsResponseModel

case class FinancialDetailsModel(balanceDetails: BalanceDetails,
                                 documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {

  def getDueDateFor(documentDetail: DocumentDetail): Option[LocalDate] = {
    financialDetails.find { fd =>
      fd.transactionId.contains(documentDetail.transactionId) &&
        fd.taxYear == documentDetail.taxYear
    } flatMap (_.items.flatMap(_.headOption.flatMap(_.dueDate))) map LocalDate.parse
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

  def findDocumentDetailForTaxYear(taxYear: Int): Option[DocumentDetail] = documentDetails.find(_.taxYear.toInt == taxYear)

  def findDocumentDetailForYearWithDueDate(taxYear: Int): Option[DocumentDetailWithDueDate] = {
    findDocumentDetailForTaxYear(taxYear)
      .map(documentDetail => DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail)))
  }

  def findDocumentDetailByIdWithDueDate(id: String): Option[DocumentDetailWithDueDate] = {
    documentDetails.find(_.transactionId == id)
      .map(documentDetail => DocumentDetailWithDueDate(
        documentDetail, getDueDateFor(documentDetail), dunningLock = dunningLockExists(documentDetail.transactionId)))
  }

  def getAllDocumentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail =>
      DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail), dunningLock = dunningLockExists(documentDetail.transactionId)))
  }

  def isAllPaid()(implicit user: MtdItUser[_]): Boolean = documentDetails.forall(_.isPaid)

  def isAllInterestPaid()(implicit user: MtdItUser[_]): Boolean = documentDetails.forall(_.interestIsPaid)

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
