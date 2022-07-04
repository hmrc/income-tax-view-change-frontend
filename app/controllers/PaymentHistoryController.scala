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

package controllers

import audit.AuditingService
import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CutOverCredits, FeatureSwitching, MFACreditsAndDebits, R7bTxmEvents}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys.gatewayPage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, PaymentHistoryService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.PaymentHistory
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import enums.GatewayPage.PaymentHistoryPage
import enums.{CutOverCredit, MFACredit, PaymentHistoryType, Refund, StandardPayment}
import exceptions.MissingFieldException
import models.financialDetails.Payment
import models.repaymentHistory.RepaymentHistory
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import javax.inject.Inject


case class PaymentHistoryEntry(date: String,
                               description: String,
                               amount: Option[BigDecimal],
                               transactionId: Option[String] = None,
                               paymentType: PaymentHistoryType,
                               linkUrl: String)

@Singleton
class PaymentHistoryController @Inject()(val paymentHistoryView: PaymentHistory,
                                         val checkSessionTimeout: SessionTimeoutPredicate,
                                         val authenticate: AuthenticationPredicate,
                                         val retrieveNino: NinoPredicate,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                         val incomeSourceDetailsService: IncomeSourceDetailsService,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         auditingService: AuditingService,
                                         retrieveBtaNavBar: NavBarPredicate,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService,
                                         val languageUtils: LanguageUtils)
                                        (implicit override val mcc: MessagesControllerComponents,
                                         implicit val ec: ExecutionContext,
                                         implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    paymentHistoryService.getPaymentHistory.flatMap {
      case Right(payments) =>
        paymentHistoryService.getRepaymentHistory.map {
          case Right(repayments) =>
            println("REFUNDS" + repayments)
            auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments, R7bTxmEvents = isEnabled(R7bTxmEvents),
              CutOverCreditsEnabled = isEnabled(CutOverCredits),
              MFACreditsEnabled = isEnabled(MFACreditsAndDebits)))
            val paymentHistoryEntries = combinePaymentHistoryData(payments, repayments, isAgent)
            val groupedPaymentHistoryEntries = groupedPayments(paymentHistoryEntries)
            Ok(paymentHistoryView(groupedPaymentHistoryEntries, CutOverCreditsEnabled = isEnabled(CutOverCredits), backUrl, user.saUtr,
              btaNavPartial = user.btaNavPartial, isAgent = isAgent, MFACreditsEnabled = isEnabled(MFACreditsAndDebits))
            ).addingToSession(gatewayPage -> PaymentHistoryPage.name)
          case Left(_) => itvcErrorHandler.showInternalServerError()
        }

      case Left(_) => Future(itvcErrorHandler.showInternalServerError())
    }
  }

  private def sortPayments(payments: List[PaymentHistoryEntry]) = {
    payments
      .map(payment => (LocalDate.parse(payment.date).toEpochDay, payment))
      .sortWith((left, right) => left._1 < right._1)
      .map { case (_, payments) => payments }
  }

  def groupedPayments(payments: List[PaymentHistoryEntry]): List[(Int, List[PaymentHistoryEntry])] = {
    payments.groupBy[Int] { payment => {
      LocalDate.parse(payment.date).getYear
    }
    }.toList.sortBy(_._1).reverse
      .map { case (year, payments) => (year, sortPayments(payments)) }
  }

  def getControllerHref(transactionId: Option[String], isAgent: Boolean) = {
    if (isAgent) {
      controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent(transactionId.getOrElse(throw MissingFieldException("Document ID"))).url
    } else {
      controllers.routes.PaymentAllocationsController.viewPaymentAllocation(transactionId.getOrElse(throw MissingFieldException("Document ID"))).url
    }
  }

  def getMFACreditsLink(date: String, isAgent: Boolean) = {
    val year = LocalDate.parse(date).getYear
    if (isAgent) {
      "/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/" + year
    } else {
      "/report-quarterly/income-and-expenses/view/credits-from-hmrc/" + year
    }
  }

  def combinePaymentHistoryData(payments: List[Payment], repayments: List[RepaymentHistory], isAgent: Boolean)(implicit messages: Messages)
    : List[PaymentHistoryEntry] = {
    val filteredPayments = payments.flatMap(payment => {
      if (payment.credit.isEmpty) {
        Some(PaymentHistoryEntry(
          date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date")),
          description = "paymentHistory.paymentToHmrc",
          transactionId = payment.transactionId,
          amount = payment.amount,
          paymentType = StandardPayment,
          linkUrl = getControllerHref(payment.transactionId, isAgent)
        ))
      } else {
        if (payment.validMFACreditDescription()) {
          if (isEnabled(MFACreditsAndDebits)) {
            Some(PaymentHistoryEntry(
              date = payment.documentDate,
              description = "paymentHistory.mfaCredit",
              amount = payment.amount,
              paymentType = MFACredit,
              linkUrl = getMFACreditsLink(payment.documentDate, isAgent)
            ))
          } else None
        } else {
          if (isEnabled(CutOverCredits)) {
            Some(PaymentHistoryEntry(
              date = payment.dueDate.getOrElse(throw MissingFieldException("Payment Due Date Cutover credit")),
              description = "paymentHistory.paymentFromEarlierYear",
              amount = payment.amount,
              paymentType = CutOverCredit,
              linkUrl = getControllerHref(payment.transactionId, isAgent)
            ))
          } else None
        }
      }
    })

    val filteredRepayments = repayments.flatMap(repayment => {
      Some(PaymentHistoryEntry(
        date = languageUtils.Dates.shortDate(repayment.estimatedRepaymentDate)(messages),
        description = "paymentHistory.refund",
        amount = repayment.amountApprovedforRepayment,
        paymentType = Refund,
        linkUrl = "refund-to-taxpayer"
      ))
    })
    filteredPayments ++ filteredRepayments
  }

  def show(origin: Option[String] = None): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {

    implicit user =>
      handleRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin,
        backUrl = controllers.routes.HomeController.show(origin).url
      )
  }

  def showAgent(): Action[AnyContent] = {
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap {
            implicit mtdItUser =>
              handleRequest(
                itvcErrorHandler = itvcErrorHandlerAgent,
                isAgent = true,
                backUrl = controllers.routes.HomeController.showAgent().url
              )
          }
    }
  }


}

