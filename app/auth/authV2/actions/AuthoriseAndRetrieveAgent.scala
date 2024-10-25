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

import audit.AuditingService
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import auth.authV2.AgentUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthoriseAndRetrieveAgent @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val appConfig: FrontendAppConfig,
                                          override val config: Configuration,
                                          override val env: Environment,
                                          mcc: MessagesControllerComponents)
  extends  AuthRedirects with ActionRefiner[Request, AgentUser] with FeatureSwitching {

  lazy val logger: Logger = Logger(getClass)

  implicit val executionContext: ExecutionContext = mcc.executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, AgentUser[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: Request[A] = request

    val isAgent: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    val isNotAgent: Predicate = AffinityGroup.Individual or AffinityGroup.Organisation

    authorisedFunctions.authorised( isAgent or isNotAgent)
      .retrieve(allEnrolments and credentials and affinityGroup and confidenceLevel) {
        redirectIfNotAgent() orElse redirectIfNotAgent
        constructAgentUser()
      }(hc, executionContext) recoverWith logAndRedirect
  }

  def logAndRedirect[A]: PartialFunction[Throwable, Future[Either[Result, AgentUser[A]]]] = {
    case _: BearerTokenExpired =>
      logger.debug("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)))
    case _: InsufficientEnrolments =>
      logger.debug(s"missing agent reference. Redirect to agent error page.")
      Future.successful(Left(Redirect(controllers.agent.errors.routes.AgentErrorController.show)))
    case authorisationException: AuthorisationException =>
      logger.debug(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn)))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }

  private type AuthAgentRetrievals =
    Enrolments ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel


  private def constructAgentUser[A]()(
    implicit request: Request[A]): PartialFunction[AuthAgentRetrievals, Future[Either[Result, AgentUser[A]]]] = {
    case enrolments ~ credentials ~ affinityGroup ~ confidenceLevel =>
      Future.successful(
        Right(AgentUser(
          enrolments = enrolments,
          affinityGroup = affinityGroup,
          confidenceLevel =confidenceLevel,
          credentials = credentials)))
  }

  private def redirectIfNotAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthAgentRetrievals, Future[Either[Result, AgentUser[A]]]] = {

    case _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ _ =>
      Logger(getClass).debug(s"$ag on endpoint for agents")
      Future.successful(Left(Redirect(controllers.agent.routes.EnterClientsUTRController.show)))
  }
}
