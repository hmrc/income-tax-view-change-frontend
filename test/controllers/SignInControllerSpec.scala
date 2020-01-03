/*
 * Copyright 2020 HM Revenue & Customs
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

import config.FrontendAppConfig
import play.api.http.Status
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import testUtils.TestSupport

class SignInControllerSpec extends TestSupport {

  object TestSignInController extends SignInController(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment]
  )

  "navigating to SignIn page" should {
    lazy val result = TestSignInController.signIn(fakeRequestNoSession)

    "return OK (303)" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "Redirect to GG Sign In on Company Auth Frontend" in {
      redirectLocation(result) shouldBe Some(
        "/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9081%2Freport-quarterly%2Fincome-and-expenses%2Fview%2Fobligations&origin=income-tax-view-change-frontend"
      )
    }

  }

}
