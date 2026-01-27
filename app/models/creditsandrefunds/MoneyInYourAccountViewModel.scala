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

case class MoneyInYourAccountViewModel(availableCredit: BigDecimal,
                                       allocatedCredit: BigDecimal,
                                       unallocatedCredit: BigDecimal,
                                       totalCredit: BigDecimal,
                                       firstPendingAmountRequested: Option[BigDecimal],
                                       secondPendingAmountRequested: Option[BigDecimal],
                                       creditRows: List[CreditRow],
                                       checkRefundStatusLink: String) {
  val hasCreditOrRefunds: Boolean = {
    availableCredit > 0 || allocatedCredit > 0 || creditRows.exists(_.amount > 0)
  }
  val hasAvailableCredit = availableCredit != 0
  val hasAllocatedCredit = allocatedCredit != 0
}

object MoneyInYourAccountViewModel {

  def fromCreditsModel(model: CreditsModel, refundsUrl: String): MoneyInYourAccountViewModel = {
    MoneyInYourAccountViewModel(
      availableCredit = model.availableCreditForRepayment,
      allocatedCredit = model.allocatedCreditForFutureCharges,
      unallocatedCredit =  model.unallocatedCredit,
      totalCredit = model.totalCredit,
      firstPendingAmountRequested = model.firstPendingAmountRequested,
      secondPendingAmountRequested = model.secondPendingAmountRequested,
      creditRows =
        (removeNoRemainingCredit andThen
          orderByDescendingTaxYear andThen
          orderCreditsFirstRepaymentsSecond).apply(model.transactions).flatMap(CreditRow.fromTransaction),
      checkRefundStatusLink = refundsUrl
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


