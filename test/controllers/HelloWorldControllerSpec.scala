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

package controllers

import auth.{MockAuthorisedUser, MockUnauthorisedUser}
import config.MockAppConfig
import controllers.predicates.AuthenticationPredicate
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, _}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class HelloWorldControllerSpec extends UnitSpec with WithFakeApplication {

  "Unauthorised Tests" should {

    object TestHelloWorldController extends HelloWorld()(MockAppConfig, new AuthenticationPredicate(MockUnauthorisedUser))

    "return 401" in {
      val result = TestHelloWorldController.helloWorld()(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "Authorised Tests" should {

    object TestHelloWorldController extends HelloWorld()(MockAppConfig, new AuthenticationPredicate(MockAuthorisedUser))

    "return 200" in {
      val result = TestHelloWorldController.helloWorld()(FakeRequest())
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = TestHelloWorldController.helloWorld()(FakeRequest())
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }


}
