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

import auth.FrontendAuthorisedFunctions
import auth.authV2.models.{AuthUserDetails, AuthorisedUserRequest}
import com.google.inject.Singleton
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAndRetrieveAgentForNrs @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents)
  extends FeatureSwitching with AuthoriseHelper {

  lazy val logger: Logger = Logger(getClass)

  def authorise(arnRequired: Boolean = true): ActionRefiner[Request, AuthorisedUserRequest] = new ActionRefiner[Request, AuthorisedUserRequest] {

    implicit val executionContext: ExecutionContext = mcc.executionContext

    override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedUserRequest[A]]] = {

      implicit val hc: HeaderCarrier = HeaderCarrierConverter
        .fromRequestAndSession(request, request.session)

      implicit val req: Request[A] = request

      val isAgent: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
      val isNotAgent: Predicate = AffinityGroup.Individual or AffinityGroup.Organisation

      val predicate = if(arnRequired) {
        isAgent or isNotAgent
      } else EmptyPredicate

      authorisedFunctions.authorised(predicate)
        .retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel and internalId and externalId
          and agentCode and nino and dateOfBirth and email and agentInformation and groupIdentifier and credentialRole
          and mdtpInformation and itmpName and itmpDateOfBirth and itmpAddress and credentialStrength and loginTimes) {
          redirectIfNonAgent() orElse constructAgentUser()
        }(hc, executionContext) recoverWith agentLogAndRedirect
    }
  }

  def agentLogAndRedirect[A]: PartialFunction[Throwable, Future[Either[Result, AuthorisedUserRequest[A]]]] = {
    case _: BearerTokenExpired =>
      logger.warn("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())))
    case _: InsufficientEnrolments =>
      logger.error(s"missing agent reference. Redirect to agent error page.")
      Future.successful(Left(Redirect(controllers.agent.errors.routes.AgentErrorController.show())))
    case authorisationException: AuthorisationException =>
      logger.error(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn())))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }

  private def constructAgentUser[A]()(
    implicit request: Request[A]): PartialFunction[NrsAgentAuthRetrievals, Future[Either[Result, AuthorisedUserRequest[A]]]] = {
    case enrolments ~ userName ~ credentials ~ affinityGroup ~ confidenceLevel ~ internalId ~ externalId ~ agentCode ~ nino ~
      dateOfBirth ~ email ~ agentInformation ~ groupIdentifier ~ credentialRole ~ mdtpInformation ~ itmpName ~ itmpDateOfBirth ~ itmpAddress ~
      credentialStrength ~ loginTimes =>
      val authUserDetails = AuthUserDetails(
        enrolments = enrolments,
        affinityGroup = affinityGroup,
        credentials = credentials,
        name = userName,
        confidenceLevel = Some(confidenceLevel),
        internalId = internalId,
        externalId = externalId,
        agentCode = agentCode,
        nino = nino,
        dateOfBirth = dateOfBirth,
        email = email,
        agentInformation = Some(agentInformation),
        groupIdentifier = groupIdentifier,
        credentialRole = credentialRole,
        mdtpInformation = mdtpInformation,
        itmpName = itmpName,
        itmpDateOfBirth = itmpDateOfBirth,
        itmpAddress = itmpAddress,
        credentialStrength = credentialStrength,
        loginTimes = Some(loginTimes)
      )
      Future.successful(
        Right(AuthorisedUserRequest(authUserDetails))
      )
  }

  private def redirectIfNonAgent[A]()(
    implicit request: Request[A]): PartialFunction[NrsAgentAuthRetrievals, Future[Either[Result, AuthorisedUserRequest[A]]]] = {
    case _ ~ _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ _ ~ _ ~ _ ~ _ ~ _ ~
      _ ~ _ ~ _ ~ _ ~ _ ~ _ ~ _ ~ _ ~ _ ~ _ ~ _ =>
      logger.error(s"$ag on endpoint for agents")
      Future.successful(Left(Redirect(controllers.routes.HomeController.show())))
  }
}
