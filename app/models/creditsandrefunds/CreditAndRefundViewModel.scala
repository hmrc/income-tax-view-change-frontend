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

package models.creditsandrefunds

import models.financialDetails._

import java.time.LocalDate

sealed trait CreditRow {
  val amount: BigDecimal
  val creditType: CreditType
}
case class CreditViewRow(amount: BigDecimal, creditType: CreditType, taxYear: String) extends CreditRow
case class PaymentCreditRow(amount: BigDecimal,  date: LocalDate) extends CreditRow {

  override val creditType: CreditType = PaymentType

}
case class RefundRow(amount: BigDecimal) extends CreditRow {

  override val creditType: CreditType = Repayment

}

case class CreditAndRefundViewModel(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)], balanceDetails: Option[BalanceDetails]) {

  private val repayments: Seq[CreditRow] = balanceDetails.fold(Seq[RefundRow]())(details =>
       Seq(details.firstPendingAmountRequested, details.secondPendingAmountRequested)
        .flatten.sorted(Ordering.BigDecimal.reverse).map(amount => RefundRow(amount)))

  private val creditsAndPayments: Seq[CreditRow] = sortCreditsByYear.flatMap(cc => {
    val (documentDetails, financialDetail) = cc
    val maybeCreditRow = for {
      creditType <- financialDetail.getCreditType
      amount <- documentDetails.documentDetail.paymentOrChargeCredit
    } yield {
      creditType match {
        case PaymentType => PaymentCreditRow(amount = amount, date = documentDetails.dueDate.get)
        case _ => CreditViewRow(amount = amount, creditType = creditType, taxYear = financialDetail.taxYear)
      }
    }
    maybeCreditRow
  })

  val sortedCreditRows: Seq[CreditRow] = creditsAndPayments ++ repayments

  private def sortCreditsByYear: List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    val sortedCredits = creditCharges.sortBy {
      case (_, financialDetails) => financialDetails.taxYear
    }
    sortedCredits.reverse
  }

  val sortedCreditCharges: Seq[(DocumentDetailWithDueDate, FinancialDetail)] = sortCreditsByYear

}



