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

import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import models.sessionData.SessionDataGetResponse.SessionDataGetSuccess
import play.api.Logger
import play.api.mvc._
import services.SessionDataService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionStorageServiceController @Inject()(val authActions: AuthActions,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val sessionDataService: SessionDataService
                                               )(implicit val ec: ExecutionContext,
                                                 val mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  def show(): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleShow(isAgent = false)
  }

  def showAgent: Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async  {
    implicit mtdItUser =>
      handleShow(isAgent = true)
  }

  private def handleShow(isAgent: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    sessionDataService.getSessionData() map {
      case Left(ex: Throwable) =>
        Logger("application").error(s"${if (isAgent) "Agent" else "Individual"}" +
          s" - GET user data request to income-tax-session-data unsuccessful: - message: ${ex.getMessage} - cause: ${ex.getCause} - ")
        InternalServerError("Internal server error. There was an unexpected error fetching this data from income-tax-session-data service")
      case Right(model: SessionDataGetSuccess) =>
        Ok(
            s"Session Data Service GET request was successful!\n" +
            s"User model:        ${model.toString}\n" +
            s"session id:        ${model.sessionId}\n" +
            s"internal id:       Not Implemented in FE Auth Predicate\n" +
            s"mtditid:           ${model.mtditid}\n" +
            s"nino:              ${model.nino}\n" +
            s"utr:               ${model.utr}\n"
        )
    }
  }
}
