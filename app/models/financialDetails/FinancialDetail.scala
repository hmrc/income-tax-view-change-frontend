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

case class FinancialDetail(taxYear: String,
                           mainType: Option[String] = None,
                           transactionId: Option[String] = None,
                           transactionDate: Option[String] = None,
                           `type`: Option[String] = None,
                           totalAmount: Option[BigDecimal] = None,
                           originalAmount: Option[BigDecimal] = None,
                           outstandingAmount: Option[BigDecimal] = None,
                           clearedAmount: Option[BigDecimal] = None,
                           chargeType: Option[String] = None,
                           items: Option[Seq[SubItem]]
                          ) {

  val payments: Seq[Payment] = items match {
    case Some(subItems) => subItems.map { subItem =>
      Payment(reference = subItem.paymentReference, amount = subItem.paymentAmount, method = subItem.paymentMethod,
        lot = subItem.paymentLot, lotItem = subItem.paymentLotItem, date = subItem.clearingDate, transactionId = subItem.transactionId)
    }.filter(_.reference.isDefined)
    case None => Seq.empty[Payment]
  }

  lazy val allocation: Seq[Payment] = items match {
    case Some(subItems) => subItems
      .collect {
        case subItem if subItem.paymentLot.isDefined && subItem.paymentLotItem.isDefined =>
          Payment(reference = subItem.paymentReference, amount = subItem.amount, method = subItem.paymentMethod,
            lot = subItem.paymentLot, lotItem = subItem.paymentLotItem, date = subItem.clearingDate, transactionId = subItem.transactionId)
      }
    case None => Seq.empty[Payment]
  }
}


object FinancialDetail {
  implicit val format: Format[FinancialDetail] = Json.format[FinancialDetail]
}
