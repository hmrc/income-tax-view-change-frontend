/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport

class SignOutControllerSpec extends TestSupport {

  object TestSignOutController extends SignOutController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents]
  )

  "navigating to signout page" should {
    lazy val result = TestSignOutController.signOut(fakeRequestWithActiveSession)

    "return OK (303)" in {
      status(result) shouldBe Status.SEE_OTHER
    }
    "redirect to the exitSurvey page" in {
      redirectLocation(await(result)).head endsWith appConfig.exitSurveyUrl
    }

  }

}
