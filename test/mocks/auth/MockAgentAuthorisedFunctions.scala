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

import authV2.AuthActionsTestData.delegatedEnrolmentPredicate
import forms.IncomeSourcesFormsSpec.AuthRetrievals
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentAuthorisedFunctions extends BeforeAndAfterEach {
  self: Suite with MockAuthServiceSupport =>

  lazy val isAgentPredicate: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
  lazy val isNotAgentPredicate: Predicate = AffinityGroup.Individual or AffinityGroup.Organisation
  lazy val authPredicateForAgent: Predicate = isAgentPredicate or isNotAgentPredicate


  def setupMockAgentAuthSuccess[X, Y](retrievalValue: X ~ Y): Unit =
    when(mockAuthService.authorised(authPredicateForAgent))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) =
            new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                body(retrievalValue.asInstanceOf[A])
            }
        }
      )

  def setupMockAgentWithoutARNAuthSuccess[X, Y](retrievalValue: X ~ Y): Unit =
    when(mockAuthService.authorised(EmptyPredicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) =
            new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                body(retrievalValue.asInstanceOf[A])
            }
        }
      )

  def setupMockAgentWithClientAuthSuccess[X, Y](
                                                 retrievalValue: X ~ Y,
                                                 mtdItId: String,
                                                 isSupportingAgent: Boolean = false
                                               ): Unit = {
    setupMockAgentAuthSuccess(retrievalValue)
    if (isSupportingAgent) {
      setupMockNoPrimaryDelegatedEnrolmentForMTDItId(mtdItId)
      setupMockSecondaryAgentAuthRetrievalSuccess(mtdItId)
    } else {
      setupMockPrimaryAgentAuthRetrievalSuccess(mtdItId)
    }
  }

  def setupMockAgentWithMissingDelegatedMTDEnrolment(
                                                      retrievalValue: AuthRetrievals,
                                                      mtdItId: String
                                                    ): Unit = {
    setupMockAgentAuthSuccess(retrievalValue)
    setupMockNoPrimaryDelegatedEnrolmentForMTDItId(mtdItId)
    setupMockNoSecondaryDelegatedEnrolmentForMTDItId(mtdItId)
  }

  def setupMockAgentAuthException(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised(authPredicateForAgent))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {

          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(exception)

          override def retrieve[A](retrieval: Retrieval[A]) =
            new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                Future.failed(exception)
            }
        }
      )

  def setupMockAgentWithClientAuthException(
                                             exception: AuthorisationException = new InvalidBearerToken,
                                             mtdItId: String,
                                             isSupportingAgent: Boolean = false
                                           ): Unit = {
    val predicate = delegatedEnrolmentPredicate(mtdItId, isSupportingAgent)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(exception)
        }
      )
  }

  def setupMockPrimaryAgentAuthRetrievalSuccess(mtdItId: String): Unit = {
    val predicate = delegatedEnrolmentPredicate(mtdItId, false)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            body
        }
      )
  }

  def setupMockNoPrimaryDelegatedEnrolmentForMTDItId(mtdItId: String): Unit = {
    val predicate = delegatedEnrolmentPredicate(mtdItId, false)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(InsufficientEnrolments())
        }
      )
  }

  def setupMockSecondaryAgentAuthRetrievalSuccess(mtdItId: String): Unit = {
    val predicate = delegatedEnrolmentPredicate(mtdItId, true)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            body
        }
      )
  }

  def setupMockNoSecondaryDelegatedEnrolmentForMTDItId(mtdItId: String): Unit = {
    val predicate = delegatedEnrolmentPredicate(mtdItId, true)
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(InsufficientEnrolments())
        }
      )
  }
}
