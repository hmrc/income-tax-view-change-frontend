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
import auth.authV2.Constants.mtdEnrolmentName
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockUserAuthorisedFunctions extends BeforeAndAfterEach {
  self: Suite with MockAuthServiceSupport =>

  lazy val isMTDUserPredicate: Predicate =
    Enrolment(mtdEnrolmentName) and (AffinityGroup.Organisation or AffinityGroup.Individual)

  lazy val predicate: Predicate = AffinityGroup.Agent or isMTDUserPredicate

  def setupMockUserAuthSuccess[X, Y](retrievalValue: X ~ Y): Unit =
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new authService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) =
            new authService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                body(retrievalValue.asInstanceOf[A])
            }
        }
      )

  def setupMockUserAuthException(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised(predicate))
      .thenReturn(
        new authService.AuthorisedFunction(EmptyPredicate) {

          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(exception)

          override def retrieve[A](retrieval: Retrieval[A]) =
            new authService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                Future.failed(exception)
            }
        }
      )

  def setupMockAuthorisedUserNoCheckAuthSuccess[X, Y](retrievalValue: X ~ Y): Unit =
    when(mockAuthService.authorised(EmptyPredicate))
      .thenReturn(
        new authService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) =
            new authService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                body(retrievalValue.asInstanceOf[A])
            }
        }
      )

  def setupMockUserAuthNoCheckException(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised(EmptyPredicate))
      .thenReturn(
        new authService.AuthorisedFunction(EmptyPredicate) {

          override def apply[A](body: => Future[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
            Future.failed(exception)

          override def retrieve[A](retrieval: Retrieval[A]) =
            new authService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
              override def apply[B](body: A => Future[B])
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
                Future.failed(exception)
            }
        }
      )
}
