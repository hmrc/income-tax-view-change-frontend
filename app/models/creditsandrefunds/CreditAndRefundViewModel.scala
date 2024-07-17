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

object CreditRow {

  def fromTransaction(transaction: Transaction): Option[CreditRow] = {

    transaction.creditType match {
      case PaymentType =>
        transaction.dueDate.map(date =>
          PaymentCreditRow(
            amount = transaction.amount,
            date = date))
      case Repayment =>
        Some(RefundRow(amount = transaction.amount))
      case creditType =>
        transaction.taxYear.map(year =>
          CreditViewRow(
            amount = transaction.amount,
            creditType = creditType,
            taxYear = year))
    }
  }
}

case class CreditViewRow(amount: BigDecimal, creditType: CreditType, taxYear: String) extends CreditRow

case class PaymentCreditRow(amount: BigDecimal,  date: LocalDate) extends CreditRow {

  override val creditType: CreditType = PaymentType
}
case class RefundRow(amount: BigDecimal) extends CreditRow {

  override val creditType: CreditType = Repayment
}

case class CreditAndRefundViewModel(availableCredit: BigDecimal,
                                    allocatedCredit: BigDecimal,
                                    creditRows: List[CreditRow])

object CreditAndRefundViewModel {

  def fromCreditAndRefundModel(model: CreditsModel): CreditAndRefundViewModel = {
    CreditAndRefundViewModel(
      availableCredit = model.availableCredit,
      allocatedCredit = model.allocatedCredit,
      creditRows =
        (removeNoRemainingCredit andThen
          orderByDescendingTaxYear andThen
          orderCreditsFirstRepaymentsSecond).apply(model.transactions).flatMap(CreditRow.fromTransaction)
    )
  }

  private val orderCreditsFirstRepaymentsSecond: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) =>
    // all credits first
    transactions.filter(_.creditType != Repayment) ++:
      // then repayments sorted by amount, descending
      transactions.filter(_.creditType == Repayment).sortBy(_.amount).reverse

  private val removeNoRemainingCredit: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) =>
    transactions.filter(_.amount > 0)

  private val orderByDescendingTaxYear: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) =>
    transactions.sortBy(_.taxYear.map(_.toInt).getOrElse(0)).reverse
}


