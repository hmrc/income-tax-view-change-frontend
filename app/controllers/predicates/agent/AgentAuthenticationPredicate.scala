/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.predicates.agent

import cats.implicits._
import controllers.agent.utils.SessionKeys
import controllers.agent.routes
import controllers.predicates.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import controllers.predicates.IncomeTaxAgentUser
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp}

import scala.concurrent.Future

object AgentAuthenticationPredicate extends Results {

  lazy val timeoutRoute: Result = Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show())

  lazy val missingARNFailure: Future[Result] = Future.failed(MissingAgentReferenceNumber())

  val timeoutPredicate: AuthPredicate[IncomeTaxAgentUser] = request => _ =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  def arnPredicate(onMissingARN: Future[Result] = missingARNFailure): AuthPredicate[IncomeTaxAgentUser] = _ => user =>
    if (user.agentReferenceNumber.nonEmpty) Right(AuthPredicateSuccess)
    else Left(onMissingARN)

  // Redirects to Select Client Page if client details aren't in session
  val detailsPredicate: AuthPredicate[IncomeTaxAgentUser] = request => _ =>
    if (request.session.get(SessionKeys.clientFirstName).nonEmpty
      && request.session.get(SessionKeys.clientLastName).nonEmpty
      && request.session.get(SessionKeys.clientUTR).nonEmpty) {
      Right(AuthPredicateSuccess)
    } else Left(Future.successful(noClientDetailsRoute))

  // Redirects to Select Client Page if the agent hasn't confirmed the client
  val selectedClientPredicate: AuthPredicate[IncomeTaxAgentUser] = request => _ =>
    if (request.session.get(SessionKeys.confirmedClient).nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(noClientDetailsRoute))

  def defaultAgentPredicates(onMissingARN: Future[Result] = missingARNFailure): AuthPredicate[IncomeTaxAgentUser] =
    timeoutPredicate |+| arnPredicate(onMissingARN)

  val defaultPredicates: AuthPredicate[IncomeTaxAgentUser] = defaultAgentPredicates()

  val clientDetailsPredicates: AuthPredicate[IncomeTaxAgentUser] = defaultPredicates |+| detailsPredicate

  val confirmedClientPredicates: AuthPredicate[IncomeTaxAgentUser] = clientDetailsPredicates |+| selectedClientPredicate

  case class MissingAgentReferenceNumber(msg: String = "Agent Reference Number was not found in user's enrolments") extends AuthorisationException(msg)

}
