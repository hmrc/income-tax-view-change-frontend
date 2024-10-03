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

package controllers

import audit.AuditingService
import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.PaymentHistoryPage
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import models.admin.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits, PaymentHistoryRefunds, ReviewAndReconcilePoa}
import models.paymentCreditAndRefundHistory.PaymentCreditAndRefundHistoryViewModel
import models.repaymentHistory.RepaymentHistoryUtils
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, PaymentHistoryService, RepaymentService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.{CreditAndRefunds, PaymentHistory}
import views.html.errorPages.CustomNotFoundError

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryController @Inject()(val paymentHistoryView: PaymentHistory,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         auditingService: AuditingService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService,
                                         val auth: AuthenticatorPredicate,
                                         val retrieveBtaNavBar: NavBarPredicate,
                                         val authenticate: AuthenticationPredicate,
                                         val checkSessionTimeout: SessionTimeoutPredicate,
                                         val repaymentService: RepaymentService,
                                         val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate)
                                        (implicit val appConfig: FrontendAppConfig,
                                         dateService: DateServiceInterface,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         val view: CreditAndRefunds,
                                         val customNotFoundErrorView: CustomNotFoundError) extends ClientConfirmedController
  with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] =
    for {
      payments                       <- paymentHistoryService.getPaymentHistory
      repayments                     <- paymentHistoryService.getRepaymentHistory(isEnabled(PaymentHistoryRefunds))
    } yield (payments, repayments) match {

      case (Right(payments), Right(repayments)) =>

        auditingService.extendedAudit(PaymentHistoryResponseAuditModel(
          mtdItUser = user,
          payments = payments,
          CutOverCreditsEnabled = isEnabled(CutOverCredits),
          MFACreditsEnabled = isEnabled(MFACreditsAndDebits)
        ))

        val paymentHistoryEntries = RepaymentHistoryUtils.getGroupedPaymentHistoryData(
          isAgent = isAgent,
          payments = payments,
          repayments = repayments,
          languageUtils = languageUtils,
          CutOverCreditsEnabled = isEnabled(CutOverCredits),
          MFACreditsEnabled = isEnabled(MFACreditsAndDebits),
          reviewAndReconcileEnabled = isEnabled(ReviewAndReconcilePoa)
        )

        val viewModel = PaymentCreditAndRefundHistoryViewModel(isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds))

        Ok(paymentHistoryView(
          backUrl = backUrl,
          isAgent = isAgent,
          saUtr = user.saUtr,
          viewModel = viewModel,
          btaNavPartial = user.btaNavPartial,
          groupedPayments = paymentHistoryEntries,
          paymentHistoryAndRefundsEnabled = isEnabled(PaymentHistoryRefunds)
        ))
          .addingToSession(gatewayPage -> PaymentHistoryPage.name)

      case _ => itvcErrorHandler.showInternalServerError()
    }

  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin,
        backUrl = controllers.routes.HomeController.show(origin).url
      )
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true,
        backUrl = controllers.routes.HomeController.showAgent.url
      )
  }

  private def handleStatusRefundRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                                       (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    repaymentService.view(user.nino).flatMap {
      case _ if !isEnabled(PaymentHistoryRefunds) =>
        Future.successful(Ok(customNotFoundErrorView()(user, messages)))
      case Right(nextUrl) =>
        Future.successful(Redirect(nextUrl))
      case Left(_) =>
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  def refundStatus(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        user.userType match {
          case _ if !isEnabled(PaymentHistoryRefunds) =>
            Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
          case Some(Agent) => Future.successful(itvcErrorHandlerAgent.showInternalServerError())
          case _ =>
            handleStatusRefundRequest(
              backUrl = "",
              itvcErrorHandler = itvcErrorHandler,
              isAgent = false
            )
        }
    }
}
