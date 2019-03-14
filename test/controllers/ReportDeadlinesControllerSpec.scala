/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants.testUserDetails
import assets.Messages.{NoReportDeadlines, ReportDeadlines => messages}
import audit.AuditingService
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockReportDeadlinesService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._

class ReportDeadlinesControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockReportDeadlinesService {

  object TestReportDeadlinesController extends ReportDeadlinesController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[AuditingService],
    mockReportDeadlinesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi]
  )

  "The ReportDeadlinesController.getNextObligation function" when {

    "the Report Deadlines feature is disabled" should {

      lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)

      "return Redirect (303)" in {
        mockSingleBusinessIncomeSource()
        TestReportDeadlinesController.config.features.reportDeadlinesEnabled(false)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the Income Tax Home Page" in {
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }
    }

    "the Report Deadlines feature is enabled" should {

      lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
      lazy val document = Jsoup.parse(bodyOf(result))

      "set Report Deadlines enabled" in {
        TestReportDeadlinesController.config.features.reportDeadlinesEnabled(true)
        TestReportDeadlinesController.config.features.reportDeadlinesEnabled() shouldBe true
      }

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "successfully retrieves a set of Business ReportDeadlines from the ReportDeadlines service" should {

          "return Status OK (200)" in {
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
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
            mockPropertyIncomeSourceWithDeadlines()
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
            mockBothIncomeSourcesBusinessAligned()
            mockBothIncomeSourcesBusinessAlignedWithDeadlines()
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

        "receives an Error from the ReportDeadlines Service" should {

          lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return Status ISE (500)" in {
            mockSingleBusinessIncomeSource()
            mockErrorIncomeSourceWithDeadlines()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
        }

        "doesn't have any Income Source" should {

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

          "render the NoReportDeadlines page" in {
            document.title shouldBe NoReportDeadlines.title
          }

          s"have the correct no report deadlines messges '${NoReportDeadlines.noReports}'" in {
            document.getElementById("p1").text shouldBe NoReportDeadlines.noReports
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

  "getNextObligation" should {
    "show the obligations page" when {

      "the obligationsPageEnabled feature switch is set to true" in {
        mockBothIncomeSourcesBusinessAligned()
        mockBothIncomeSourcesBusinessAlignedWithDeadlines()

        TestReportDeadlinesController.config.features.reportDeadlinesEnabled(true)

        TestReportDeadlinesController.config.features.obligationsPageEnabled(true)

        val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        val document = Jsoup.parse(bodyOf(result))

        document.getElementById("page-heading").text shouldBe "Returns"

      }
    }
    "show the report deadlines page" when {
      "the obligationsPageEnabled feature switch is set to false" in {
        mockBothIncomeSourcesBusinessAligned()
        mockBothIncomeSourcesBusinessAlignedWithDeadlines()

        TestReportDeadlinesController.config.features.reportDeadlinesEnabled(true)

        TestReportDeadlinesController.config.features.obligationsPageEnabled(false)
        val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
        val document = Jsoup.parse(bodyOf(result))

        document.select("#page-heading").text shouldBe "Report deadlines"

      }
    }
  }
}
