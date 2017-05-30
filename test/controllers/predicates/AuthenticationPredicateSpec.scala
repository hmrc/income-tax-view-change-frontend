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
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class AuthenticationPredicateSpec extends UnitSpec with MockitoSugar with WithFakeApplication with MockAuthenticationPredicate {

  "The authentication async method" when {

    def result(authenticationPredicate: AuthenticationPredicate): Future[Result] = authenticationPredicate.async {
      implicit request =>
        Future.successful(Ok)
    } apply FakeRequest()

    "called with an authenticated user" should {
      "should return Ok (200)" in {
        status(result(MockAuthenticated)) shouldBe Status.OK
      }
    }

    "called with an unauthenticated user (No Bearer Token in header)" should {
      "should be a rerdirect (303)" in {
        status(result(MockUnauthorised)) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result(MockUnauthorised)) shouldBe Some("/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9081%2Fcheck-your-income-tax-and-expenses&origin=income-tax-view-change-frontend")
      }
    }

    "called with a Timed Out user (Bearer Token Exprired)" should {

      "should be a rerdirect (303)" in {
        status(result(MockTimeout)) shouldBe Status.SEE_OTHER
      }

      "should redirect to GG Sign In" in {
        redirectLocation(result(MockTimeout)) shouldBe Some(controllers.routes.SessionTimeoutController.timeout().url)
      }
    }
  }
}
