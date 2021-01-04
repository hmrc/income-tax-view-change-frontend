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

package models.paymentAllocations

import play.api.libs.json.{Format, Json}

sealed trait PaymentAllocationsResponseModel

case class PaymentAllocationsModel(amount: Option[BigDecimal],
                                   method: Option[String],
                                   transactionDate: Option[String],
                                   allocations: Seq[AllocationDetail]) extends PaymentAllocationsResponseModel


object PaymentAllocationsModel {
  implicit val format: Format[PaymentAllocationsModel] = Json.format[PaymentAllocationsModel]
}

case class PaymentAllocationsErrorModel(code: Int, message: String) extends PaymentAllocationsResponseModel

object PaymentAllocationsErrorModel {
  implicit val format: Format[PaymentAllocationsErrorModel] = Json.format[PaymentAllocationsErrorModel]
}
