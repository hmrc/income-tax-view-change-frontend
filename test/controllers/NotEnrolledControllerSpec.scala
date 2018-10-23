/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.Messages.{NotEnrolled => messages}
import config.FrontendAppConfig
import controllers.notEnrolled.NotEnrolledController
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import testUtils.TestSupport

class NotEnrolledControllerSpec extends TestSupport {

  object TestNotEnrolledController extends NotEnrolledController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi]
  )

  "the NotEnrolledController.show() action" should {

    lazy val result = TestNotEnrolledController.show(fakeRequestNoSession)
    lazy val document = Jsoup.parse(bodyOf(result))

    "return OK (200)" in {
      status(result) shouldBe Status.OK
    }

    "show the not_enrolled page" in {
      document.getElementById("page-heading").text() shouldBe messages.heading
    }

  }

}
