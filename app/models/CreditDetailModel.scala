/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import models.financialDetails.{DocumentDetail, FinancialDetailsModel}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel

case class CreditDetailModel(documentDetail: DocumentDetail, creditType: CreditType)

sealed trait CreditType

case object MfaCreditType extends CreditType

case object CutOverCreditType extends CreditType

object CreditDetailModel {

  implicit def financialDetailsWithDocumentDetailsModelToCreditDetailsModel(document: FinancialDetailsWithDocumentDetailsModel): List[CreditDetailModel] = {
    document.documentDetails.map(documentDetail => CreditDetailModel(documentDetail = documentDetail, creditType = CutOverCreditType))
  }

  implicit def financialDetailsModelToCreditModel(document: FinancialDetailsModel): List[CreditDetailModel] = {
    document.documentDetails.map(documentDetail => CreditDetailModel(documentDetail = documentDetail, creditType = MfaCreditType))
  }

}