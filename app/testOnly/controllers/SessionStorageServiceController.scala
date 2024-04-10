/*
 * Copyright 2024 HM Revenue & Customs
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

package testOnly.controllers

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import play.api.Logger
import play.api.mvc._
import testOnly.models.SessionDataModel
import testOnly.services.SessionDataService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.AuthenticatorPredicate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionStorageServiceController @Inject()(implicit val ec: ExecutionContext,
                                                implicit override val mcc: MessagesControllerComponents,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val auth: AuthenticatorPredicate,
                                                val sessionDataService: SessionDataService
                                               ) extends ClientConfirmedController {

  def show(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShow(isAgent = false)
  }

  def showAgent: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleShow(isAgent = true)
  }

  private def handleShow(isAgent: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_]): Future[Result] = {
    hc.sessionId match {
      case Some(sessionId: SessionId) => post(isAgent = isAgent, sessionId) flatMap {
        case Left(ex) =>
          Logger("application").error(s"[SessionStorageServiceController][handleShow] " +
            s"${if (isAgent) "Agent" else "Individual"} - POST user data to income-tax-session-data unsuccessful: - ${ex.getMessage} - ${ex.getCause} - ")
          Future.successful(handleError(isAgent))
        case Right(id: String) =>
          handlePostSuccess(id, isAgent)
      }
      case None =>
        Logger("application").error(s"[SessionStorageServiceController][handleShow] ${if (isAgent) "Agent" else "Individual"}" +
          s" - HeaderCarrier contained no sessionId!")
        Future.successful(handleError(isAgent))
    }
  }.recover {
    case ex: Throwable =>
      Logger("application").error(s"[SessionStorageServiceController][handleShow] " +
        s"${if (isAgent) "Agent" else "Individual"} - Error on income-tax-session-data service test only page, status: - ${ex.getMessage} - ${ex.getCause} - ")
      handleError(isAgent)
  }

  private def handlePostSuccess(id: String, isAgent: Boolean)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    sessionDataService.getSessionData(id) map {
      case Left(ex) =>
        Logger("application").error(s"[SessionStorageServiceController][handleShow] ${if (isAgent) "Agent" else "Individual"}" +
          s" - GET user data request to income-tax-session-data unsuccessful: - ${ex.getMessage} - ${ex.getCause} - ")
        InternalServerError("Internal server error. There was an unexpected error fetching this data from income-tax-session-data service")
      case Right(model: SessionDataModel) =>
        Ok(
          s"User model:          ${model.toString}\n" +
            s"session id:          ${model.sessionID}\n" +
            s"mtditid:             ${model.mtditid}\n" +
            s"nino:                ${model.nino}\n" +
            s"saUtr:               ${model.saUtr}\n" +
            s"client first name:   ${model.clientFirstName}\n" +
            s"client last name:    ${model.clientLastName}\n" +
            s"user type:           ${model.userType}\n")
    }
  }

  def post(isAgent: Boolean, sessionId: SessionId)(implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_])
  : Future[Either[Throwable, String]] =
    sessionDataService.postSessionData(isAgent, sessionId)

  private def handleError(isAgent: Boolean)(implicit request: Request[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
  }

}
