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
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.RepaymentHistoryConnector
import models.admin.PaymentHistoryRefunds
import models.core.Nino
import models.creditsandrefunds.RefundToTaxPayerViewModel
import models.repaymentHistory.{RepaymentHistoryErrorModel, RepaymentHistoryModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.RefundToTaxPayerView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RefundToTaxPayerController @Inject()(val refundToTaxPayerView: RefundToTaxPayerView,
                                           val repaymentHistoryConnector: RepaymentHistoryConnector,
                                           val authActions: AuthActions,
                                           itvcErrorHandler: ItvcErrorHandler,
                                           itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           auditingService: AuditingService)
                                          (implicit mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext,
                                           val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    itvcErrorHandler: ShowInternalServerError,
                    repaymentRequestNumber: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isEnabled(PaymentHistoryRefunds)) {
      repaymentHistoryConnector.getRepaymentHistoryByRepaymentId(Nino(user.nino), repaymentRequestNumber).map {
        case repaymentHistoryModel: RepaymentHistoryModel =>
          repaymentHistoryModel.repaymentsViewerDetails.headOption match {
            case Some(repaymentHistory) =>
              auditingService.extendedAudit(RefundToTaxPayerResponseAuditModel(repaymentHistoryModel))
              val viewModelMaybe = RefundToTaxPayerViewModel.createViewModel(repaymentHistory)
              viewModelMaybe match {
                case Left(error) =>
                  Logger("application").error(s"error constructing view model: ${error.getMessage}")
                  itvcErrorHandler.showInternalServerError()
                case Right(viewModel: RefundToTaxPayerViewModel) =>
                  Ok(
                    refundToTaxPayerView(
                      viewModel,
                      paymentHistoryRefundsEnabled = true,
                      backUrl, user.saUtr,
                      btaNavPartial = user.btaNavPartial,
                      isAgent = user.isAgent()))
              }
            case None => {
              Logger("application").error(s"No repayment details returned")
              itvcErrorHandler.showInternalServerError()
            }
          }
        case error: RepaymentHistoryErrorModel =>
          Logger("application")
            .error(s"${if (user.isAgent()) "[Agent] "}Could not retrieve repayment history" +
              s" for repaymentRequestNumber: $repaymentRequestNumber - ${error.message} - ${error.code}")
          itvcErrorHandler.showInternalServerError()
      }
    } else {
      Future.successful(Redirect(homeUrl(user.isAgent())))
    }
  }

  def show(repaymentRequestNumber: String,
           origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.PaymentHistoryController.show(origin).url,
        origin = origin,
        itvcErrorHandler = itvcErrorHandler,
        repaymentRequestNumber = repaymentRequestNumber
      )
  }

  def showAgent(repaymentRequestNumber: String): Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.PaymentHistoryController.showAgent().url,
        itvcErrorHandler = itvcErrorHandlerAgent,
        repaymentRequestNumber = repaymentRequestNumber
      )
  }

  lazy val homeUrl: Boolean => String = isAgent => if (isAgent) {
    controllers.routes.HomeController.showAgent().url
  } else {
    controllers.routes.HomeController.show().url
  }
}
