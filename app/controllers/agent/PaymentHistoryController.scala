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

package controllers.agent

import audit.AuditingService
import audit.models.PaymentHistoryResponseAuditModel
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{IncomeSourceDetailsService, PaymentHistoryService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentHistoryController @Inject()(paymentHistory: PaymentHistory,
                                         auditingService: AuditingService,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         paymentHistoryService: PaymentHistoryService)
                                        (implicit val appConfig: FrontendAppConfig,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport {

  def viewPaymentHistory(): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        for {
          mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true)
          paymentHistoryResponse <- paymentHistoryService.getPaymentHistory(implicitly, mtdItUser)
        } yield {
          paymentHistoryResponse match {
            case Right(payments) =>
              auditingService.extendedAudit(PaymentHistoryResponseAuditModel(mtdItUser, payments))
              Ok(paymentHistory(payments, backUrl, mtdItUser.saUtr, isAgent = true))
            case Left(_) => itvcErrorHandler.showInternalServerError()
          }
        }
    }

  def backUrl: String = controllers.routes.HomeController.showAgent().url


}
