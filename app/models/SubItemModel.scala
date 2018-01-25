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

import play.api.libs.json._

case class SubItemModel(subItem: Option[String],
                   dueDate: Option[LocalDate],
                   amount: Option[BigDecimal],
                   clearingDate: Option[LocalDate],
                   clearingReason: Option[String],
                   outgoingPaymentMethod: Option[String],
                   paymentLock: Option[String],
                   clearingLock: Option[String],
                   interestLock: Option[String],
                   dunningLock: Option[String],
                   returnFlag: Option[Boolean],
                   paymentReference: Option[String],
                   paymentAmount: Option[BigDecimal],
                   paymentMethod: Option[String],
                   paymentLot: Option[String],
                   paymentLotItem: Option[String],
                   clearingSAPDocument: Option[String],
                   statisticalDocument: Option[String],
                   returnReason: Option[String],
                   promiseToPay: Option[String])

object SubItemModel {
  implicit val format: Format[SubItemModel] = Json.format[SubItemModel]
}