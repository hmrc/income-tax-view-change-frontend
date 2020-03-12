/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class TransactionModel(chargeType: Option[String] = None,
                       mainType: Option[String] = None,
                       periodKey: Option[String] = None,
                       periodKeyDescription: Option[String] = None,
                       taxPeriodFrom: Option[LocalDate] = None,
                       taxPeriodTo: Option[LocalDate] = None,
                       businessPartner: Option[String] = None,
                       contractAccountCategory: Option[String] = None,
                       contractAccount: Option[String] = None,
                       contractObjectType: Option[String] = None,
                       contractObject: Option[String] = None,
                       sapDocumentNumber: Option[String] = None,
                       sapDocumentNumberItem: Option[String] = None,
                       chargeReference: Option[String] = None,
                       mainTransaction: Option[String] = None,
                       subTransaction: Option[String] = None,
                       originalAmount: Option[BigDecimal] = None,
                       outstandingAmount: Option[BigDecimal] = None,
                       clearedAmount: Option[BigDecimal] = None,
                       accruedInterest: Option[BigDecimal] = None,
                       items: Option[Seq[SubItemModel]] = None) {

  val isPaid: Boolean = outstandingAmount.fold(true)(_ <= 0)

  def charges(): Seq[SubItemModel] = items.getOrElse(Seq()).filter(_.dueDate.isDefined)

  def payments(): Seq[SubItemModel] = items.getOrElse(Seq()).filter(_.paymentReference.isDefined)

  def eligibleToPay(paymentEnabled: Boolean) :Boolean = {
    !isPaid && paymentEnabled
  }

  val due: Option[LocalDate] = charges().headOption.flatMap(_.dueDate)

  def isOverdue: Boolean = due.exists(_.isBefore(LocalDate.now()))
}

object TransactionModel {
  implicit val format: Format[TransactionModel] = Json.format[TransactionModel]
}