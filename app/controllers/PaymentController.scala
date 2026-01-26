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
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.PayApiConnector
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentController @Inject()(val authActions: AuthActions,
                                  payApiConnector: PayApiConnector,
                                  val auditingService: AuditingService,
                                  val itvcErrorHandler: ItvcErrorHandler,
                                  val itvcAgentErrorHandler: AgentItvcErrorHandler
                                 )(implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def handleHandoff(paymentAmountInPence: Long, origin: Option[String] = None)
                   (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    auditingService.extendedAudit(
      InitiatePayNowAuditModel(mtdItUser.mtditid, mtdItUser.nino, mtdItUser.saUtr, mtdItUser.credId, mtdItUser.userType)
    )
    mtdItUser.saUtr match {
      case Some(utr) =>
        payApiConnector.startPaymentJourney(utr, paymentAmountInPence, mtdItUser.isAgent()).map {
          case model: PaymentJourneyModel => Redirect(model.nextUrl)
          case PaymentJourneyErrorResponse(status, message) => logAndHandleError(s"Failed to start payments journey due to downstream response, status: $status, message: $message")
          case _: PaymentJourneyResponse => logAndHandleError("Failed to start payments journey due to downstream response")
        }.recover{
          case ex => logAndHandleError(ex.getMessage)
        }
      case _ => Future.successful(
        logAndHandleError("Failed to start payments journey due to missing UTR")
      )
    }
  }

  def paymentHandoff(amountInPence: Long, origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleHandoff(amountInPence, origin = origin)
  }

  val agentPaymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => authActions.asMTDPrimaryAgent.async {
    implicit user =>
      handleHandoff(paymentAmountInPence)
  }

  def logAndHandleError(message: String)
                       (implicit mtdItUser: MtdItUser[_]): Result = {
    Logger("application").error(message)
    val errorHandler = if(mtdItUser.isAgent()) itvcAgentErrorHandler else itvcErrorHandler
    errorHandler.showInternalServerError()
  }

}
