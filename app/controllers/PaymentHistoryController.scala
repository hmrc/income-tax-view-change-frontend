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
import models.paymentCreditAndRefundHistory.PaymentCreditAndRefundHistoryViewModel
import models.repaymentHistory.RepaymentHistoryUtils
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateService, DateServiceInterface, PaymentHistoryService, RepaymentService}
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
                                         dateService: DateService,
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
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    paymentHistoryService.getPaymentHistory.flatMap {

      case Right(payments) =>
        val MFACreditsEnabled = isEnabled(MFACreditsAndDebits)
        val CutOverCreditsEnabled = isEnabled(CutOverCredits)
        val paymentHistoryAndRefundsEnabled = isEnabled(PaymentHistoryRefunds)
        val viewModel = PaymentCreditAndRefundHistoryViewModel(isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds))
        if (paymentHistoryAndRefundsEnabled) {
          paymentHistoryService.getRepaymentHistory.map {
            case Right(repayments) =>
              auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments,
                CutOverCreditsEnabled = CutOverCreditsEnabled,
                MFACreditsEnabled = MFACreditsEnabled))
              val paymentHistoryEntries = RepaymentHistoryUtils.getGroupedPaymentHistoryData(payments, repayments, isAgent,
                MFACreditsEnabled = MFACreditsEnabled, CutOverCreditsEnabled = CutOverCreditsEnabled, languageUtils)
              Ok(paymentHistoryView(paymentHistoryEntries, viewModel, paymentHistoryAndRefundsEnabled, backUrl, user.saUtr,
                btaNavPartial = user.btaNavPartial, isAgent = isAgent)
              ).addingToSession(gatewayPage -> PaymentHistoryPage.name)
            case Left(_) => itvcErrorHandler.showInternalServerError()
          }
        } else {
          auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments,
            CutOverCreditsEnabled = CutOverCreditsEnabled,
            MFACreditsEnabled = MFACreditsEnabled))
          val paymentHistoryEntries = RepaymentHistoryUtils.getGroupedPaymentHistoryData(payments, List(), isAgent,
            MFACreditsEnabled = MFACreditsEnabled, CutOverCreditsEnabled = CutOverCreditsEnabled, languageUtils)
          Future(Ok(paymentHistoryView(paymentHistoryEntries, viewModel, paymentHistoryAndRefundsEnabled, backUrl, user.saUtr,
            btaNavPartial = user.btaNavPartial, isAgent = isAgent)
          ).addingToSession(gatewayPage -> PaymentHistoryPage.name))
        }

      case Left(_) => Future(itvcErrorHandler.showInternalServerError())
    }
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
      case _ if isDisabled(PaymentHistoryRefunds) =>
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
          case _ if isDisabled(PaymentHistoryRefunds) =>
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
