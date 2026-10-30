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

package financials.models.creditsandrefunds

import common.models.incomeSourceDetails.TaxYear
import financials.models.*

import java.time.LocalDate


sealed trait CreditRow {
  val transaction: Transaction
  val date: LocalDate
  val amount: BigDecimal = transaction.amount
  val transactionId: String = transaction.transactionId
  val creditType: CreditType = transaction.transactionType
  val taxYear: TaxYear = TaxYear.getTaxYear(date)
}

case class MoneyInYourAccountViewModel(availableCredit: BigDecimal,
                                       allocatedCredit: BigDecimal,
                                       unallocatedCredit: BigDecimal,
                                       totalCredit: BigDecimal,
                                       firstPendingAmountRequested: Option[BigDecimal],
                                       secondPendingAmountRequested: Option[BigDecimal],
                                       creditRows: List[CreditRow],
                                       checkRefundStatusLink: String,
                                       dunningLock: Boolean)

