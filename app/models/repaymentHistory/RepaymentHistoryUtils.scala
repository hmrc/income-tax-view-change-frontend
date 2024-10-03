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
import models.financialDetails._
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json
import services.DateServiceInterface
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate

object  RepaymentHistoryUtils {

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
                                   MFACreditsEnabled: Boolean, CutOverCreditsEnabled: Boolean, reviewAndReconcileEnabled: Boolean, languageUtils: LanguageUtils
                                  )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[(Int, List[PaymentHistoryEntry])] = {
    val combinedPayments = combinePaymentHistoryData(payments, repayments, isAgent,
      MFACreditsEnabled, CutOverCreditsEnabled, reviewAndReconcileEnabled, languageUtils
    )
    groupedPayments(combinedPayments)
  }

  private def combinePaymentHistoryData(payments: List[Payment],
                                        repayments: List[RepaymentHistory],
                                        isAgent: Boolean,
                                        MFACreditsEnabled: Boolean,
                                        CutOverCreditsEnabled: Boolean,
                                        reviewAndReconcileEnabled: Boolean,
                                        languageUtils: LanguageUtils
                                       )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[PaymentHistoryEntry] = {

    val filteredPayments = payments.flatMap(payment => filterPayment(payment, isAgent, MFACreditsEnabled, CutOverCreditsEnabled, reviewAndReconcileEnabled))

    val filteredRepayments = repayments.filter(_.status.isInstanceOf[Approved]).map(repayment => filterRepayment(repayment)(messages, languageUtils, dateServiceInterface))

    filteredPayments ++ filteredRepayments
  }

  private def filterPayment(payment: Payment,
                            isAgent: Boolean,
                            MFACreditsEnabled: Boolean,
                            CutOverCreditsEnabled: Boolean,
                            reviewAndReconcileEnabled: Boolean
                           )(implicit messages: Messages, dateservice: DateServiceInterface): Option[PaymentHistoryEntry] = {

    val hasCredit = payment.credit.isDefined
    val hasLot    = payment.lot.isDefined && payment.lotItem.isDefined

    payment.creditType match {
      case Some(MfaCreditType)                                       if hasCredit && MFACreditsEnabled          => Some(mfaCreditEntry(payment, isAgent))
      case Some(CutOverCreditType)                                   if hasCredit && CutOverCreditsEnabled      => Some(creditEntry(payment, isAgent))
      case Some(BalancingChargeCreditType)                           if hasCredit                               => Some(creditEntry(payment, isAgent))
      case Some(RepaymentInterest)                                   if hasCredit                               => Some(creditEntry(payment, isAgent))
      case Some(PaymentOnAccountOneReviewAndReconcileCredit)         if hasCredit && reviewAndReconcileEnabled  => Some(creditEntry(payment, isAgent))
      case Some(PaymentOnAccountTwoReviewAndReconcileCredit)         if hasCredit && reviewAndReconcileEnabled  => Some(creditEntry(payment, isAgent))
      case Some(PaymentType)                                         if !hasCredit && hasLot                    => Some(paymentToHMRCEntry(payment, isAgent))
      case _ => None
    }
  }

  private def paymentToHMRCEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    Logger("application").info("json:" + Json.prettyPrint(Json.toJson(payment)))
    PaymentHistoryEntry(
      date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date")),
      creditType = PaymentType,
      transactionId = payment.transactionId,
      amount = payment.amount,
      linkUrl = getControllerHref(payment.transactionId, isAgent),
      visuallyHiddenText = s"${payment.dueDate.get} ${payment.amount.getOrElse(throw MissingFieldException("Amount")).abs.toCurrency}"
    )
  }

  private def mfaCreditEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = payment.documentDate,
      creditType = MfaCreditType,
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.documentDate, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Transaction ID"))}"
    )
  }

  private def creditEntry(payment: Payment, isAgent: Boolean)(implicit messages: Messages, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    val creditType = payment.creditType.getOrElse(throw MissingFieldException("Credit type"))
    PaymentHistoryEntry(
      date = payment.dueDate.getOrElse(throw MissingFieldException(s"Payment Due Date - ${creditType.getClass.getSimpleName}")),
      creditType = creditType,
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.dueDate.get, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Document ID"))}"
    )
  }

  private def filterRepayment(repayment: RepaymentHistory)(implicit messages: Messages, languageUtils: LanguageUtils, dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = LocalDate.parse(languageUtils.Dates.shortDate(repayment.estimatedRepaymentDate.getOrElse(throw MissingFieldException("Estimated Repayment Date")))(messages)),
      creditType = Repayment,
      amount = repayment.totalRepaymentAmount,
      linkUrl = s"refund-to-taxpayer/${repayment.repaymentRequestNumber}",
      visuallyHiddenText = s"${repayment.repaymentRequestNumber}"
    )
  }

}
