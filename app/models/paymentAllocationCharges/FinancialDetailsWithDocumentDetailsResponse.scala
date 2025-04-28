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

package models.paymentAllocationCharges

import models.financialDetails.{DocumentDetail, FinancialDetail}
import models.readNullableList
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}

import scala.util.Try

sealed trait FinancialDetailsWithDocumentDetailsResponse


case class FinancialDetailsWithDocumentDetailsModel(
                                                     documentDetails: List[DocumentDetail],
                                                     financialDetails: List[FinancialDetail]
                                                   ) extends FinancialDetailsWithDocumentDetailsResponse {

  // TODO: apply proper fix
  val filteredDocumentDetails = Try {
    documentDetails.filter(_.paymentLot == financialDetails.head.items.get.head.paymentLot)
      .filter(_.paymentLotItem == financialDetails.head.items.get.head.paymentLotItem)
  }.toOption.getOrElse( List.empty )

}


object FinancialDetailsWithDocumentDetailsModel {
  implicit val writes: OWrites[FinancialDetailsWithDocumentDetailsModel] = Json.writes[FinancialDetailsWithDocumentDetailsModel]

  implicit val reads: Reads[FinancialDetailsWithDocumentDetailsModel] = (
    readNullableList[DocumentDetail](__ \ "documentDetails") and
      readNullableList[FinancialDetail](__ \ "financialDetails")
    )(FinancialDetailsWithDocumentDetailsModel.apply _)
}

case class FinancialDetailsWithDocumentDetailsErrorModel(code: Int, message: String) extends FinancialDetailsWithDocumentDetailsResponse
