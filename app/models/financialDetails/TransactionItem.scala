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

import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface

trait TransactionItem {

  val transactionId: String

  val transactionType: TransactionType

  val subTransactionType: Option[SubTransactionType]

  val taxYear: TaxYear

  val outstandingAmount: BigDecimal

  val isLatePaymentInterest: Boolean

  def isOverdue()(implicit dateService: DateServiceInterface): Boolean
}
