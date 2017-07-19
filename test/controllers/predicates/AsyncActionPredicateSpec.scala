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

import assets.TestConstants._
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import utils.TestSupport
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

class AsyncActionPredicateSpec extends TestSupport with MockitoSugar with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  "The authentication async method" when {

    def setupResult(): Action[AnyContent] =
      new AsyncActionPredicate()(
        fakeApplication.injector.instanceOf[MessagesApi],
        fakeApplication.injector.instanceOf[SessionTimeoutPredicate],
        MockAuthenticationPredicate,
        MockIncomeSourceDetailsPredicate
      ).async {
        implicit request => implicit user => implicit sources =>
          Future.successful(Ok(user.mtditid + " " + user.nino))
      }

    "called with an authenticated user" when {

      def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

      "a HMRC-MTD-IT enrolment exists with a NINO" should {

        "return Ok (200)" in {
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.businessIncomeSourceSuccess)
          status(result) shouldBe Status.OK
        }

        "should have a body with the expected user details" in {
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.businessIncomeSourceSuccess)
          bodyOf(await(result)) shouldBe testMtditid + " " + testNino
        }
      }

      "a HMRC-MTD-IT enrolment does NOT exist" should {

        "have a redirect (303 - SEE_OTHER) status" in {
          setupMockAuthorisationException(new InsufficientEnrolments)
          status(result) shouldBe Status.SEE_OTHER

        }

        "redirect to the Not Enrolled page" in {
          setupMockAuthorisationException(new InsufficientEnrolments)
          redirectLocation(result) shouldBe Some(controllers.notEnrolled.routes.NotEnrolledController.show().url)
        }
      }
    }

    "called with a Bearer Token Expired response from Auth" should {

      lazy val result = setupResult()(fakeRequestWithActiveSession)

      "should be a rerdirect (303)" in {
        setupMockAuthorisationException(new BearerTokenExpired)
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        setupMockAuthorisationException(new BearerTokenExpired)
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "called with an Authenticated User with a Timed Out session (Session Expired)" should {

      lazy val result = setupResult()(fakeRequestWithTimeoutSession)

      "should be a rerdirect (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "called with a user with no session" should {

      lazy val result = setupResult()(fakeRequestNoSession)

      "should be a rerdirect (303)" in {
        setupMockAuthorisationException()
        status(result) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "When an exception which is not an authorisation exception" should {

      def setupFailedFutureResult(): Action[AnyContent] =
        new AsyncActionPredicate()(
          fakeApplication.injector.instanceOf[MessagesApi],
          fakeApplication.injector.instanceOf[SessionTimeoutPredicate],
          MockAuthenticationPredicate,
          MockIncomeSourceDetailsPredicate
        ).async {
          implicit request => implicit user => implicit sources =>
            Future.failed(new Exception("Unexpected Error"))
        }

      lazy val result = setupFailedFutureResult()(fakeRequestWithActiveSession)

      "render an ISE (500)" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
