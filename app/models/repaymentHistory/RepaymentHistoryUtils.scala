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
import services.{DateService, DateServiceInterface}
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

  def getChargeLinkUrl(isAgent: Boolean, taxYear: Int, chargeId: String): String = {
    if (isAgent) {
      controllers.routes.ChargeSummaryController.showAgent(taxYear, chargeId).url
    } else {
      controllers.routes.ChargeSummaryController.show(taxYear, chargeId).url
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

  def getGroupedPaymentHistoryData(payments: List[Payment], repayments: List[RepaymentHistory], codedOutCharges: List[ChargeItem],
                                   isAgent: Boolean, languageUtils: LanguageUtils
                                  )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[(Int, List[PaymentHistoryEntry])] = {

    val combinedPayments = combinePaymentHistoryData(payments, repayments, codedOutCharges, isAgent, languageUtils)

    groupedPayments(combinedPayments)
  }

  private def combinePaymentHistoryData(payments: List[Payment],
                                        repayments: List[RepaymentHistory],
                                        codedOutCharges: List[ChargeItem],
                                        isAgent: Boolean,
                                        languageUtils: LanguageUtils
                                       )(implicit messages: Messages, dateServiceInterface: DateServiceInterface): List[PaymentHistoryEntry] = {

    val filteredPayments = payments.flatMap { payment => filterPayment(payment, isAgent) match {
        case Right(entry) => Some(entry)
        case Left(error) =>
          Logger("application").error(s"Error processing payment: ${error.getMessage}")
          None
      }
    }

    val codedOutChargesList = codedOutCharges.map(codedOutChargeEntry(_, isAgent))

    val filteredRepayments = repayments.filter(_.status.isInstanceOf[Approved]).map(repayment => filterRepayment(repayment)(messages, languageUtils, dateServiceInterface))

    filteredPayments ++ filteredRepayments ++ codedOutChargesList
  }

  private def filterPayment(payment: Payment,
                            isAgent: Boolean
                           )(implicit dateservice: DateServiceInterface): Either[Throwable, PaymentHistoryEntry] = {

    val hasCredit = payment.credit.isDefined
    val hasLot = payment.lot.isDefined && payment.lotItem.isDefined

    (hasCredit,  hasLot, payment.creditType) match {
      case (true, _, Some(MfaCreditType))                                   => Right(mfaCreditEntry(payment, isAgent))
      case (true, _, Some(CutOverCreditType))                               => creditEntry(payment, isAgent)
      case (true, _, Some(PoaOneReconciliationCredit))  => creditEntry(payment, isAgent, true)
      case (true, _, Some(PoaTwoReconciliationCredit))  => creditEntry(payment, isAgent, true)
      case (true, _, Some(BalancingChargeCreditType))                       => creditEntry(payment, isAgent)
      case (true, _, Some(RepaymentInterest))                               => creditEntry(payment, isAgent)
      case (false, true, Some(PaymentType))                                 => Right(paymentToHMRCEntry(payment, isAgent))
      case (_, _, _)                                                        => Left(MissingFieldException("Invalid Payment Data"))
    }
  }

  private def paymentToHMRCEntry(payment: Payment, isAgent: Boolean)
                                (implicit dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
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

  private def mfaCreditEntry(payment: Payment, isAgent: Boolean)(implicit dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = payment.documentDate,
      creditType = MfaCreditType,
      amount = payment.amount,
      linkUrl = getCreditsLinkUrl(payment.documentDate, isAgent),
      visuallyHiddenText = s"${payment.transactionId.getOrElse(throw MissingFieldException("Transaction ID"))}"
    )
  }

  private def creditEntry(payment: Payment, isAgent: Boolean, isPoaReconciliationCredit: Boolean = false)
                         (implicit dateServiceInterface: DateServiceInterface): Either[Throwable, PaymentHistoryEntry] = {
    for {
      creditType <- payment.creditType.toRight(MissingFieldException("Credit type"))
      dueDate <- payment.dueDate.toRight(MissingFieldException(s"Payment Due Date - ${creditType.getClass.getSimpleName}"))
      amount = payment.amount
      transactionId <- payment.transactionId.toRight(MissingFieldException(
        if (isPoaReconciliationCredit) "Transaction ID" else "Document ID"))
    } yield PaymentHistoryEntry(
      date = dueDate,
      creditType = creditType,
      amount = amount,
      linkUrl = if (isPoaReconciliationCredit)
        getChargeLinkUrl(isAgent, payment.documentDate.getYear, transactionId)
      else
        getCreditsLinkUrl(dueDate, isAgent),
      visuallyHiddenText = transactionId
    )
  }

  private def codedOutChargeEntry(chargeItem: ChargeItem, isAgent: Boolean)(implicit dateServiceInterface: DateServiceInterface): PaymentHistoryEntry = {
    PaymentHistoryEntry(
      date = chargeItem.lastUpdated.getOrElse(chargeItem.documentDate),
      creditType = chargeItem.transactionType,
      amount = Some(chargeItem.originalAmount),
      transactionId = Some(chargeItem.transactionId),
      linkUrl = getChargeLinkUrl(isAgent, chargeItem.taxYear.toFinancialYearEnd.getYear, chargeItem.transactionId),
      visuallyHiddenText = chargeItem.transactionType.toString
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
