/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.agent.routes
import controllers.agent.utils.SessionKeys
import controllers.agent.routes
import controllers.predicates.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import controllers.predicates.IncomeTaxAgentUser
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp}

import scala.concurrent.Future

object AgentAuthenticationPredicate extends Results {

  lazy val timeoutRoute: Result = Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())

  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show())

  val timeoutPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  val arnPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (user.arn.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(MissingAgentReferenceNumber()))

  // Redirects to Select Client Page if client details aren't in session
  val detailsPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.get(SessionKeys.clientFirstName).nonEmpty
      && request.session.get(SessionKeys.clientLastName).nonEmpty
      && request.session.get(SessionKeys.clientUTR).nonEmpty) {
      Right(AuthPredicateSuccess)
    } else Left(Future.successful(noClientDetailsRoute))


  val defaultPredicates: AuthPredicate[IncomeTaxAgentUser] = timeoutPredicate |+| arnPredicate

  val clientDetailsPredicates: AuthPredicate[IncomeTaxAgentUser] = defaultPredicates |+| detailsPredicate

  case class MissingAgentReferenceNumber(msg: String = "Agent Reference Number was not found in user's enrolments") extends AuthorisationException(msg)

}
