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
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AsMtdUser @Inject()
(implicit val executionContext: ExecutionContext) extends ActionRefiner[EnroledUser, MtdItUserOptionNino] {

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  override protected def refine[A](request: EnroledUser[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    implicit val r = request

    def getClientName: Option[Name] = {
      val firstName = request.session.get(SessionKeys.clientFirstName)
      val lastName = request.session.get(SessionKeys.clientLastName)

      if (firstName.isDefined && lastName.isDefined) {
        Some(Name(firstName, lastName))
      } else {
        None
      }
    }

    val (optMtdId, optClientName) = request.affinityGroup match {
      case Some(Agent) => (request.session.get(SessionKeys.clientMTDID), getClientName)
      case _ => (request.mtdId, None)
    }

    Future.successful(
      optMtdId.map(id => MtdItUserOptionNino(
          mtditid = id,
          nino = request.nino,
          userName = request.userName,
          saUtr = request.saId,
          credId = request.credId,
          userType = request.affinityGroup,
          arn = request.arn,
          optClientName = optClientName))
        .map(Right(_))
        .getOrElse(
          request.affinityGroup match {
            case Some(Agent) => Left(noClientDetailsRoute)
            case _ => throw new MissingMtdId
          }
        ))
  }
}
