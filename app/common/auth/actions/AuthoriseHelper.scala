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

package common.auth.actions

import common.config.featureswitch.FeatureSwitching
import common.viewUtils.InternalUrlHelper
import common.controllers.errors.routes as errorRoutes
import common.models.auth.AuthorisedAndEnrolledRequest
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, LoginTimes, MdtpInformation, Name, ~}

import java.time.LocalDate
import scala.concurrent.Future

trait AuthoriseHelper extends FeatureSwitching {

  type AuthRetrievals = Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel
  type NrsIndividualAuthRetrievals = Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel ~
  Option[String] ~ Option[String] ~ Option[String] ~ Option[LocalDate] ~ Option[String] ~ Option[String] ~ Option[CredentialRole] ~
  Option[MdtpInformation] ~ Option[ItmpName] ~ Option[LocalDate] ~ Option[ItmpAddress] ~ Option[String] ~ LoginTimes
  type NrsAgentAuthRetrievals = Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel ~
    Option[String] ~ Option[String] ~ Option[String] ~ Option[String] ~ Option[LocalDate] ~ Option[String]~ AgentInformation ~
    Option[String] ~ Option[CredentialRole] ~ Option[MdtpInformation] ~ Option[ItmpName] ~ Option[LocalDate] ~
    Option[ItmpAddress] ~ Option[String] ~ LoginTimes

  val logger: Logger

  def logAndRedirect[A](): PartialFunction[Throwable, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _: BearerTokenExpired =>
      logger.warn("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(InternalUrlHelper.timeoutCall)))
    case insufficientEnrolments: InsufficientEnrolments =>
      logger.warn(s"Insufficient enrolments: ${insufficientEnrolments.msg}")
      Future.successful(Left(Redirect(errorRoutes.NotEnrolledController.show())))
    case authorisationException: AuthorisationException =>
      logger.warn(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(InternalUrlHelper.signinCall)))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }


  def redirectIfAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _ ~ _ ~ _ ~ Some(Agent) ~ _ =>
      logger.error(s"Agent on endpoint for individuals")
      Future.successful(Left(Redirect(hub.controllers.agent.routes.EnterClientsUTRController.show())))
  }

  def redirectIfNotAgent[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case _ ~ _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ _ =>
      logger.error(s"$ag on endpoint for agents")
      Future.successful(Left(Redirect(hub.controllers.routes.HomeController.show())))
  }
}
