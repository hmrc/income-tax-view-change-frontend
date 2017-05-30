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

package auth

import config.FrontendAuthConnector
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait MockAuthorisedFunctions extends MockitoSugar with AuthorisedFunctions {
  override val authConnector = mock[FrontendAuthConnector]
}

object MockAuthorisedUser extends MockAuthorisedFunctions {
  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = body
    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = {
        body(Enrolments(Set(Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "XAITSA000123456")), "activated", ConfidenceLevel.L0))).asInstanceOf[A])
      }
    }
  }
}

object MockUnauthorisedUser extends MockAuthorisedFunctions {
  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = Future.failed(new MissingBearerToken)
    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(new MissingBearerToken)
    }
  }
}

object MockTimeoutUser extends MockAuthorisedFunctions {
  override def authorised(): AuthorisedFunction = new AuthorisedFunction(EmptyPredicate) {
    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier): Future[A] = Future.failed(new BearerTokenExpired)
    override def retrieve[A](retrieval: Retrieval[A]): AuthorisedFunctionWithResult[A] = new AuthorisedFunctionWithResult(EmptyPredicate, retrieval) {
      override def apply[B](body: (A) => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(new BearerTokenExpired)
    }
  }
}