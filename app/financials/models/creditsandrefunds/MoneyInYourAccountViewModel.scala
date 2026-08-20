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
import financials.controllers.routes
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

object CreditRow {

  def fromTransaction(transaction: Transaction): Option[CreditRow] = {

    (transaction.transactionType, transaction.chargeClassification) match {
      case (PaymentType, _) =>
        for 
          dueDate <- transaction.dueDate
          effectiveDate <- transaction.effectiveDateOfPayment
        yield PaymentCreditRow(
          transaction = transaction,
          date = dueDate,
          effectiveDate = effectiveDate
        )
      case (Repayment, _) =>
        Some(RefundRow(transaction, date = LocalDate.now())) // Set date to current date to enable correct ordering of rows in WhereMoneyCameFromTable.scala.html
      case (ITSAReturnAmendmentCredit, Some("AC" | "MC")) => 
        transaction.dueDate.map(CorrectionRow(transaction, _))
      case creditType =>
        for 
          taxYear <- transaction.taxYear
          dueDate <- transaction.dueDate
        yield CreditViewRow(
            transaction = transaction, 
            taxYear = taxYear,
            date = dueDate,
            isRevenueAmendment = transaction.isRevenueAmendment
          )
    }
  }
}

case class CreditViewRow(transaction: Transaction, override val taxYear: TaxYear, date: LocalDate, isRevenueAmendment: Boolean) extends CreditRow {
  def descriptionLink(isAgent: Boolean): String = creditType match {
    case PoaOneReconciliationCredit | PoaTwoReconciliationCredit | ITSAReturnAmendmentCredit =>
      if (isAgent) routes.ChargeSummaryController.showAgent(taxYear = taxYear.endYear, id = transactionId).url else routes.ChargeSummaryController.show(taxYear = taxYear.endYear, id = transactionId).url
    case _ =>
      if (isAgent) routes.CreditsSummaryController.showAgentCreditsSummary(calendarYear = date.getYear).url else routes.CreditsSummaryController.showCreditsSummary(calendarYear = date.getYear).url
  }
}

case class PaymentCreditRow(transaction: Transaction, date: LocalDate, effectiveDate: LocalDate) extends CreditRow {
  def descriptionLink(isAgent: Boolean): String =
    if (isAgent) routes.PaymentAllocationsController.viewPaymentAllocationAgent(transactionId).url else routes.PaymentAllocationsController.viewPaymentAllocation(transactionId).url
}

case class RefundRow(transaction: Transaction, date: LocalDate) extends CreditRow {
  def descriptionLink: String = routes.PaymentHistoryController.refundStatus().url
}

case class CorrectionRow(transaction: Transaction, date: LocalDate) extends CreditRow {
  def descriptionLink: String = ??? 
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
      unallocatedCredit = model.unallocatedCredit,
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


