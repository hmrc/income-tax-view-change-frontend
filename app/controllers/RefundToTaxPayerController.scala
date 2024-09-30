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
import audit.models.RefundToTaxPayerResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.RepaymentHistoryConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.admin.PaymentHistoryRefunds
import models.core.Nino
import models.repaymentHistory.RepaymentHistoryModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.RefundToTaxPayer

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RefundToTaxPayerController @Inject()(val refundToTaxPayerView: RefundToTaxPayer,
                                           val repaymentHistoryConnector: RepaymentHistoryConnector,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           itvcErrorHandler: ItvcErrorHandler,
                                           auditingService: AuditingService,
                                           val auth: AuthenticatorPredicate)
                                          (implicit mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext,
                                           val appConfig: FrontendAppConfig,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean,
                    itvcErrorHandler: ShowInternalServerError,
                    repaymentRequestNumber: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isEnabled(PaymentHistoryRefunds)) {
      (for {
        repaymentHistoryModel <- repaymentHistoryConnector.getRepaymentHistoryByRepaymentId(Nino(user.nino), repaymentRequestNumber).collect {
          case repaymentHistoryModel: RepaymentHistoryModel => repaymentHistoryModel
        }
      } yield {
        auditingService.extendedAudit(RefundToTaxPayerResponseAuditModel(repaymentHistoryModel))
        Ok(
          refundToTaxPayerView(
            repaymentHistoryModel,
            paymentHistoryRefundsEnabled = isEnabled(PaymentHistoryRefunds),
            backUrl, user.saUtr,
            btaNavPartial = user.btaNavPartial,
            isAgent = isAgent))
      }).recover {
        case ex if isAgent =>
          Logger("application")
            .error(s"Could not retrieve repayment history for repaymentRequestNumber: $repaymentRequestNumber - ${ex.getMessage} - ${ex.getCause}")
          itvcErrorHandlerAgent.showInternalServerError()
        case ex =>
          Logger("application")
            .error(s"[Agent]Could not retrieve repayment history for repaymentRequestNumber: $repaymentRequestNumber - ${ex.getMessage} - ${ex.getCause}")
          itvcErrorHandler.showInternalServerError()
      }
    } else {
      Future.successful(Redirect(controllers.routes.HomeController.show().url))
    }
  }

  def show(repaymentRequestNumber: String, origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.PaymentHistoryController.show(origin).url,
        origin = origin,
        isAgent = false,
        itvcErrorHandler = itvcErrorHandler,
        repaymentRequestNumber = repaymentRequestNumber
      )
  }

  def showAgent(repaymentRequestNumber: String): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.PaymentHistoryController.showAgent().url,
        isAgent = true,
        itvcErrorHandler = itvcErrorHandlerAgent,
        repaymentRequestNumber = repaymentRequestNumber
      )
  }
}

