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

package models.paymentAllocationCharges

import models.financialDetails.Payment
import play.api.libs.json.{Format, Json}

case class FinancialDetail(taxYear: String,
                           transactionId: String,
                           transactionDate: Option[String],
                           `type`: Option[String],
                           totalAmount: Option[BigDecimal],
                           originalAmount: Option[BigDecimal],
                           outstandingAmount: Option[BigDecimal],
                           clearedAmount: Option[BigDecimal],
                           chargeType: Option[String],
                           mainType: Option[String],
                           items: Option[Seq[SubItem]]
                          ) {

  val payments: Seq[Payment] = items match {
    case Some(subItems) => subItems.map { subItem =>
      Payment(
        reference = subItem.paymentReference,
        amount = subItem.paymentAmount,
        method = subItem.paymentMethod,
        lot = subItem.paymentLot,
        lotItem = subItem.paymentLotItem,
        date = subItem.clearingDate
      )
    }.filter(_.reference.isDefined)
    case None => Seq.empty[Payment]
  }

}

object FinancialDetail {

  implicit val format: Format[FinancialDetail] = Json.format[FinancialDetail]

}