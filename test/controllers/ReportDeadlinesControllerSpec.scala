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

import java.time.LocalDate

import assets.BaseTestConstants
import assets.MessagesLookUp.{NoReportDeadlines, Obligations => obligationsMessages}
import audit.AuditingService
import config.featureswitch.{FeatureSwitching, NextUpdates, ObligationsPage, ReportDeadlines}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.Inject
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockReportDeadlinesService
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.ReportDeadlinesService
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.Future

class ReportDeadlinesControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
                                            with MockReportDeadlinesService with FeatureSwitching{

  object TestReportDeadlinesController extends ReportDeadlinesController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[AuditingService],
    mockReportDeadlinesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ImplicitDateFormatterImpl]
  )

  val reportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]

  val date: LocalDate = LocalDate.now

  def mockPreviousObligations: OngoingStubbing[Future[ReportDeadlinesResponseModel]] = {
    when(reportDeadlinesService.getReportDeadlines(matches(true))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq(
        ReportDeadlinesModel(BaseTestConstants.testSelfEmploymentId, List(ReportDeadlineModel(date, date, date, "Quarterly", Some(date), "#001"))),
        ReportDeadlinesModel(BaseTestConstants.testPropertyIncomeId, List(ReportDeadlineModel(date, date, date, "EOPS", Some(date), "EOPS")))
      ))))
  }

  def mockNoPreviousObligations: OngoingStubbing[Future[ReportDeadlinesResponseModel]] = {
    when(reportDeadlinesService.getReportDeadlines(matches(true))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq(
      ))))
  }

  "The ReportDeadlinesController.getReportDeadlines function" when {

    "the Report Deadlines feature is disabled" should {

      lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)

      "return Redirect (303)" in {
        mockSingleBusinessIncomeSource()
        disable(ReportDeadlines)
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
        enable(ReportDeadlines)
      }

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "successfully retrieves a set of Business ReportDeadlines and Previous Obligations from the ReportDeadlines service" should {

          "return Status OK (200)" in {
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
            mockPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
          }
        }

        "successfully retrieves a set of Property ReportDeadlines and Previous from the ReportDeadlines service" should {

          lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return Status OK (200)" in {
            mockPropertyIncomeSource()
            mockPropertyIncomeSourceWithDeadlines()
            mockPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
          }
        }

        "successfully retrieves a set of both Business & Property ReportDeadlines and Previous Obligations from the ReportDeadlines service" should {

          lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return Status OK (200)" in {
            mockBothIncomeSourcesBusinessAligned()
            mockBothIncomeSourcesBusinessAlignedWithDeadlines()
            mockPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
          }
        }

        "successfully retrieves a set of only Business ReportDeadlines and no Previous Obligations from the ReportDeadlines service" should {

          "return Status OK (200)" in {
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
            mockNoPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
          }
        }

        "successfully retrieves a set of only Property ReportDeadlines and no Previous from the ReportDeadlines service" should {

          lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return Status OK (200)" in {
            mockPropertyIncomeSource()
            mockPropertyIncomeSourceWithDeadlines()
            mockNoPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
          }
        }

        "successfully retrieves a set of only both Business & Property ReportDeadlines and no Previous Obligations from the ReportDeadlines service" should {

          lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return Status OK (200)" in {
            mockBothIncomeSourcesBusinessAligned()
            mockBothIncomeSourcesBusinessAlignedWithDeadlines()
            mockNoPreviousObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the ReportDeadlines page" in {
            document.title shouldBe obligationsMessages.title
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

          s"have the correct no report deadlines message '${NoReportDeadlines.noReports}'" in {
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

		"the Next Updates feature switch is enabled" should {
			lazy val result = TestReportDeadlinesController.getReportDeadlines()(fakeRequestWithActiveSession)
			lazy val document = Jsoup.parse(bodyOf(result))

			"set Report Deadlines enabled" in {
				enable(ReportDeadlines)
				enable(NextUpdates)
			}

			"return Status OK (200)" in {
				mockSingleBusinessIncomeSource()
				mockSingleBusinessIncomeSourceWithDeadlines()
				mockPreviousObligations
				status(result) shouldBe Status.OK
			}

			"return HTML" in {
				contentType(result) shouldBe Some("text/html")
				charset(result) shouldBe Some("utf-8")
			}

			"render the next updates page" in {
				document.title shouldBe obligationsMessages.nextTitle
			}
		}
  }
}
