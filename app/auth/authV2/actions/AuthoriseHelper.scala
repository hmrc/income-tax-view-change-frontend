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

import auth.authV2.models.AuthorisedAndEnrolledRequest
import config.featureswitch.FeatureSwitching
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}

import scala.concurrent.Future

trait AuthoriseHelper extends FeatureSwitching {

  type AuthRetrievals = Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel

  val logger: Logger

  def logAndRedirect[A](): PartialFunction[Throwable, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _: BearerTokenExpired =>
      logger.warn("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())))
    case insufficientEnrolments: InsufficientEnrolments =>
      logger.error(s"Insufficient enrolments: ${insufficientEnrolments.msg}")
      Future.successful(Left(Redirect(controllers.errors.routes.NotEnrolledController.show())))
    case authorisationException: AuthorisationException =>
      logger.error(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn())))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }


  def redirectIfAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _ ~ _ ~ _ ~ Some(Agent) ~ _ =>
      logger.error(s"Agent on endpoint for individuals")
      Future.successful(Left(Redirect(controllers.agent.routes.EnterClientsUTRController.show())))
  }

  def redirectIfNotAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _ ~ _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ _ =>
      logger.error(s"$ag on endpoint for agents")
      Future.successful(Left(Redirect(controllers.routes.HomeController.show())))
  }
}
