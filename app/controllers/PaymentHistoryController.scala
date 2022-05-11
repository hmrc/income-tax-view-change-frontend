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
import config.featureswitch.{CutOverCredits, FeatureSwitching, R7bTxmEvents}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys.gatewayPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, PaymentHistoryService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.PaymentHistory
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import enums.GatewayPage.PaymentHistoryPage

@Singleton
class PaymentHistoryController @Inject()(val paymentHistoryView: PaymentHistory,
                                         val checkSessionTimeout: SessionTimeoutPredicate,
                                         val authenticate: AuthenticationPredicate,
                                         val retrieveNino: NinoPredicate,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                         val incomeSourceDetailsService: IncomeSourceDetailsService,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         auditingService: AuditingService,
                                         retrieveBtaNavBar: NavBarPredicate,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService)
                                        (implicit mcc: MessagesControllerComponents,
                                         implicit val ec: ExecutionContext,
                                         implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    origin: Option[String] = None,
                    isAgent: Boolean,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    paymentHistoryService.getPaymentHistory.map {
      case Right(payments) =>
          auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments, R7bTxmEvents=isEnabled(R7bTxmEvents), CutOverCreditsEnabled=isEnabled(CutOverCredits)))

        Ok(paymentHistoryView(payments, CutOverCreditsEnabled=isEnabled(CutOverCredits),backUrl, user.saUtr,
          btaNavPartial = user.btaNavPartial, isAgent = isAgent)).addingToSession(gatewayPage -> PaymentHistoryPage.name)
      case Left(_) => itvcErrorHandler.showInternalServerError()
    }

  }

  def show(origin: Option[String] = None): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin,
        backUrl = controllers.routes.HomeController.show(origin).url
      )
  }

  def showAgent(): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            itvcErrorHandler = itvcErrorHandlerAgent,
            isAgent = true,
            backUrl = controllers.routes.HomeController.showAgent().url
          )
        }
    }
  }


}

