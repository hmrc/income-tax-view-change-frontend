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

case class FinancialDetailsModel(documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {

  val documentDescriptionToFinancialMainType: String => String = {
    case "ITSA- POA 1" => "SA Payment on Account 1"
    case "ITSA - POA 2" => "SA Payment on Account 2"
    case "ITSA- Bal Charge" => "SA Balancing Charge"
    case other => other
  }

  def getDueDateFor(documentDetail: DocumentDetail): Option[LocalDate] = {
    documentDetail.documentDescription flatMap { documentDescription =>
      financialDetails.find { financialDetail =>
        financialDetail.mainType.contains(documentDescriptionToFinancialMainType(documentDescription)) &&
          financialDetail.taxYear == documentDetail.taxYear
      }
    } flatMap (_.items.flatMap(_.headOption.flatMap(_.dueDate))) map LocalDate.parse
  }

  def getAllDueDates: List[LocalDate] = {
    documentDetails.filter(_.documentDescription.isDefined)
      .map(documentDetail => (documentDetail.taxYear, documentDescriptionToFinancialMainType(documentDetail.documentDescription.get)))
      .flatMap { case (taxYear, mainType) =>
        financialDetails.find(financialDetail => financialDetail.mainType.contains(mainType) && financialDetail.taxYear == taxYear)
      }.flatMap(_.items.flatMap(_.headOption.flatMap(_.dueDate)))
      .map(LocalDate.parse)
  }

  def findDocumentDetailForTaxYear(taxYear: Int): Option[DocumentDetail] = documentDetails.find(_.taxYear.toInt == taxYear)

  def findDocumentDetailForYearWithDueDate(taxYear: Int): Option[DocumentDetailWithDueDate] = {
    findDocumentDetailForTaxYear(taxYear)
      .map(documentDetail => DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail)))
  }

  def findDocumentDetailByIdWithDueDate(id: String): Option[DocumentDetailWithDueDate] = {
    documentDetails.find(_.transactionId == id)
      .map(documentDetail => DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail)))
  }

  def getAllDocumentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail => DocumentDetailWithDueDate(documentDetail, getDueDateFor(documentDetail)))
  }

  def isAllPaid()(implicit user: MtdItUser[_]): Boolean = documentDetails.forall(_.isPaid)

}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
