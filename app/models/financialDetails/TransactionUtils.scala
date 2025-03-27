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

package models.financialDetails

import play.api.Logger

import scala.util.{Failure, Success, Try}

trait TransactionUtils {

  // implicit class IntWithTimes
  implicit class FinancialDetailsModelConversion(fdm: FinancialDetailsModel) {
    def documentDetailsFilterByTaxYear(taxYear: Int) : List[DocumentDetail] = {
      fdm match {
        case FinancialDetailsModel(_, documentDetails, _) =>
          documentDetails.filter(_.taxYear == taxYear)
      }
    }
  }

  //documentDetailsFilterByTaxYear

  def getChargeItemOpt(financialDetails: List[FinancialDetail])
                      (documentDetail: DocumentDetail): Option[ChargeItem] = {
    Try(ChargeItem.fromDocumentPair(
      documentDetail,
      financialDetails)
    ) match {
      case Failure(exception) =>
        Logger("application").warn(exception.getMessage)
        None
      case Success(value) =>
        Some(value)
    }
  }

}
