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
import audit.models.ClaimARefundAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.predicates._
import models.admin.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import models.creditsandrefunds.{CreditAndRefundViewModel, CreditsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditService, IncomeSourceDetailsService, RepaymentService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.ErrorRecovery
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditAndRefundController @Inject()(val authActions: AuthActions,
                                          val creditService: CreditService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                          val individualErrorHandler: ItvcErrorHandler,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val repaymentService: RepaymentService,
                                          val auditingService: AuditingService,
                                          val controllerComponents: MessagesControllerComponents)
                                         (implicit val appConfig: FrontendAppConfig,
                                          val languageUtils: LanguageUtils,
                                          val ec: ExecutionContext,
                                          val agentErrorHandler: AgentItvcErrorHandler,
                                          val view: CreditAndRefunds,
                                          val customNotFoundErrorView: CustomNotFoundError)
  extends FrontendBaseController with FeatureSwitching with I18nSupport with ErrorRecovery {

  def show(origin: Option[String] = None): Action[AnyContent] =
    authActions.individualOrAgentWithClient async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show(origin).url,
          itvcErrorHandler = individualErrorHandler,
          isAgent = false
        ) recover logAndRedirect
    }

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getAllCredits map {
      case _ if !isEnabled(CreditsRefundsRepay) =>
        Ok(customNotFoundErrorView()(user, messages))
      case creditsModel: CreditsModel =>
        val isMFACreditsAndDebitsEnabled: Boolean = isEnabled(MFACreditsAndDebits)
        val isCutOverCreditsEnabled: Boolean = isEnabled(CutOverCredits)
        val viewModel = CreditAndRefundViewModel.fromCreditAndRefundModel(creditsModel)

        auditClaimARefund(creditsModel)

        Ok(view(viewModel, isAgent, backUrl, isMFACreditsAndDebitsEnabled, isCutOverCreditsEnabled)(user, user, messages))
      case _ => logAndRedirect("Invalid response from financial transactions")
    }
  }

  def showAgent(): Action[AnyContent] = {
    authActions.individualOrAgentWithClient async {
      implicit mtdItUser =>
        handleRequest(
          backUrl = controllers.routes.HomeController.showAgent.url,
          itvcErrorHandler = agentErrorHandler,
          isAgent = true
        ) recover logAndRedirect
    }
  }

  def startRefund(): Action[AnyContent] =
    authActions.individualOrAgentWithClient async {
      implicit user =>
        user.userType match {
          case _ if !isEnabled(CreditsRefundsRepay) =>
            Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
          case Some(Agent) =>
            Future.successful(agentErrorHandler.showInternalServerError())
          case _ =>
            handleRefundRequest(
              backUrl = "", // TODO: do we need a backUrl
              itvcErrorHandler = individualErrorHandler,
              isAgent = false
            ) recover logAndRedirect
        }
    }

  private def handleRefundRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getAllCredits flatMap {
      case _ if !isEnabled(CreditsRefundsRepay) =>
        Future.successful(Ok(customNotFoundErrorView()(user, messages)))

      case financialDetailsModel: CreditsModel =>
        repaymentService.start(user.nino, Some(financialDetailsModel.availableCredit)) map {
          case Right(nextUrl) =>
            Redirect(nextUrl)
          case Left(_) =>
            itvcErrorHandler.showInternalServerError()
        }
      case _ => Logger("application").error("")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def auditClaimARefund(creditsModel: CreditsModel)
                               (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {

    auditingService.extendedAudit(ClaimARefundAuditModel(creditsModel))
  }
}