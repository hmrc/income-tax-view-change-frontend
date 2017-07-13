/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.http.HeaderCarrier
import assets.TestConstants.testAuthSuccessResponse

import scala.concurrent.Future

trait MockFrontendAuthorisedFunctions extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthService = mock[FrontendAuthorisedFunctions]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    setupMockAuthRetrievalSuccess(testAuthSuccessResponse)
  }

  def setupMockAuthRetrievalSuccess[T](retrievalValue: T): Unit = {
    when(mockAuthService.authorised(Enrolment("HMRC-MTD-IT") and Enrolment("HMRC-NI")))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def setupMockAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised(Enrolment("HMRC-MTD-IT") and Enrolment("HMRC-NI")))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier) = Future.failed(exception)
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(exception)
          }
        })
}


//trait MockAuthorisedFunctions extends MockitoSugar with AuthorisedFunctions {
//  override val authConnector = mock[FrontendAuthConnector]
//}
//
//object MockAuthorisedUserWithEnrolment extends MockAuthorisedFunctions {
//  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
//    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = body
//    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
//      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = {
//        body(Enrolments(Set(
//          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated", ConfidenceLevel.L0),
//          Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated", ConfidenceLevel.L0)
//        )).asInstanceOf[A])
//      }
//    }
//  }
//  override def authorised(predicate: Predicate): AuthorisedFunction = this.authorised()
//}
//
//object MockAuthorisedUserNoEnrolment extends MockAuthorisedFunctions {
//  override def authorised(predicate: Predicate): AuthorisedFunction = new AuthorisedFunction(predicate) {
//    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = Future.failed(new InsufficientEnrolments)
//    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
//      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(new InsufficientEnrolments)
//    }
//  }
//  override def authorised(): AuthorisedFunction = this.authorised(EmptyPredicate)
//}
//
//object MockUnauthorisedUser extends MockAuthorisedFunctions {
//  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
//    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = Future.failed(new MissingBearerToken)
//    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
//      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(new MissingBearerToken)
//    }
//  }
//  override def authorised(predicate: Predicate): AuthorisedFunction = this.authorised()
//}
//
//object MockTimeoutUser extends MockAuthorisedFunctions {
//  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
//    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = Future.failed(new BearerTokenExpired)
//    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
//      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(new BearerTokenExpired)
//    }
//  }
//  override def authorised(predicate: Predicate): AuthorisedFunction = this.authorised()
//}