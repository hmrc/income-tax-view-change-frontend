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

package models.repaymentHistory

import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface

import java.time.LocalDate

case class PaymentHistoryEntry(date: LocalDate,
                               creditType: TransactionType,
                               amount: Option[BigDecimal],
                               transactionId: Option[String] = None,
                               linkUrl: String,
                               visuallyHiddenText: String)(implicit val dateService: DateServiceInterface) {

  def getTaxYear: TaxYear = {
    val endYear = dateService.getAccountingPeriodEndDate(date).getYear
    TaxYear(endYear-1, endYear)
  }

  private val creditTypes: Seq[CreditType] = Seq(
    BalancingChargeCreditType,
    CutOverCreditType,
    MfaCreditType,
    RepaymentInterest,
    PoaOneReconciliationCredit,
    PoaTwoReconciliationCredit
  )

  def isCredit: Boolean = creditTypes.contains(creditType)
}
