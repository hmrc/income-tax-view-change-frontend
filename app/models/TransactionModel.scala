/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class TransactionModel(chargeType: Option[String],
                       mainType: Option[String],
                       periodKey: Option[String],
                       periodKeyDescription: Option[String],
                       taxPeriodFrom: Option[LocalDate],
                       taxPeriodTo: Option[LocalDate],
                       businessPartner: Option[String],
                       contractAccountCategory: Option[String],
                       contractAccount: Option[String],
                       contractObjectType: Option[String],
                       contractObject: Option[String],
                       sapDocumentNumber: Option[String],
                       sapDocumentNumberItem: Option[String],
                       chargeReference: Option[String],
                       mainTransaction: Option[String],
                       subTransaction: Option[String],
                       originalAmount: Option[BigDecimal],
                       outstandingAmount: Option[BigDecimal],
                       clearedAmount: Option[BigDecimal],
                       accruedInterest: Option[BigDecimal],
                       items: Seq[SubItemModel]){

  def charges(): Seq[SubItemModel] = items.filter(_.dueDate.isDefined)

  def payments(): Seq[SubItemModel] = items.filter(_.paymentReference.isDefined)

}

case class TransactionModelWithYear(model: TransactionModel,
                                    taxYear: Int)

object TransactionModel {
  implicit val format: Format[TransactionModel] = Json.format[TransactionModel]
}