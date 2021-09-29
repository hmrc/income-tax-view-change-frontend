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
import audit.models.PaymentAllocationsResponseAuditModel
import config.featureswitch.{FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.core.Nino
import models.paymentAllocationCharges.PaymentAllocationViewModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{IncomeSourceDetailsService, PaymentAllocationsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.PaymentAllocation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(paymentAllocationView: PaymentAllocation,
                                             paymentAllocationsService: PaymentAllocationsService,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             auditingService: AuditingService,
                                             incomeSourceDetailsService: IncomeSourceDetailsService
                                            )(implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              itvcErrorHandler: ItvcErrorHandler
                                            ) extends ClientConfirmedController with FeatureSwitching {

  lazy val backUrl: String = controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url

  def viewPaymentAllocation(documentNumber: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(PaymentAllocation)) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { implicit mtdItUser =>
          paymentAllocationsService.getPaymentAllocation(Nino(getClientNino(request)), documentNumber) map {
            case Right(viewModel: PaymentAllocationViewModel) =>
              if (isEnabled(TxmEventsApproved)) {
                auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(mtdItUser, viewModel))
              }
              Ok(paymentAllocationView(viewModel, backUrl = backUrl))
            case _ => itvcErrorHandler.showInternalServerError()
          }
        }
      } else Future.failed(new NotFoundException("[PaymentAllocationsController] - PaymentAllocation is disabled"))
  }

}
