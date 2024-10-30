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

import auth.MtdItUserOptionNino
import config.featureswitch.FeatureSwitching
import controllers.agent.AuthUtils.{agentEnrolmentName, primaryAgentEnrolmentName, secondaryAgentEnrolmentName}
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, BearerTokenExpired, ConfidenceLevel, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.Future

trait AuthoriseHelper extends AuthRedirects with FeatureSwitching {

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel

  val logger: Logger

  def logAndRedirect[A](requireAgent: Boolean): PartialFunction[Throwable, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case _: BearerTokenExpired =>
      logger.debug("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)))
    case InsufficientEnrolments(msg) if msg.contains("HMRC-MTD-IT") && requireAgent =>
      logger.debug(s"missing delegated enrolment. Redirect to agent error page.")
      Future.successful(Left(Redirect(controllers.agent.routes.EnterClientsUTRController.show)))
    case _: InsufficientEnrolments if requireAgent =>
      logger.debug(s"missing agent reference. Redirect to agent error page.")
      Future.successful(Left(Redirect(controllers.agent.errors.routes.AgentErrorController.show)))
    case insufficientEnrolments: InsufficientEnrolments =>
      logger.debug(s"Insufficient enrolments: ${insufficientEnrolments.msg}")
      Future.successful(Left(Redirect(controllers.errors.routes.NotEnrolledController.show)))
    case authorisationException: AuthorisationException =>
      logger.debug(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn)))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }


  def redirectIfAgent[A]()(
    implicit request: Request[A],
    hc: HeaderCarrier): PartialFunction[AuthRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case _ ~ _ ~ _ ~ Some(Agent) ~ _ =>
      logger.debug(s"Agent on endpoint for individuals")
      Future.successful(Left(Redirect(controllers.agent.routes.EnterClientsUTRController.show)))
  }

  def redirectIfNotAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case _ ~ _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ _ =>
      logger.debug(s"$ag on endpoint for agents")
      Future.successful(Left(Redirect(controllers.routes.HomeController.show())))
  }
}
