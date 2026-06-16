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

package financials.controllers

import common.auth.{AuthActions, MtdItUser}
import common.config.featureswitch.FeatureSwitching
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.models.admin.CreditsRefundsRepay
import common.services.AuditingService
import common.views.html.errorPages.CustomNotFoundErrorView
import financials.models.audit.ClaimARefundAuditModel
import financials.services.{CreditService, RepaymentService}
import financials.utils.ErrorRecovery
import models.creditsandrefunds.{CreditsModel, MoneyInYourAccountViewModel}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import financials.views.html.{CreditAndRefundsView, MoneyInYourAccountView}

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class MoneyInYourAccountController @Inject()(val authActions: AuthActions,
                                             val creditService: CreditService,
                                             val view: CreditAndRefundsView,
                                             val moneyInYourAccountView: MoneyInYourAccountView,
                                             val repaymentService: RepaymentService,
                                             val auditingService: AuditingService,
                                             mcc: MessagesControllerComponents)
                                            (implicit val appConfig: FrontendAppConfig,
                                          val individualErrorHandler: ItvcErrorHandler,
                                          val agentErrorHandler: AgentItvcErrorHandler,
                                          val languageUtils: LanguageUtils,
                                          val ec: ExecutionContext,
                                          val customNotFoundErrorView: CustomNotFoundErrorView)
  extends FrontendController(mcc)
    with FeatureSwitching
    with I18nSupport
    with ErrorRecovery {

  def show(origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual().async {
      implicit user =>
        handleRequest(
          backUrl = hub.controllers.routes.HomeController.show(origin).url
        ) recover logAndRedirect
    }

  def handleRequest(backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getAllCredits map {
      case _ if !isEnabled(CreditsRefundsRepay) =>
        Ok(customNotFoundErrorView()(user, messages))
      case creditsModel: CreditsModel =>
        val viewModel = MoneyInYourAccountViewModel.fromCreditsModel(creditsModel, appConfig.repaymentsUrl)
        auditClaimARefund(creditsModel)
        Ok(moneyInYourAccountView(viewModel, backUrl)(user, user, messages))
    }
  }

  def showAgent(): Action[AnyContent] = {
    authActions.asMTDPrimaryAgent() async {
      implicit mtdItUser =>
        handleRequest(
          backUrl = hub.controllers.routes.HomeController.showAgent().url
        ) recover logAndRedirect
    }
  }

  def startRefund(): Action[AnyContent] =
    authActions.asMTDIndividual() async {
      implicit user =>
        if (isEnabled(CreditsRefundsRepay)) {
          handleRefundRequest(
            backUrl = "", // TODO: do we need a backUrl
            isAgent = false
          ) recover logAndRedirect
        } else {
          Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
        }
    }

  def startRefundAgents(): Action[AnyContent] =
    authActions.asMTDPrimaryAgent() async {
      implicit user =>
        if (isEnabled(CreditsRefundsRepay)) {
          handleRefundRequest(
            backUrl = "", // TODO: do we need a backUrl
            isAgent = true
          ) recover logAndRedirect
        } else {
          Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
        }
    }

  private def handleRefundRequest(@unused isAgent: Boolean, @unused backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getAllCredits flatMap {
      case _ if !isEnabled(CreditsRefundsRepay) =>
        Future.successful(Ok(customNotFoundErrorView()(user, messages)))
      case creditsModel: CreditsModel =>
        repaymentService.start(user.nino, Some(creditsModel.availableCreditForRepayment)) map {
          case Right(nextUrl) =>
            Redirect(nextUrl)
          case Left(ex) =>
            logAndRedirect(ex.getLocalizedMessage)
        }
    }
  }

  private def auditClaimARefund(creditsModel: CreditsModel)
                               (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {

    auditingService.extendedAudit(ClaimARefundAuditModel(creditsModel))
  }
}