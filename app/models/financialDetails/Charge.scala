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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class Charge(taxYear: Option[String] = None,
                  transactionId: Option[String] = None,
                  transactionDate: Option[String] = None,
                  `type`: Option[String] = None,
                  totalAmount: Option[BigDecimal] = None,
                  originalAmount: Option[BigDecimal] = None,
                  outstandingAmount: Option[BigDecimal] =None,
                  items: Option[Seq[SubItem]] = None
                 ) {
  val isPaid: Boolean = outstandingAmount.fold(true)(_ <= 0)

  def charges(): Seq[SubItem] = items.getOrElse(Seq()).filter(_.dueDate.isDefined)

  def payments(): Seq[SubItem] = items.getOrElse(Seq()).filter(_.paymentReference.isDefined)

  def eligibleToPay(paymentEnabled: Boolean) :Boolean = {
    !isPaid && paymentEnabled
  }

  val due: Option[LocalDate] = charges().headOption.flatMap(_.dueDate).map(due => LocalDate.parse(due))

  def isOverdue: Boolean = due.exists(dueDate => dueDate.isBefore(LocalDate.now()))
}

object Charge {

  implicit val format: Format[Charge] = Json.format[Charge]
}
