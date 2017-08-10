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
import play.api.test.Helpers.{contentType, _}
import utils.TestSupport
import assets.Messages.{ExitSurvey => messages}

class ExitSurveyControllerSpec extends TestSupport {

  object TestExitSurveyController extends ExitSurveyController()(
    fakeApplication.injector.instanceOf[FrontendAppConfig],
    fakeApplication.injector.instanceOf[MessagesApi]
  )

  "Navigating to the exit survey page" should {
    lazy val result = TestExitSurveyController.show(fakeRequestWithActiveSession)
    lazy val document = result.toHtmlDocument

    "return Status OK (200)" in {
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "render the EstimatedTaxLiability page" in {
      document.title() shouldBe messages.title
    }
  }

}
