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
import enums.GatewayPage.PaymentHistoryPage
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import models.paymentCreditAndRefundHistory.PaymentCreditAndRefundHistoryViewModel
import models.repaymentHistory.RepaymentHistoryUtils
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PaymentHistoryService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.PaymentHistory

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryController @Inject()(val paymentHistoryView: PaymentHistory,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         auditingService: AuditingService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService,
                                         val languageUtils: LanguageUtils,
                                         val auth: AuthenticatorPredicate)
                                        (implicit override val mcc: MessagesControllerComponents,
                                         implicit val ec: ExecutionContext,
                                         implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
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
              Ok(paymentHistoryView(paymentHistoryEntries, viewModel, backUrl, user.saUtr,
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
          Future(Ok(paymentHistoryView(paymentHistoryEntries, viewModel, backUrl, user.saUtr,
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
}
