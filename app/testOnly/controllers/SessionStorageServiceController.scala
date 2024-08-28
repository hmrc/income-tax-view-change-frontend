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
import models.core.ErrorModel
import play.api.Logger
import play.api.mvc._
import testOnly.models.SessionDataRetrieval
import testOnly.models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostResponse, SessionDataPostSuccess}
import testOnly.services.SessionDataService
import uk.gov.hmrc.http.HeaderCarrier
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
    post() flatMap {
      case Left(errorModel: SessionDataPostFailure) =>
        Logger("application").error(s"${if (isAgent) "Agent" else "Individual"} " +
          s"- POST user data to income-tax-session-data unsuccessful: - status: ${errorModel.status} - message: ${errorModel.errorMessage} - ")
        Future.successful(InternalServerError(s"There was an error. status: ${errorModel.status} - message: ${errorModel.errorMessage}"))
      case Right(successModel: SessionDataPostSuccess) =>
        Logger("application").debug(s"${if (isAgent) "Agent" else "Individual"} " +
          s"- POST user data to income-tax-session-data successful! status: ${successModel.status}")
        handlePostSuccess(isAgent)
    }
  }.recover {
    case ex: Throwable =>
      Logger("application").error(s"${if (isAgent) "Agent" else "Individual"}" +
        s" - Error on income-tax-session-data service test only page, status: - ${ex.getMessage} - ${ex.getCause} - ")
      handleError(isAgent)
  }

  private def handlePostSuccess(isAgent: Boolean)(implicit hc: HeaderCarrier): Future[Result] = {
    sessionDataService.getSessionData() map {
      case Left(errorModel: ErrorModel) =>
        Logger("application").error(s"${if (isAgent) "Agent" else "Individual"}" +
          s" - GET user data request to income-tax-session-data unsuccessful: - status: ${errorModel.code} - message: ${errorModel.message} - ")
        InternalServerError("Internal server error. There was an unexpected error fetching this data from income-tax-session-data service")
      case Right(model: SessionDataRetrieval) =>
        Ok(
          s"Session Data Service POST and GET requests were successful!\n" +
            s"User model:          ${model.toString}\n" +
            s"session id:        ${model.sessionId}\n" +
            s"internal id:       Not Implemented in FE Auth Predicate\n" +
            s"mtditid:           ${model.mtditid}\n" +
            s"nino:              ${model.nino}\n" +
            s"utr:               ${model.utr}\n"
        )
    }
  }

  def post()(implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_])
  : Future[SessionDataPostResponse] =
    sessionDataService.postSessionData()

  private def handleError(isAgent: Boolean)(implicit request: Request[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
  }

}
