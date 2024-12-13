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
import config.FrontendAppConfig
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.SessionDataService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentHasClientDetails @Inject()(implicit val executionContext: ExecutionContext,
                                      sessionDataService: SessionDataService, appConfig: FrontendAppConfig)
  extends ActionRefiner[MtdItUserOptionNino, MtdItUserOptionNino] {

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  override protected def refine[A](request: MtdItUserOptionNino[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    val hasConfirmedClient: Boolean = appConfig.isSessionDataStorageEnabled || request.session.get(SessionKeys.confirmedClient).nonEmpty

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    lazy val cookiesHaveClientDetails: Boolean = {
      request.session.get(SessionKeys.clientMTDID).nonEmpty &&
        request.session.get(SessionKeys.clientFirstName).nonEmpty &&
        request.session.get(SessionKeys.clientLastName).nonEmpty &&
        request.session.get(SessionKeys.clientUTR).nonEmpty
    }

    val hasClientDetails: Future[Boolean] = {
      if (appConfig.isSessionDataStorageEnabled){
        sessionDataService.getSessionData()(request, hc) map {
          case Right(_) => true
          case Left(ex) =>
            Logger("application").warn(s"Failed to get client details from mongo: $ex")
            cookiesHaveClientDetails
        }
      }
      else{
       Future.successful(cookiesHaveClientDetails)
      }
    }

    // This check might not be necessary now we authorise on the Agent enrolment?
    val hasArn: Boolean = request.arn.nonEmpty

    hasClientDetails flatMap{ hasDetails =>
      if (!request.userType.contains(Agent)) {
        Future.successful(Right(request))
      } else if (hasArn && hasConfirmedClient && hasDetails) {
        Future.successful(Right(request))
      } else if (!hasArn) {
        throw new MissingAgentReferenceNumber
      } else {
        Future.successful(Left(noClientDetailsRoute))
      }
    }

  }
}
