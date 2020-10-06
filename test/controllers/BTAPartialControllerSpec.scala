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

import assets.MessagesLookUp.{BtaPartial => btaPartialMessages}
import config.FrontendAppConfig
import controllers.predicates.SessionTimeoutPredicate
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport

class BTAPartialControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestBTAPartialController extends BTAPartialController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate
    )(
    ec,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents]
  )

  "The BTAPartialController.setupPartial action" when {

    "authorised with active session" should {

      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe btaPartialMessages.heading
      }
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
