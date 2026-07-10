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

package returns.models

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class FinancialDetail(taxYear: String,
                           mainType: Option[String] = None,
                           mainTransaction: Option[String] = None,
                           transactionId: Option[String] = None,
                           transactionDate: Option[LocalDate] = None,
                           chargeReference: Option[String] = None,
                           `type`: Option[String] = None,
                           totalAmount: Option[BigDecimal] = None,
                           originalAmount: Option[BigDecimal] = None,
                           outstandingAmount: Option[BigDecimal] = None,
                           clearedAmount: Option[BigDecimal] = None,
                           chargeType: Option[String] = None,
                           accruedInterest: Option[BigDecimal] = None,
                           items: Option[Seq[SubItem]]
                          ) {

  lazy val dunningLockExists: Boolean = dunningLocks.nonEmpty
  
  lazy val dunningLocks: Seq[SubItem] = {
    items.fold(Seq.empty[SubItem]) { subItems =>
      subItems.filter(_.dunningLock.contains("Stand over order"))
    }
  }
}


object FinancialDetail {
  implicit val format: Format[FinancialDetail] = Json.format[FinancialDetail]
}