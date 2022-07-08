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
import config.featureswitch.{CutOverCredits, FeatureSwitching, MFACreditsAndDebits, PaymentHistoryRefunds, R7bTxmEvents}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.IncomeTaxViewChangeConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.PaymentHistoryPage
import forms.utils.SessionKeys.gatewayPage
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryErrorModel, RepaymentHistoryModel, RepaymentHistoryResponseModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, PaymentHistoryService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RefundToTaxPayer

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RefundToTaxPayerController @Inject()(val refundToTaxPayerView: RefundToTaxPayer,
                                           val checkSessionTimeout: SessionTimeoutPredicate,
                                           val authenticate: AuthenticationPredicate,
                                           val retrieveNino: NinoPredicate,
                                           val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           val incomeSourceDetailsService: IncomeSourceDetailsService,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           auditingService: AuditingService,
                                           retrieveBtaNavBar: NavBarPredicate,
                                           itvcErrorHandler: ItvcErrorHandler,
                                           paymentHistoryService: PaymentHistoryService)
                                          (implicit mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext,
                                           val appConfig: FrontendAppConfig,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private def getRepaymentHistoryModel(repaymentRequestNumber: String, isAgent: Boolean)(callback: RepaymentHistoryModel => Future[Result])
                                      (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    incomeTaxViewChangeConnector.getRepaymentHistoryByRepaymentId(Nino(user.nino), repaymentRequestNumber).flatMap {
      case repaymentHistoryModel: RepaymentHistoryModel => {
        callback(repaymentHistoryModel)
      }

      // TODO do we need it
//      case RepaymentHistoryErrorModel(NOT_FOUND, _) => callback(List.empty)

      case _ if isAgent =>
        Logger("application").error(s"[RefundToTaxPayerController][withTaxYearFinancials] - Could not retrieve repayment history for repaymentRequestNumber: $repaymentRequestNumber")
        Future.successful(itvcErrorHandlerAgent.showInternalServerError())
      case _ =>
        Logger("application").error(s"[Agent][RefundToTaxPayerController][withTaxYearFinancials] - Could not retrieve repayment history for repaymentRequestNumber: $repaymentRequestNumber")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean,
                    itvcErrorHandler: ShowInternalServerError,
                    repaymentRequestNumber: String)
                   (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    if (isEnabled(PaymentHistoryRefunds)) {
      // TODO implement it
      getRepaymentHistoryModel(repaymentRequestNumber, isAgent) { repaymentHistoryModel =>
        println(s"@@@@@@@@@@@@@@@@@@@@@ repaymentHistoryModel = $repaymentHistoryModel @@@@@@@@@@@@@@@@@@@@@@@@@@")

        Future.successful(Ok(
          refundToTaxPayerView(
            repaymentHistoryModel,
            paymentHistoryRefundsEnabled = isEnabled(PaymentHistoryRefunds),
            backUrl, user.saUtr,
            btaNavPartial = user.btaNavPartial,
            isAgent = isAgent)))
      }
    } else {
      Future.successful(Redirect(controllers.routes.HomeController.show().url))
    }

    /*paymentHistoryService.getPaymentHistory.map {
      case Right(payments) =>
        auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments, R7bTxmEvents = isEnabled(R7bTxmEvents),
          CutOverCreditsEnabled = isEnabled(CutOverCredits),
          MFACreditsEnabled = isEnabled(MFACreditsAndDebits)))

        Ok(refundToTaxPayerView(payments, paymentHistoryRefundsEnabled = isEnabled(PaymentHistoryRefunds), backUrl, user.saUtr,
          btaNavPartial = user.btaNavPartial, isAgent = isAgent)
        ).addingToSession(gatewayPage -> PaymentHistoryPage.name)
      case Left(_) => itvcErrorHandler.showInternalServerError()
    }*/


  }

  def show(repaymentRequestNumber: String, origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.PaymentHistoryController.show(origin).url,
          origin = origin,
          isAgent = false,
          itvcErrorHandler = itvcErrorHandler,
          repaymentRequestNumber = repaymentRequestNumber
        )
    }

  def showAgent(repaymentRequestNumber: String): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            backUrl = controllers.routes.PaymentHistoryController.showAgent().url,
            isAgent = true,
            itvcErrorHandler = itvcErrorHandlerAgent,
            repaymentRequestNumber = repaymentRequestNumber
          )
        }
    }
  }


}

