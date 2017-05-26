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

import auth.{MockAuthorisedUser, MockUnauthorisedUser}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class AuthenticationPredicateSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  "The authentication async method" when {

    def result(authenticationPredicate: AuthenticationPredicate): Future[Result] = authenticationPredicate.async {
      implicit request =>
        Future.successful(Ok)
    } apply(FakeRequest())

    "called with an unauthenticated user (No Bearer Token in header)" should {
      object TestAuthenticationPredicate extends AuthenticationPredicate(MockUnauthorisedUser)

      "should return Unauthorised (401)" in {
        status(result(TestAuthenticationPredicate)) shouldBe Status.UNAUTHORIZED
      }
    }

    "called with an authenticated user" should {
      object TestAuthenticationPredicate extends AuthenticationPredicate(MockAuthorisedUser)

      "should return Ok (200)" in {
        status(result(TestAuthenticationPredicate)) shouldBe Status.OK
      }
    }
  }
}
