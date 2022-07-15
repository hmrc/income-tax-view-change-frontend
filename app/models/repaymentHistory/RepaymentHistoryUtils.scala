/*
 * Copyright 2022 HM Revenue & Customs
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

import exceptions.MissingFieldException
import models.financialDetails.Payment
import play.api.i18n.Messages
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate

object RepaymentHistoryUtils {

  private def getControllerHref(transactionId: Option[String], isAgent: Boolean) = {
    if (isAgent) {
      controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent(transactionId.getOrElse(throw MissingFieldException("Transaction ID"))).url
    } else {
      controllers.routes.PaymentAllocationsController.viewPaymentAllocation(transactionId.getOrElse(throw MissingFieldException("Transaction ID"))).url
    }
  }

  private def getMFACreditsLink(date: String, isAgent: Boolean) = {
    val year = LocalDate.parse(date).getYear
    if (isAgent) {
      controllers.routes.CreditsSummaryController.showAgentCreditsSummary(year).url
    } else {
      controllers.routes.CreditsSummaryController.showCreditsSummary(year).url
    }
  }

  private def getRefundsLink(isAgent: Boolean) = {
    if (isAgent) {
      "agents/refund-to-taxpayer"
    } else {
      "refund-to-taxpayer"
    }
  }

  private def groupedPayments(payments: List[PaymentHistoryEntry]): List[(Int, List[PaymentHistoryEntry])] = {
    def sortPayments(payments: List[PaymentHistoryEntry]) = {
      payments
        .map(payment => (LocalDate.parse(payment.date).toEpochDay, payment))
        .sortWith((left, right) => left._1 < right._1)
        .map { case (_, payments) => payments }
    }

    payments.groupBy[Int] { payment => {
      LocalDate.parse(payment.date).getYear
    }
    }.toList.sortBy(_._1).reverse
      .map { case (year, payments) => (year, sortPayments(payments)) }
  }

  def getGroupedPaymentHistoryData(payments: List[Payment], repayments: List[RepaymentHistory], isAgent: Boolean,
                                   MFACreditsEnabled: Boolean, CutOverCreditsEnabled: Boolean, languageUtils: LanguageUtils
                                  )(implicit messages: Messages): List[(Int, List[PaymentHistoryEntry])] = {
    val combinedPayments = combinePaymentHistoryData(payments, repayments, isAgent,
      MFACreditsEnabled, CutOverCreditsEnabled, languageUtils
    )
    groupedPayments(combinedPayments)
  }

  private def combinePaymentHistoryData(payments: List[Payment], repayments: List[RepaymentHistory], isAgent: Boolean,
                                        MFACreditsEnabled: Boolean, CutOverCreditsEnabled: Boolean, languageUtils: LanguageUtils
                                       )(implicit messages: Messages): List[PaymentHistoryEntry] = {
    val filteredPayments = payments.flatMap(payment => {
      if (payment.credit.isEmpty) {
        Some(PaymentHistoryEntry(
          date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date")),
          description = "paymentHistory.paymentToHmrc",
          transactionId = payment.transactionId,
          amount = payment.amount,
          linkUrl = getControllerHref(payment.transactionId, isAgent),
          visuallyHiddenText = s"${payment.dueDate.get} ${payment.amount.getOrElse(throw MissingFieldException("Amount")).abs.toCurrency}"
        ))
      } else {
        if (payment.validMFACreditDescription()) {
          if (MFACreditsEnabled) {
            Some(PaymentHistoryEntry(
              date = payment.documentDate,
              description = "paymentHistory.mfaCredit",
              amount = payment.amount,
              linkUrl = getMFACreditsLink(payment.documentDate, isAgent),
              visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Transaction ID"))}"
            ))
          } else None
        } else {
          if (CutOverCreditsEnabled) {
            Some(PaymentHistoryEntry(
              date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date Cutover credit")),
              description = "paymentHistory.paymentFromEarlierYear",
              amount = payment.amount,
              linkUrl = getControllerHref(payment.transactionId, isAgent),
              visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Document ID"))}"
            ))
          } else None
        }
      }
    })

    val filteredRepayments = repayments.flatMap(repayment => {
      Some(PaymentHistoryEntry(
        date = languageUtils.Dates.shortDate(repayment.estimatedRepaymentDate)(messages),
        description = "paymentHistory.refund",
        amount = Some(repayment.totalRepaymentAmount),
        linkUrl = s"${getRefundsLink(isAgent)}/${repayment.repaymentRequestNumber}",
        visuallyHiddenText = s"${repayment.repaymentRequestNumber}"
      ))
    })
    filteredPayments ++ filteredRepayments
  }

}
