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
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.creditsandrefunds.CreditAndRefundViewModel
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditService, DateServiceInterface, IncomeSourceDetailsService, RepaymentService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import utils.CreditAndRefundUtils.UnallocatedCreditType
import utils.CreditAndRefundUtils.UnallocatedCreditType.maybeUnallocatedCreditType
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditAndRefundController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val creditService: CreditService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val repaymentService: RepaymentService,
                                          val auditingService: AuditingService,
                                          val auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          dateService: DateServiceInterface,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val view: CreditAndRefunds,
                                          val customNotFoundErrorView: CustomNotFoundError)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(origin: Option[String] = None): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show(origin).url,
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getCreditCharges()(implicitly, user) map {
      case _ if isDisabled(CreditsRefundsRepay) =>
        Ok(customNotFoundErrorView()(user, messages))
      case financialDetailsModel: List[FinancialDetailsModel] =>
        val isMFACreditsAndDebitsEnabled: Boolean = isEnabled(MFACreditsAndDebits)
        val isCutOverCreditsEnabled: Boolean = isEnabled(CutOverCredits)
        val balance: Option[BalanceDetails] = financialDetailsModel.headOption.map(balance => balance.balanceDetails)

        val credits: List[(DocumentDetailWithDueDate, FinancialDetail)] = financialDetailsModel.flatMap(
          financialDetailsModel => financialDetailsModel.getAllDocumentDetailsWithDueDatesAndFinancialDetails()
        )

        val viewModel = CreditAndRefundViewModel(credits)
        val creditAndRefundType: Option[UnallocatedCreditType] = maybeUnallocatedCreditType(credits, balance, isMFACreditsAndDebitsEnabled, isCutOverCreditsEnabled)
        auditClaimARefund(balance, credits)

        Ok(view(viewModel, balance, creditAndRefundType, isAgent, backUrl, isMFACreditsAndDebitsEnabled, isCutOverCreditsEnabled)(user, user, messages))
      case _ => Logger("application").error(
        s"${if (isAgent) "[Agent]"}[CreditAndRefundController][show] Invalid response from financial transactions")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def showAgent(): Action[AnyContent] = {
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleRequest(
          backUrl = controllers.routes.HomeController.showAgent.url,
          itvcErrorHandler = itvcErrorHandlerAgent,
          isAgent = true
        )
    }
  }

  def startRefund(): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        user.userType match {
          case _ if isDisabled(CreditsRefundsRepay) =>
            Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
          case Some(Agent) =>
            Future.successful(itvcErrorHandlerAgent.showInternalServerError())
          case _ =>
            handleRefundRequest(
              backUrl = "", // TODO: do we need a backUrl
              itvcErrorHandler = itvcErrorHandler,
              isAgent = false
            )
        }
    }

  private def handleRefundRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getCreditCharges()(implicitly, user) flatMap {
      case _ if isDisabled(CreditsRefundsRepay) =>
        Future.successful(Ok(customNotFoundErrorView()(user, messages)))

      case financialDetailsModel: List[FinancialDetailsModel] =>
        val balance: Option[BalanceDetails] = financialDetailsModel.headOption.map(balance => balance.balanceDetails)
        repaymentService.start(user.nino, balance.flatMap(_.availableCredit)).flatMap {
          case Right(nextUrl) =>
            Future.successful(Redirect(nextUrl))
          case Left(_) =>
            Future.successful(itvcErrorHandler.showInternalServerError())
        }
      case _ => Logger("application").error("[CreditAndRefundController][handleRefundRequest]")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def auditClaimARefund(balanceDetails: Option[BalanceDetails], creditDocuments: List[(DocumentDetailWithDueDate, FinancialDetail)])
                               (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {

    auditingService.extendedAudit(ClaimARefundAuditModel(
      balanceDetails = balanceDetails,
      creditDocuments = creditDocuments))
  }
}