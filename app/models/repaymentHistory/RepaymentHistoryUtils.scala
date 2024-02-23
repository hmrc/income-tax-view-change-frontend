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

import exceptions.MissingFieldException
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.financialDetails.Payment
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json
import services.DateServiceInterface
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

  private def getCreditsLinkUrl(date: LocalDate, isAgent: Boolean) = {
    val year = date.getYear
    if (isAgent) {
      controllers.routes.CreditsSummaryController.showAgentCreditsSummary(year).url
    } else {
      controllers.routes.CreditsSummaryController.showCreditsSummary(year).url
    }
  }

  private def groupedPayments(payments: List[PaymentHistoryEntry]): List[(Int, List[PaymentHistoryEntry])] = {
    def sortPayments(payments: List[PaymentHistoryEntry]) = {
      payments
        .map(payment => (payment.date.toEpochDay, payment))
        .sortWith((left, right) => left._1 < right._1)
        .map { case (_, payments) => payments }
    }

    payments.groupBy[Int] { payment => {
      payment.date.getYear
    }
    }.toList.sortBy(_._1).reverse
      .map { case (year, payments) => (year, sortPayments(payments)) }
  }

  def getGroupedPaymentHistoryData(payments: List[Payment], repayments: List[RepaymentHistory], isAgent: Boolean,
                                   MFACreditsEnabled: Boolean, CutOverCreditsEnabled: Boolean, languageUtils: LanguageUtils
                                  )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[(Int, List[PaymentHistoryEntry])] = {
    val combinedPayments = combinePaymentHistoryData(payments, repayments, isAgent,
      MFACreditsEnabled, CutOverCreditsEnabled, languageUtils
    )
    groupedPayments(combinedPayments)
  }

  private def combinePaymentHistoryData(payments: List[Payment],
                                        repayments: List[RepaymentHistory],
                                        isAgent: Boolean,
                                        MFACreditsEnabled: Boolean,
                                        CutOverCreditsEnabled: Boolean,
                                        languageUtils: LanguageUtils
                                       )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[PaymentHistoryEntry] = {

    val filteredPayments = payments.flatMap(payment => filterPayment(payment, isAgent, MFACreditsEnabled, CutOverCreditsEnabled))

    val filteredRepayments = repayments.filter(_.status.isInstanceOf[Approved]).map(repayment => filterRepayment(repayment)(messages, languageUtils, dateServiceInterface))

    filteredPayments ++ filteredRepayments
  }

  private def filterPayment(payment: Payment,
                            isAgent: Boolean,
                            MFACreditsEnabled: Boolean,
                            CutOverCreditsEnabled: Boolean
                           )(implicit messages: Messages, dateservice: DateServiceInterface): Option[PaymentHistoryEntry] = {
    val isBCC = payment.isBalancingChargeCredit
    val isCutover = payment.isCutOverCredit
    val isMFA = payment.isMFACredit
    val isPayment = payment.isPaymentToHMRC

    (isBCC, isCutover, isMFA, isPayment) match {
      case (true, false, false, false) => Some(balancingChargeCreditEntry(payment, isAgent))
      case (false, true, false, false) => if (CutOverCreditsEnabled) Some(cutOverCreditEntry(payment, isAgent)) else None
      case (false, false, true, false) => if (MFACreditsEnabled) Some(mfaCreditEntry(payment, isAgent)) else None
      case (false, false, false, true) => Some(paymentToHMRCEntry(payment, isAgent))
      case _ => None
    }

  }

  private def paymentToHMRCEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    Logger("application").info("[RepaymentHistoryUtils][combinePaymentHistoryData][paymentToHMRCEntry], json:" + Json.prettyPrint(Json.toJson(payment)))
    PaymentHistoryEntry(
      date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date")),
      description = "paymentHistory.paymentToHmrc",
      transactionId = payment.transactionId,
      amount = payment.amount,
      linkUrl = getControllerHref(payment.transactionId, isAgent),
      visuallyHiddenText = s"${payment.dueDate.get} ${payment.amount.getOrElse(throw MissingFieldException("Amount")).abs.toCurrency}"
    )
  }

  private def mfaCreditEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = payment.documentDate,
      description = "paymentHistory.mfaCredit",
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.documentDate, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Transaction ID"))}"
    )
  }

  private def cutOverCreditEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date - Cutover credit")),
      description = "paymentHistory.paymentFromEarlierYear",
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.dueDate.get, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Document ID"))}"
    )
  }

  private def balancingChargeCreditEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date - Balancing Charge credit")),
      description = "paymentHistory.balancingChargeCredit",
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.dueDate.get, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Document ID"))}"
    )
  }

  private def filterRepayment(repayment: RepaymentHistory)(implicit messages: Messages, languageUtils: LanguageUtils, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = LocalDate.parse(languageUtils.Dates.shortDate(repayment.estimatedRepaymentDate.getOrElse(throw MissingFieldException("Estimated Repayment Date")))(messages)),
      description = "paymentHistory.refund",
      amount = repayment.totalRepaymentAmount,
      linkUrl = s"refund-to-taxpayer/${repayment.repaymentRequestNumber}",
      visuallyHiddenText = s"${repayment.repaymentRequestNumber}"
    )
  }

}
