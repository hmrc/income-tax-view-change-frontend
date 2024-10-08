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

import auth.authV2.AuthExceptions._
import auth.authV2.EnroledUser
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentHasClientDetails @Inject()(implicit val executionContext: ExecutionContext) extends ActionRefiner[EnroledUser, EnroledUser] {

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  override protected def refine[A](request: EnroledUser[A]): Future[Either[Result, EnroledUser[A]]] = {

    val hasConfirmedClient: Boolean = request.session.get(SessionKeys.confirmedClient).nonEmpty

    val hasClientDetails: Boolean = {
      request.session.get(SessionKeys.clientMTDID).nonEmpty &&
      request.session.get(SessionKeys.clientFirstName).nonEmpty &&
      request.session.get(SessionKeys.clientLastName).nonEmpty &&
      request.session.get(SessionKeys.clientUTR).nonEmpty
    }

    // This check might not be necessary now we authorise on the Agent enrolment?
    val hasArn: Boolean = request.arn.nonEmpty

    if (!request.affinityGroup.contains(Agent)) {
      Future.successful(Right(request))
    } else if (hasArn && hasConfirmedClient && hasClientDetails) {
      Future.successful(Right(request))
    } else if (!hasArn) {
        throw new MissingAgentReferenceNumber
    } else {
      Future.successful(Left(noClientDetailsRoute))
    }
  }
}
