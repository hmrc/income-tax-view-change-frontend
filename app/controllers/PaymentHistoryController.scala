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
import auth.authV2.AuthActions
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.GatewayPage.PaymentHistoryPage
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import models.admin.{CreditsRefundsRepay, PaymentHistoryRefunds, ReviewAndReconcilePoa}
import models.paymentCreditAndRefundHistory.PaymentCreditAndRefundHistoryViewModel
import models.repaymentHistory.RepaymentHistoryUtils
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, PaymentHistoryService, RepaymentService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory
import views.html.errorPages.CustomNotFoundError

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryController @Inject()(authActions: AuthActions,
                                         auditingService: AuditingService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService,
                                         val repaymentService: RepaymentService,
                                         paymentHistoryView: PaymentHistory,
                                         val customNotFoundErrorView: CustomNotFoundError
                                        )(implicit val appConfig: FrontendAppConfig,
                                          dateService: DateServiceInterface,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] =
    for {
      payments                       <- paymentHistoryService.getPaymentHistory
      repayments                     <- paymentHistoryService.getRepaymentHistory(isEnabled(PaymentHistoryRefunds))
    } yield (payments, repayments) match {

      case (Right(payments), Right(repayments)) =>
        auditingService.extendedAudit(PaymentHistoryResponseAuditModel(
          mtdItUser = user,
          payments = payments
        ))

        val paymentHistoryEntries = RepaymentHistoryUtils.getGroupedPaymentHistoryData(
          isAgent = isAgent,
          payments = payments,
          repayments = repayments,
          languageUtils = languageUtils,
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

      case _ => logAndHandleError("failed to get payments and/or repayments")
    }

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        origin = origin,
        backUrl = controllers.routes.HomeController.show(origin).url
      )
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        backUrl = controllers.routes.HomeController.showAgent().url
      )
  }

  def refundStatus: Action[AnyContent] =
    authActions.asMTDIndividual.async { implicit user =>
      if (isEnabled(PaymentHistoryRefunds)) {
        repaymentService.view(user.nino).map {
          case Right(nextUrl) => Redirect(nextUrl)
          case Left(ex) => logAndHandleError(ex.getMessage)
        }
      } else {
        Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
      }
    }

  def logAndHandleError(message: String)
                       (implicit mtdItUser: MtdItUser[_]): Result = {
    Logger("application").error(message)
    val errorHandler = if(mtdItUser.isAgent()) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
  }
}
