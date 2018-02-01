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

import assets.Messages.{ISE => errorMessages, ReportDeadlines => messages}
import assets.TestConstants.testUserName
import audit.AuditingService
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockReportDeadlinesService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class ReportDeadlinesControllerSpec
  extends TestSupport with MockAuthenticationPredicate
    with MockIncomeSourceDetailsPredicate with MockReportDeadlinesService {

  object TestReportDeadlinesController extends ReportDeadlinesController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[AuditingService]
  )

  "The ReportDeadlinesController.getNextObligation function" when {

    "called with an Authenticated HMRC-MTD-IT user with NINO" which {

      "successfully retrieves a set of Business ReportDeadlines from the ReportDeadlines service" should {

        lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockSingleBusinessIncomeSource()
          mockBusinessSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the ReportDeadlines page" in {
          document.title shouldBe messages.title
        }
      }

      "successfully retrieves a set of Property ReportDeadlines from the ReportDeadlines service" should {

        lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockPropertyIncomeSource()
          mockPropertySuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the ReportDeadlines page" in {
          document.title shouldBe messages.title
        }
      }

      "successfully retrieves a set of both Business & Property ReportDeadlines from the ReportDeadlines service" should {

        lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockBothIncomeSources()
          mockBusinessSuccess()
          mockPropertySuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the ReportDeadlines page" in {
          document.title shouldBe messages.title
        }
      }

      "doesn't retrieve any ReportDeadlines from the ReportDeadlines service" should {

        lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockNoIncomeSources()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the ReportDeadlines page" in {
          document.title shouldBe messages.title
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}
