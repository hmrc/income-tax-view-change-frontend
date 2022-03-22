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
import audit.models.InitiatePayNowAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import connectors.PayApiConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import play.api.Logger
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                  val authenticate: AuthenticationPredicate,
                                  payApiConnector: PayApiConnector,
                                  val auditingService: AuditingService,
                                  val authorisedFunctions: FrontendAuthorisedFunctions
                                 )(implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler) extends ClientConfirmedController {

  val action: ActionBuilder[MtdItUserOptionNino, AnyContent] = checkSessionTimeout andThen authenticate

  def handleHandoff(mtditid: String, nino: Option[String], saUtr: Option[String], credId: Option[String],
                    userType: Option[String], paymentAmountInPence: Long, isAgent: Boolean)
                   (implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    auditingService.extendedAudit(
      InitiatePayNowAuditModel(mtditid, nino, saUtr, credId, userType)
    )
    saUtr match {
      case Some(utr) =>
        payApiConnector.startPaymentJourney(utr, paymentAmountInPence, isAgent).map {
          case model: PaymentJourneyModel => Redirect(model.nextUrl)
          case _: PaymentJourneyErrorResponse => throw new Exception("Failed to start payments journey due to downstream response")
        }
      case _ =>
        Logger("application").error("Failed to start payments journey due to missing UTR")
        Future.failed(new Exception("Failed to start payments journey due to missing UTR"))
    }
  }

  val paymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => action.async {
    implicit user =>
      handleHandoff(user.mtditid, user.nino, user.saUtr, user.credId, user.userType, paymentAmountInPence, isAgent = false)
  }

  val agentPaymentHandOff: Long => Action[AnyContent] = paymentAmountInPence => Authenticated.async {
    implicit request =>
      implicit user =>
        handleHandoff(getClientMtditid, Some(getClientNino), getClientUtr, user.credId, Some("Agent"), paymentAmountInPence, isAgent = true)
  }
}
