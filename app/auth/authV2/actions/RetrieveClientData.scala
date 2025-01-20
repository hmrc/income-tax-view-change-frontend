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

import com.google.inject.Singleton
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import models.sessionData.SessionDataGetResponse.SessionDataNotFound
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import play.api.{Configuration, Environment, Logger}
import services.SessionDataService
import services.agent.ClientDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveClientData @Inject()(sessionDataService: SessionDataService,
                                   clientDetailsService: ClientDetailsService,
                                   errorHandler: AgentItvcErrorHandler,
                                   override val env: Environment,
                                   override val config: Configuration,
                                   mcc: MessagesControllerComponents,
                                   appConfig: FrontendAppConfig)
                                  (implicit val executionContext: ExecutionContext) extends AuthRedirects {

  lazy val logger: Logger = Logger(getClass)

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  def authorise(useCookies: Boolean = false): ActionRefiner[Request, ClientDataRequest] = new ActionRefiner[Request, ClientDataRequest] {

    implicit val executionContext: ExecutionContext = mcc.executionContext

    override protected def refine[A](request: Request[A]): Future[Either[Result, ClientDataRequest[A]]] = {

      implicit val r: Request[A] = request
      implicit val hc: HeaderCarrier = HeaderCarrierConverter
        .fromRequestAndSession(request, request.session)

      val useSessionDataService = appConfig.isSessionDataStorageEnabled

      val useCookiesFlg: Boolean = !useSessionDataService
      sessionDataService.getSessionData(useCookie = useCookiesFlg ).flatMap {
        case Right(sessionData) =>
          clientDetailsService.checkClientDetails(sessionData.utr).map {
            case Right(name) => Right(ClientDataRequest(
              sessionData.mtditid,
              name.firstName,
              name.lastName,
              sessionData.nino,
              sessionData.utr,
              getBooleanFromSession(SessionKeys.isSupportingAgent),
              confirmed = {
                if (appConfig.isSessionDataStorageEnabled) true
                else getBooleanFromSession(SessionKeys.confirmedClient)
              }
            ))
            case Left(error) =>
              Logger("error").error(s"unable to find client with UTR: ${sessionData.utr} - ${useCookiesFlg}" + error)
              Left(Redirect(routes.EnterClientsUTRController.show))
          }
        case Left(_: SessionDataNotFound) => Future.successful(Left(Redirect(routes.EnterClientsUTRController.show)))
        case Left(_) => Future.successful(Left(errorHandler.showInternalServerError()))
      }
    }

    private def getBooleanFromSession(key: String)(implicit r: Request[_]): Boolean = {
      r.session.get(key).fold(false)(_.toBoolean)
    }
  }
}
