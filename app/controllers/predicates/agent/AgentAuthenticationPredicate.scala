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
import controllers.predicates.agent.AgentJourneyState.SessionFunctions
import controllers.predicates.agent.AuthPredicate._
import play.api.mvc._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp}

import scala.concurrent.Future

object AgentAuthenticationPredicate  extends Results {

  lazy val timeoutRoute: Result = Redirect(controllers.timeout.routes.SessionTimeoutController.timeout())

  lazy val homeRoute: Result = Redirect(controllers.routes.HomeController.home())
  // Redirects to individual home for now until agent home logic is implemented.

  lazy val confirmationRoute: Result = Redirect(controllers.routes.HomeController.home())
  // Redirects to home for now until confirmation logic is implemented.

  val notSubmitted: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (user.arn.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(confirmationRoute))

  val hasSubmitted: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (user.arn.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("auth.AuthPredicates.hasSubmitted")))


  val timeoutPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  val signUpJourneyPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.isInState(AgentSignUp)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))


  val userMatchingJourneyPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.isInState(AgentUserMatching)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val userMatchedJourneyPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (request.session.isInState(AgentUserMatched)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val arnPredicate: AuthPredicate[IncomeTaxAgentUser] = request => user =>
    if (user.arn.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.arnPredicate")))


  val defaultPredicates: AuthPredicate[IncomeTaxAgentUser] = timeoutPredicate |+| arnPredicate

  val homePredicates: AuthPredicate[IncomeTaxAgentUser] = defaultPredicates |+| notSubmitted

  val userMatchingPredicates: AuthPredicate[IncomeTaxAgentUser] = homePredicates |+| userMatchingJourneyPredicate

  val subscriptionPredicates: AuthPredicate[IncomeTaxAgentUser] = homePredicates |+| signUpJourneyPredicate

  val confirmationPredicates: AuthPredicate[IncomeTaxAgentUser] = defaultPredicates |+| hasSubmitted

}




