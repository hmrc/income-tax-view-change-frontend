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
import models.incomeSourceDetails.TaxYear

import java.time.LocalDate


sealed trait CreditRow {
  val amount: BigDecimal
  val creditType: CreditType
}

object CreditRow {

  def fromTransaction(transaction: Transaction): Option[CreditRow] = {

    transaction.transactionType match {
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

case class CreditViewRow(amount: BigDecimal, creditType: CreditType, taxYear: TaxYear) extends CreditRow

case class PaymentCreditRow(amount: BigDecimal,  date: LocalDate) extends CreditRow {

  override val creditType: CreditType = PaymentType
}
case class RefundRow(amount: BigDecimal) extends CreditRow {

  override val creditType: CreditType = Repayment
}

case class CreditAndRefundViewModel(availableCredit: BigDecimal,
                                    allocatedCreditForOverdueCharges: BigDecimal,
                                    allocatedCreditForFutureCharges: BigDecimal,
                                    unallocatedCredit: BigDecimal,
                                    totalCredit: BigDecimal,
                                    creditRows: List[CreditRow]) {
  val hasCreditOrRefunds: Boolean = {
    availableCredit > 0 || allocatedCreditForFutureCharges > 0 || creditRows.exists(_.amount > 0)
  }
  val hasAvailableCredit = availableCredit != 0
  val hasAllocatedCreditForFutureCharges = allocatedCreditForFutureCharges != 0
}

object CreditAndRefundViewModel {

  def fromCreditAndRefundModel(model: CreditsModel): CreditAndRefundViewModel = {
    CreditAndRefundViewModel(
      availableCredit = model.availableCreditForRepayment,
      allocatedCreditForOverdueCharges = model.allocatedCreditForOverdueCharges,
      allocatedCreditForFutureCharges = model.allocatedCreditForFutureCharges,
      unallocatedCredit = model.unallocatedCredit,
      totalCredit = model.totalCredit,
      creditRows =
        (removeNoRemainingCredit andThen
          orderByDescendingTaxYear andThen
          orderCreditsFirstRepaymentsSecond).apply(model.transactions).flatMap(CreditRow.fromTransaction)
    )
  }

  private val orderCreditsFirstRepaymentsSecond: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) =>
    // all credits first
    transactions.filter(_.transactionType != Repayment) ++:
      // then repayments sorted by amount, descending
      transactions.filter(_.transactionType == Repayment).sortBy(_.amount).reverse

  private val removeNoRemainingCredit: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) =>
    transactions.filter(_.amount > 0)

  private val orderByDescendingTaxYear: (List[Transaction]) => List[Transaction] = (transactions: List[Transaction]) => {
    // sort by tax year, but maintain ordering within tax years
    val groupedByTaxYear = transactions
      .groupBy(_.taxYear.map(_.endYear).getOrElse(0))

    groupedByTaxYear.keys.toList.sorted.reverse.flatMap(groupedByTaxYear.get).flatten
  }
}


