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

package controllers.predicates

import auth.MockAuthenticationPredicate
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport
import assets.TestConstants._

import scala.concurrent.Future

class AuthenticationPredicateSpec extends TestSupport with MockitoSugar with MockAuthenticationPredicate {

  "The authentication async method" when {

    def setupResult(authenticationPredicate: AuthenticationPredicate): Action[AnyContent] = authenticationPredicate.async {
      implicit request => implicit user =>
        Future.successful(Ok(user.mtditid + " " + user.nino))
    }

    "called with an authenticated user" when {

      "a HMRC-MTD-IT enrolment exists with a NINO" should {

        lazy val result = setupResult(MockAuthenticated)(fakeRequestWithActiveSession)

        "return Ok (200)" in {
          status(result) shouldBe Status.OK
        }

        "should have a body with the expected user details" in {
          bodyOf(await(result)) shouldBe testMtditid + " " + testNino
        }
      }

      "a HMRC-MTD-IT enrolment does NOT exist" should {

        lazy val result = setupResult(MockAuthenticatedNoEnrolment)(fakeRequestWithActiveSession)

        "return Internal Server Error (500)" in {
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "called with an unauthenticated user (Bearer Token Expired response from Auth)" should {

      lazy val result = setupResult(MockUnauthorised)(fakeRequestWithActiveSession)

      "should be a rerdirect (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "called with an Authenticated User with a Timed Out session (Session Expired)" should {

      lazy val result = setupResult(MockAuthenticated)(fakeRequestWithTimeoutSession)

      "should be a rerdirect (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "called with a user with no session" should {

      lazy val result = setupResult(MockUnauthorised)(fakeRequestNoSession)

      "should be a rerdirect (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
  }
}
