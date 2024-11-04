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

package mocks.auth

import auth.FrontendAuthorisedFunctions
import controllers.agent.AuthUtils.{agentIdentifier, primaryAgentAuthRule, primaryAgentEnrolmentName, secondaryAgentAuthRule, secondaryAgentEnrolmentName}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import testConstants.BaseTestConstants._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentAuthorisedFunctions extends BeforeAndAfterEach {
  self: Suite =>

  val mockAuthService: FrontendAuthorisedFunctions
  lazy val isAgentPredicate: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
  lazy val isNotAgentPredicate: Predicate = AffinityGroup.Individual or AffinityGroup.Organisation
  lazy val authPredicateForAgent: Predicate = isAgentPredicate or isNotAgentPredicate


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
  }

  def setupMockAgentAuthSuccess[X, Y](retrievalValue: X ~ Y): Unit = {
    when(mockAuthService.authorised(authPredicateForAgent))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def setupMockAgentAuthException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    when(mockAuthService.authorised(authPredicateForAgent))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, executionContext: ExecutionContext) = Future.failed(exception)

          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[B] = Future.failed(exception)
          }
        }
      )
  }

  def setupMockPrimaryAgentAuthRetrievalSuccess[X, Y](retrievalValue: X ~ Y, mtdItId: String): Unit = {
    val predicate = Enrolment(primaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(primaryAgentAuthRule)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def setupMockPrimaryAgentAuthorisationException(mtdItId: String): Unit = {

    val predicate = Enrolment(primaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(primaryAgentAuthRule)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, executionContext: ExecutionContext) = Future.failed(InsufficientEnrolments())

          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[B] = Future.failed(InsufficientEnrolments())
          }
        })
  }

  def setupMockSecondaryAgentAuthRetrievalSuccess[X, Y](retrievalValue: X ~ Y, mtdItId: String): Unit = {
    val predicate = Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(secondaryAgentAuthRule)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def setupMockSecondaryAgentAuthorisationException(mtdItId: String): Unit = {

    val predicate = Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
      .withDelegatedAuthRule(secondaryAgentAuthRule)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, executionContext: ExecutionContext) = Future.failed(InsufficientEnrolments())

          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[B] = Future.failed(InsufficientEnrolments())
          }
        })
  }
}
