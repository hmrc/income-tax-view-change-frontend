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

import auth.authV2.AuthExceptions.NoAssignment
import auth.authV2.models.{AuthorisedAgentWithClientDetailsRequest, AuthorisedAndEnrolledRequest}
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.AuthUtils.*
import enums.MTDUserRole
import enums.MTDUserRole.{MTDPrimaryAgent, MTDSupportingAgent}
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAndRetrieveMtdAgent @Inject()(authorisedFunctions: AuthorisedFunctions,
                                             val appConfig: FrontendAppConfig,
                                             mcc: MessagesControllerComponents,
                                             errorHandler: AgentItvcErrorHandler)
  extends FeatureSwitching
    with ActionRefiner[AuthorisedAgentWithClientDetailsRequest, AuthorisedAndEnrolledRequest] {

  lazy val logger: Logger = Logger(getClass)

  implicit val executionContext: ExecutionContext = mcc.executionContext

  override protected def refine[A](request: AuthorisedAgentWithClientDetailsRequest[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val req: AuthorisedAgentWithClientDetailsRequest[A] = request

    val clientMtdItId = request.clientDetails.mtdItId
    lazy val primaryAgentDelegatedEnrolment =
      Enrolment(
        key = mtdEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, clientMtdItId)),
        state = "Activated",
        delegatedAuthRule = Some(primaryAgentAuthRule)
      )

    authorisedFunctions.authorised(primaryAgentDelegatedEnrolment) {
      constructAuthorisedAndEnrolledUser(clientMtdItId, MTDPrimaryAgent)
    }.recoverWith {
      case _ =>
        checkIfUserhasSupportingDelegatedEnrolmentPartialFunction(clientMtdItId)
    }

  }

  private def checkIfUserhasSupportingDelegatedEnrolmentPartialFunction[A](clientMtdItId: String)
                                                                          (implicit hc: HeaderCarrier, request: AuthorisedAgentWithClientDetailsRequest[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {
    lazy val supportingAgentDelegatedEnrolment =
      Enrolment(
        key = secondaryAgentEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, clientMtdItId)),
        state = "Activated",
        delegatedAuthRule = Some(secondaryAgentAuthRule)
      )
    authorisedFunctions.authorised(supportingAgentDelegatedEnrolment) {
      constructAuthorisedAndEnrolledUser(clientMtdItId, MTDSupportingAgent)
    }.recoverWith {
      case ex => handleAuthFailure(ex)
    }
  }

  private def constructAuthorisedAndEnrolledUser[A](clientMtdItId: String, mtdUserRole: MTDUserRole)(
    implicit request: AuthorisedAgentWithClientDetailsRequest[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {
    Future.successful(
      Right(
        AuthorisedAndEnrolledRequest(
          mtditId = clientMtdItId,
          mtdUserRole,
          authUserDetails = request.authUserDetails,
          clientDetails = Some(request.clientDetails)
        )
      )
    )
  }

  def handleAuthFailure[A](throwable: Throwable)(implicit request: Request[_]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {
    throwable match {
      case _: BearerTokenExpired =>
        logger.warn("Bearer Token Timed Out.")
        Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())))
      case _: InsufficientEnrolments =>
        logger.error(s"missing delegated enrolment. Redirect to agent error page.")
        Future.successful(Left(Redirect(controllers.agent.routes.ClientRelationshipFailureController.show())))
      case _: NoAssignment =>
        logger.error(s"Agent User is not in an access group associated with the Client.")
        Future.successful(Left(Redirect(controllers.agent.routes.NoAssignmentController.show())))
      case authorisationException: AuthorisationException =>
        logger.error(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
        Future.successful(Left(Redirect(controllers.routes.SignInController.signIn())))
      case ex =>
        logger.error(s"Unexpected error from Auth. Error message = ${ex.getMessage}")
        Future.successful(Left(errorHandler.showInternalServerError()))
    }
  }
}

