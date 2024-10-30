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

package auth.authV2.actions

import config.AgentItvcErrorHandler
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import services.SessionDataService
import testOnly.models.SessionDataGetResponse.SessionDataNotFound
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveClientData @Inject()(sessionDataService: SessionDataService,
                                   errorHandler: AgentItvcErrorHandler)(implicit val executionContext: ExecutionContext) extends ActionRefiner[Request, ClientDataRequest] {

  lazy val logger: Logger = Logger(getClass)

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  override protected def refine[A](request: Request[A]): Future[Either[Result, ClientDataRequest[A]]] = {

    implicit val r: Request[A] = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    sessionDataService.getSessionData(useCookie = true).map {
      case Right(sessionData) => Right(ClientDataRequest(sessionData.mtditid,None, None, sessionData.nino, sessionData.utr,
        getBooleanFromSession(SessionKeys.isSupportingAgent), getBooleanFromSession(SessionKeys.confirmedClient)))
      case Left(_: SessionDataNotFound) => Left(Redirect(routes.EnterClientsUTRController.show))
      case Left(_) => Left(errorHandler.showInternalServerError())
    }
  }

  private def getBooleanFromSession(key: String)(implicit r: Request[_]): Boolean = {
    r.session.get(key).fold(false)(_.toBoolean)
  }


}
