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

import assets.Messages.{ExitSurvey => messages}
import audit.mocks.MockAuditingService
import audit.models.ExitSurveyAuditing.ExitSurveyAuditModel
import config.FrontendAppConfig
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{contentType, _}
import forms.ExitSurveyForm._
import models.ExitSurveyModel
import play.api.mvc.Result
import utils.TestSupport

import scala.concurrent.Future

class ExitSurveyControllerSpec extends TestSupport with MockAuditingService {

  object TestExitSurveyController extends ExitSurveyController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    mockAuditingService
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

    "render the Exit Survey page" in {
      document.title() shouldBe messages.title
    }
  }

  "Submitting the exit survey with no errors" should {
    lazy val model = ExitSurveyModel(Some("Very satisfied"), Some("No Improvements"))
    lazy val auditModel = ExitSurveyAuditModel(model)
    def result: Future[Result] = TestExitSurveyController.submit(
      fakeRequestWithActiveSession.withFormUrlEncodedBody(
        model.satisfaction.fold(satisfaction -> "")(satisfaction -> _),
        model.improvements.fold(improvements -> "")(improvements -> _)
      ))

    "return Status Redirect SEE_OTHER (303)" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "Redirect to the Thank You page" in {
      redirectLocation(result) shouldBe Some(controllers.routes.ThankYouController.show().url)
    }

    "Verify that an audit event has been called" in {
      await(result)
      verifyAudit(auditModel, controllers.routes.ExitSurveyController.show().url)
    }
  }

  "Submitting the exit survey with errors" should {
    lazy val result = TestExitSurveyController.submit(
      fakeRequestWithActiveSession.withFormUrlEncodedBody(
        improvements -> "a" * (improvementsMaxLength + 1)
      ))
    lazy val document = result.toHtmlDocument

    "return Bad Request (400)" in {
      status(result) shouldBe Status.BAD_REQUEST
    }

    "render the Exit Survey page" in {
      document.title() shouldBe messages.title
    }
  }

}
