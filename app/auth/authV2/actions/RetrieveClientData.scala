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

import auth.authV2.models.{AgentClientDetails, AuthorisedAgentWithClientDetailsRequest, AuthorisedUserRequest}
import com.google.inject.Singleton
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import models.sessionData.SessionDataGetResponse.SessionDataNotFound
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import services.SessionDataService
import services.agent.ClientDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveClientData @Inject()(sessionDataService: SessionDataService,
                                   clientDetailsService: ClientDetailsService,
                                   errorHandler: AgentItvcErrorHandler,
                                   mcc: MessagesControllerComponents,
                                   appConfig: FrontendAppConfig)
                                  (implicit val executionContext: ExecutionContext) {

  lazy val logger: Logger = Logger(getClass)

  def authorise(useCookies: Boolean = false): ActionRefiner[AuthorisedUserRequest, AuthorisedAgentWithClientDetailsRequest] = new ActionRefiner[AuthorisedUserRequest, AuthorisedAgentWithClientDetailsRequest] {

    implicit val executionContext: ExecutionContext = mcc.executionContext

    override protected def refine[A](request: AuthorisedUserRequest[A]): Future[Either[Result, AuthorisedAgentWithClientDetailsRequest[A]]] = {

      implicit val r: Request[A] = request
      implicit val hc: HeaderCarrier = HeaderCarrierConverter
        .fromRequestAndSession(request, request.session)

      val useSessionDataService = appConfig.isSessionDataStorageEnabled

      sessionDataService.getSessionData(useCookie = useCookies || !useSessionDataService).flatMap {
        case Right(sessionData) =>
          clientDetailsService.checkClientDetails(sessionData.utr).map {
            case Right(details) =>
              val agentClientDetails = AgentClientDetails(
                sessionData.mtditid,
                details.firstName,
                details.lastName,
                sessionData.nino,
                sessionData.utr,
                confirmed = {
                  if (appConfig.isSessionDataStorageEnabled) true
                  else getBooleanFromSession(SessionKeys.confirmedClient)
                }
              )
              Right(AuthorisedAgentWithClientDetailsRequest(
              request.authUserDetails,
                agentClientDetails
            ))
            case Left(error) =>
              Logger("error").error(s"unable to find client with UTR: ${sessionData.utr} " + error)
              Left(Redirect(routes.EnterClientsUTRController.show()))
          }
        case Left(_: SessionDataNotFound) => Future.successful(Left(Redirect(routes.EnterClientsUTRController.show())))
        case Left(_) => Future.successful(Left(errorHandler.showInternalServerError()))
      }
    }

    private def getBooleanFromSession(key: String)(implicit r: Request[_]): Boolean = {
      r.session.get(key).fold(false)(_.toBoolean)
    }
  }
}
