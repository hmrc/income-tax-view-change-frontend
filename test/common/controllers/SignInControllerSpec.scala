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

package common.controllers

import common.config.FrontendAppConfig
import common.testUtils.TestSupport
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*

import java.net.URLEncoder

class SignInControllerSpec extends TestSupport {

  object TestSignInController extends SignInController(
    app.injector.instanceOf[FrontendAppConfig])(app.injector.instanceOf[MessagesControllerComponents])
  
  "navigating to SignIn page" should {
    lazy val result = TestSignInController.signIn(fakeRequestNoSession)

    "return OK (303)" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "Redirect to GG Sign In on Company Auth Frontend" in {
      val redirectUrl = URLEncoder.encode("http://localhost:9081/report-quarterly/income-and-expenses/view", "UTF-8")
      redirectLocation(result) shouldBe Some(
        appConfig.ggSignInUrl + "?continue_url=" + redirectUrl + "&origin=" + appConfig.appName
      )
    }

  }

}
