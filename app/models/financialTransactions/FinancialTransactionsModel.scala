/*
 * Copyright 2019 HM Revenue & Customs
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

package models.financialTransactions

import java.time.{LocalDate, ZonedDateTime}

import play.api.libs.json.{Format, Json}

sealed trait FinancialTransactionsResponseModel

case class FinancialTransactionsModel(idType: Option[String],
                                      idNumber: Option[String],
                                      regimeType: Option[String],
                                      processingDate: ZonedDateTime,
                                      financialTransactions: Option[Seq[TransactionModel]]) extends FinancialTransactionsResponseModel {

  def withYears(): Seq[TransactionModelWithYear] =
    financialTransactions.getOrElse(Seq()).flatMap(ft => ft.taxPeriodTo.map(toDate => TransactionModelWithYear(ft, toDate.getYear)))

  def findChargeForTaxYear(taxYear: Int): Option[TransactionModel] =
    financialTransactions.getOrElse(Seq()).find(_.taxPeriodTo.fold(false)(_ == LocalDate.parse(s"$taxYear-04-05")))

}


case class FinancialTransactionsErrorModel(code: Int, message: String) extends FinancialTransactionsResponseModel

object FinancialTransactionsErrorModel {
  implicit val format: Format[FinancialTransactionsErrorModel] = Json.format[FinancialTransactionsErrorModel]
}

object FinancialTransactionsModel {
  implicit val format: Format[FinancialTransactionsModel] = Json.format[FinancialTransactionsModel]
}