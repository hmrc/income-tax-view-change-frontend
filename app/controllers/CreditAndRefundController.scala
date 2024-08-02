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
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.admin.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import models.creditsandrefunds.{CreditAndRefundViewModel, CreditsModel}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditService, DateServiceInterface, RepaymentService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditAndRefundController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val creditService: CreditService,
                                          val view: CreditAndRefunds,
                                          val repaymentService: RepaymentService,
                                          val auditingService: AuditingService,
                                          val auth: AuthenticatorPredicate,
                                          val customNotFoundErrorView: CustomNotFoundError)
                                         (implicit val appConfig: FrontendAppConfig,
                                          dateService: DateServiceInterface,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(origin: Option[String] = None): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show(origin).url,
          isAgent = false
        )
    }

  def handleRequest(isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    if(!isEnabled(CreditsRefundsRepay) ) {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } else {
      creditService.getAllCredits map { creditsModel: CreditsModel =>
        val isMFACreditsAndDebitsEnabled: Boolean = isEnabled(MFACreditsAndDebits)
        val isCutOverCreditsEnabled: Boolean = isEnabled(CutOverCredits)
        val viewModel = CreditAndRefundViewModel.fromCreditAndRefundModel(creditsModel)
        auditClaimARefund(creditsModel)
        Ok(view(viewModel, isAgent, backUrl, isMFACreditsAndDebitsEnabled, isCutOverCreditsEnabled)(user, user, messages))
      }
    }
  }

  def showAgent(): Action[AnyContent] = {
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleRequest(
          backUrl = controllers.routes.HomeController.showAgent.url,
          isAgent = true
        )
    }
  }

  def startRefund(): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        if (!isEnabled(CreditsRefundsRepay)) {
          Future.successful(Ok(customNotFoundErrorView()(user, user.messages)))
        } else {
          user.userType match {
            case Some(Agent) =>
              throw new AgentStartingRefundException
            case _ =>
              handleRefundRequest(
                backUrl = "", // TODO: do we need a backUrl
                isAgent = false
              )
          }
        }
    }

  private def handleRefundRequest(isAgent: Boolean, backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    if (!isEnabled(CreditsRefundsRepay)) {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } else {
      creditService.getAllCredits flatMap {
        financialDetailsModel: CreditsModel =>
          repaymentService.start(user.nino, Some(financialDetailsModel.availableCredit)).map {
            case Right(nextUrl) =>
              Redirect(nextUrl)
            case Left(ex) =>
              throw ex
          }
      }
    }
  }

  private def auditClaimARefund(creditsModel: CreditsModel)
                               (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {

    auditingService.extendedAudit(ClaimARefundAuditModel(creditsModel))
  }
}

class AgentStartingRefundException extends RuntimeException("Agent tried to start refund")
