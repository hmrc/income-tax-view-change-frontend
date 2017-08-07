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

import config.FrontendAppConfig
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class HomeControllerSpec extends TestSupport {

  object TestHomeController extends HomeController()(
    fakeApplication.injector.instanceOf[FrontendAppConfig],
    fakeApplication.injector.instanceOf[MessagesApi]
  )

  "navigating to the home page" should {
    lazy val result = TestHomeController.redirect(fakeRequestWithActiveSession)

    "return OK (303)" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the BTA home page" in {
      redirectLocation(result) shouldBe Some(TestHomeController.config.businessTaxAccount)
    }

  }

}
