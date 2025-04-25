/*
 * Copyright 2025 HM Revenue & Customs
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

package models.extensions

import enums._
import models.financialDetails.{DocumentDetail, FinancialDetailsModel}

import java.time.LocalDate

trait FinancialDetailsModelExtension {
  // Extend FinancialDetailsModel with methods which applicable only during testing
  implicit class FinancialDetailsModelConversion(fdm: FinancialDetailsModel) {
    def documentDetailsFilterByTaxYear(taxYear: Int): List[DocumentDetail] = {
      fdm match {
        case FinancialDetailsModel(_, documentDetails, _) =>
          documentDetails.filter(_.taxYear == taxYear)
      }
    }

    def getAllDueDates: List[LocalDate] = {
      fdm match {
        case FinancialDetailsModel(_, documentDetails, _) =>
          documentDetails.flatMap(_.getDueDate())
      }
    }

    def validChargeTypeCondition: DocumentDetail => Boolean = documentDetail => {
      (documentDetail.documentText, documentDetail.getDocType) match {
        case (Some(documentText), _) if documentText.contains("Class 2 National Insurance") => true
        case (_, Poa1Charge | Poa2Charge | Poa1ReconciliationDebit | Poa2ReconciliationDebit | TRMNewCharge | TRMAmendCharge) => true
        case (_, _) => false
      }
    }
  }
}
