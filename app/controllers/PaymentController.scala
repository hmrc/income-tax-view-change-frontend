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
import audit.models.InitiatePayNowAuditModel
import auth.FrontendAuthorisedFunctions
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import connectors.PayApiConnector
import controllers.agent.predicates.ClientConfirmedController
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.AuthenticatorPredicate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class PaymentJourneyException(msg: String) extends RuntimeException(msg)

@Singleton
class PaymentController @Inject()(payApiConnector: PayApiConnector,
                                  val auditingService: AuditingService,
                                  val authorisedFunctions: FrontendAuthorisedFunctions,
                                  val auth: AuthenticatorPredicate
                                 )(implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler) extends ClientConfirmedController {

  def handleHandoff(mtditid: String, nino: Option[String], saUtr: Option[String], credId: Option[String],
                    userType: Option[AffinityGroup], paymentAmountInPence: Long, isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit request: Request[_]): Future[Result] = {
    auditingService.extendedAudit(
      InitiatePayNowAuditModel(mtditid, nino, saUtr, credId, userType)
    )
    saUtr match {
      case Some(utr) =>
        payApiConnector.startPaymentJourney(utr, paymentAmountInPence, isAgent).map {
          case model: PaymentJourneyModel => Redirect(model.nextUrl)
          case PaymentJourneyErrorResponse(status, message) => throw new PaymentJourneyException(s"Failed to start payments journey due to downstream response, status: $status, message: $message")
          case _: PaymentJourneyResponse => throw new PaymentJourneyException("Failed to start payments journey due to downstream response")
        }
      case _ =>
        Logger("application").error("Failed to start payments journey due to missing UTR")
        Future.failed(new PaymentJourneyException("Failed to start payments journey due to missing UTR"))
    }
  }


  def paymentHandoff(amountInPence: Long, origin: Option[String] = None): Action[AnyContent] = auth.authenticatedActionOptionNino {
    implicit user =>
      handleHandoff(user.mtditid, user.nino, user.saUtr, user.credId, user.userType, amountInPence, isAgent = false, origin = origin)
  }

  val agentPaymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => auth.authenticatedActionWithNinoAgent {
    implicit response =>
        handleHandoff(getClientMtditid(response.request), Some(getClientNino(response.request)), getClientUtr(response.request), response.agent.credId, Some(Agent), paymentAmountInPence, isAgent = true)(response.request)
  }
}
