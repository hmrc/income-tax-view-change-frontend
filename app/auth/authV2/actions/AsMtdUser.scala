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

package auth.authV2.actions

import auth.MtdItUserOptionNino
import auth.authV2.AuthExceptions._
import auth.authV2.EnroledUser
import config.FrontendAppConfig
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import services.SessionDataService
import services.agent.ClientDetailsService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AsMtdUser @Inject()
(implicit val executionContext: ExecutionContext, sessionDataService: SessionDataService,
 clientDetailsService: ClientDetailsService, appConfig: FrontendAppConfig) extends ActionRefiner[EnroledUser, MtdItUserOptionNino] {

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  private def getClientMtdidAndUtr(implicit request: Request[_], hc: HeaderCarrier): Future[(Option[String], Option[String])] = {
    if (appConfig.isSessionDataStorageEnabled) {
      sessionDataService.getSessionData() map {
        case Left(_) => (request.session.get(SessionKeys.clientMTDID), request.session.get(SessionKeys.clientUTR))
        case Right(value) => (Some(value.mtditid), Some(value.utr))
      }
    }
    else {
      Future.successful((request.session.get(SessionKeys.clientMTDID), request.session.get(SessionKeys.clientUTR)))
    }
  }

  private def getClientNameFromCookies(implicit request: Request[_]): Option[Name] = {
    val firstName = request.session.get(SessionKeys.clientFirstName)
    val lastName = request.session.get(SessionKeys.clientLastName)

    if (firstName.isDefined && lastName.isDefined) {
      Some(Name(firstName, lastName))
    } else {
      None
    }
  }

  private def getClientName(clientUtr: Option[String])(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Name]] = {
    if (appConfig.isSessionDataStorageEnabled) {
      clientUtr match {
        case Some(utr) => clientDetailsService.checkClientDetails(utr) map {
          case Left(detailsError) =>
            Logger("error").error(s"unable to find client with UTR: $utr " + detailsError)
            getClientNameFromCookies
          case Right(value) => Some(Name(value.firstName, value.lastName))
        }
        case None => Future.successful(None)
      }
    } else {
      Future.successful(getClientNameFromCookies)
    }
  }

  override protected def refine[A](request: EnroledUser[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val r = request

    val optMtdIdAndUtr = request.affinityGroup match {
      case Some(Agent) => getClientMtdidAndUtr
      case _ => Future.successful((request.mtdId, None))
    }

    optMtdIdAndUtr flatMap { optTuple =>
      val (optMtdId, optUtr) = optTuple
      val optClientName: Future[Option[Name]] = request.affinityGroup match {
        case Some(Agent) => getClientName(optUtr)
        case _ => Future.successful(None)
      }
      optClientName map { optName =>
        optMtdId.map(id => MtdItUserOptionNino(
            mtditid = id,
            nino = request.nino,
            userName = request.userName,
            saUtr = request.saId,
            credId = request.credId,
            userType = request.affinityGroup,
            arn = request.arn,
            optClientName = optName))
          .map(Right(_))
          .getOrElse(
            request.affinityGroup match {
              case Some(Agent) => Left(noClientDetailsRoute)
              case _ => throw new MissingMtdId
            }
          )
      }
    }
  }
}
