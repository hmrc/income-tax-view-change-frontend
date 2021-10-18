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
import audit.models.InitiatePayNowAuditModel
import auth.FrontendAuthorisedFunctions
import config.{FrontendAppConfig, ItvcErrorHandler}
import connectors.agent.PayApiConnector
import controllers.agent.predicates.ClientConfirmedController
import javax.inject.{Inject, Singleton}
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeSourceDetailsService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentController @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService,
                                  payApiConnector: PayApiConnector,
                                  val auditingService: AuditingService,
                                  val authorisedFunctions: FrontendAuthorisedFunctions
                                 )(implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: ItvcErrorHandler
                                 ) extends ClientConfirmedController {


  val paymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          mtdItUser =>
            auditingService.extendedAudit(
              InitiatePayNowAuditModel(mtdItUser.mtditid, Some(mtdItUser.nino), mtdItUser.saUtr, mtdItUser.credId, mtdItUser.userType)
            )
            mtdItUser.saUtr match {
              case Some(utr) =>
                payApiConnector.startPaymentJourney(utr, paymentAmountInPence).map {
                  case model: PaymentJourneyModel => Redirect(model.nextUrl)
                  case _: PaymentJourneyErrorResponse => throw new Exception("Failed to start payments journey due to downstream response")
                }
              case _ =>
                Logger("application").error("Failed to start payments journey due to missing UTR")
                Future.failed(new Exception("Failed to start payments journey due to missing UTR"))
            }
        }
  }
}
