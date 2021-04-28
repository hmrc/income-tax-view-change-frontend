/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent

import audit.AuditingService
import audit.models.{PaymentHistoryRequestAuditModel, PaymentHistoryResponseAuditModel}
import config.featureswitch.{AgentViewer, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{IncomeSourceDetailsService, PaymentHistoryService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.AgentsPaymentHistory

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentHistoryController @Inject()(agentsPaymentHistory: AgentsPaymentHistory,
                                         auditingService: AuditingService,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         paymentHistoryService: PaymentHistoryService)
                                        (implicit val appConfig: FrontendAppConfig,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         dateFormatter: ImplicitDateFormatterImpl,
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def viewPaymentHistory(): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (isEnabled(AgentViewer)) {
          for {
            mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService)
            _ = auditingService.extendedAudit(PaymentHistoryRequestAuditModel(mtdItUser))
            paymentHistoryResponse <- paymentHistoryService.getPaymentHistory(implicitly, mtdItUser)
          } yield {
            paymentHistoryResponse match {
              case Right(payments) =>
                auditingService.extendedAudit(PaymentHistoryResponseAuditModel(mtdItUser, payments))
                Ok(agentsPaymentHistory(payments, dateFormatter, backUrl, mtdItUser.saUtr))
              case Left(_) => itvcErrorHandler.showInternalServerError()
            }
          }
        } else {
          Future.failed(new NotFoundException("[PaymentHistoryController] - Agent viewer is disabled"))
        }
    }

  def backUrl: String = controllers.agent.routes.HomeController.show().url


}
