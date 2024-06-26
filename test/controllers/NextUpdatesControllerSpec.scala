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

package controllers

import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockIncomeSourceDetailsService, MockNextUpdatesService, MockOptOutService}
import mocks.views.agent.MockNextUpdates
import models.admin.OptOut
import models.nextUpdates.ObligationStatus.Fulfilled
import models.nextUpdates._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.optout.OptOutService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import views.html.nextUpdates.{NextUpdates, NextUpdatesOptOut, NoNextUpdates}

import java.time.LocalDate
import scala.concurrent.Future

class NextUpdatesControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockNextUpdatesService with MockNextUpdates with MockItvcErrorHandler with MockIncomeSourceDetailsService
  with MockIncomeSourceDetailsPredicate
  with MockAuditingService
  with MockOptOutService
  with TestSupport {

  val nextTitle: String = messages("htmlTitle", messages("nextUpdates.heading"))

  trait AgentTestsSetup {
    val controller = new controllers.NextUpdatesController(
      app.injector.instanceOf[NoNextUpdates],
      app.injector.instanceOf[NextUpdates],
      app.injector.instanceOf[NextUpdatesOptOut],
      mockIncomeSourceDetailsService,
      mockAuditingService,
      mockNextUpdatesService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[OptOutService],
      appConfig,
      mockAuthService,
      testAuthenticator
    )(
      app.injector.instanceOf[MessagesControllerComponents],
      mockItvcErrorHandler,
      ec
    )
  }

  object TestNextUpdatesController extends NextUpdatesController(
    app.injector.instanceOf[NoNextUpdates],
    app.injector.instanceOf[NextUpdates],
    app.injector.instanceOf[NextUpdatesOptOut],
    mockIncomeSourceDetailsService,
    mockAuditingService,
    mockNextUpdatesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[OptOutService],
    appConfig,
    mockAuthService,
    testAuthenticator
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    mockItvcErrorHandler,
    ec
  )

  val obligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(BaseTestConstants.testSelfEmploymentId, List(NextUpdateModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", Fulfilled.code))),
    NextUpdatesModel(BaseTestConstants.testPropertyIncomeId, List(NextUpdateModel(fixedDate, fixedDate, fixedDate, "EOPS", Some(fixedDate), "EOPS", Fulfilled.code)))
  ))

  val nextUpdatesViewModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(
    NextUpdatesModel(BaseTestConstants.testSelfEmploymentId, List(NextUpdateModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", Fulfilled.code))),
    NextUpdatesModel(BaseTestConstants.testPropertyIncomeId, List(NextUpdateModel(fixedDate, fixedDate, fixedDate, "EOPS", Some(fixedDate), "EOPS", Fulfilled.code)))
  )).obligationsByDate.map { case (date: LocalDate, obligations: Seq[NextUpdateModelWithIncomeType]) =>
    DeadlineViewModel(getQuarterType(obligations.head.incomeType), standardAndCalendar = false, date, obligations, Seq.empty)
  })

  private def getQuarterType(string: String) = {
    if (string == "Quarterly") QuarterlyObligation else EopsObligation
  }

  def mockObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(mockNextUpdatesService.getNextUpdates(matches(true))(any(), any()))
      .thenReturn(Future.successful(obligationsModel))
  }

  def mockNoObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(mockNextUpdatesService.getNextUpdates(matches(true))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq())))
  }

  def mockViewModel: OngoingStubbing[NextUpdatesViewModel] = {
    when(mockNextUpdatesService.getNextUpdatesViewModel(any())(any()))
      .thenReturn(nextUpdatesViewModel)
  }

  /* INDIVIDUAL **/
  "The NextUpdatesController.show function" when {

    disableAllSwitches()

    "the Next Updates feature switch is disabled" should {

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "successfully retrieves a set of Business NextUpdates and Previous Obligations from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBusinessIncomeSource()
          mockSingleBusinessIncomeSourceWithDeadlines()
          mockObligations
          mockViewModel
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of Property NextUpdates and Previous from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockPropertyIncomeSource()
          mockPropertyIncomeSourceWithDeadlines()
          mockObligations
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of both Business & Property NextUpdates and Previous Obligations from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockBothIncomeSourcesBusinessAligned()
          mockBothIncomeSourcesBusinessAlignedWithDeadlines()
          mockObligations
          val result = TestNextUpdatesController.show(origin = Some("PTA"))(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only Business NextUpdates and no Previous Obligations from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBusinessIncomeSource()
          mockSingleBusinessIncomeSourceWithDeadlines()
          mockNoObligations
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only Property NextUpdates and no Previous from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockPropertyIncomeSource()
          mockPropertyIncomeSourceWithDeadlines()
          mockNoObligations
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only both Business & Property NextUpdates and no Previous Obligations from the NextUpdates service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockViewModel
          mockBothIncomeSourcesBusinessAligned()
          mockBothIncomeSourcesBusinessAlignedWithDeadlines()
          mockNoObligations
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe nextTitle
          }
        }

        "receives an Error from the NextUpdates Service" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBusinessIncomeSource()
          mockErrorIncomeSourceWithDeadlines()
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)

          "return Status ISE (500)" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
        }

        "doesn't have any Income Source" should {

          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockNoIncomeSources()
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NoNextUpdates page" in {
            document.title shouldBe messages("htmlTitle", messages("obligations.heading"))
          }

          s"have the heading ${messages("obligations.heading")}" in {
            document.select("h1").text() shouldBe messages("obligations.heading")
          }

          s"have the correct no next updates message ${messages("obligations.noReports")}" in {
            document.select("p.govuk-body").text shouldBe messages("obligations.noReports")
          }
        }

      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          setupMockAuthorisationException()
          val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }

    "the Next Updates feature switch disabled: other cases" should {

      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      mockSingleBusinessIncomeSourceError()
      mockSingleBusinessIncomeSourceWithDeadlines()
      mockObligations
      val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "failed to retrieve a set of Business NextUpdates" should {

          "return Status ERROR (500)" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "the opt out feature switch is enabled and optOutService returns failed future" should {
      s"show an INTERNAL SERVER ERROR page with HTTP ${Status.INTERNAL_SERVER_ERROR} Status " in {
        enable(OptOut)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBusinessIncomeSourceError()
        mockSingleBusinessIncomeSourceWithDeadlines()
        mockGetNextUpdatesQuarterlyReportingContentChecks(Future.failed(new Exception("api failure")))
        mockObligations
        val result = TestNextUpdatesController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  /* AGENT **/
  "The NextUpdatesController.showAgent function" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in new AgentTestsSetup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new AgentTestsSetup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.showAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new AgentTestsSetup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the user has all correct details" should {
      "return Status OK (200) when we have obligations" in new AgentTestsSetup {
        disableAllSwitches()

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSourceWithDeadlines()
        mockSingleBusinessIncomeSource()
        mockViewModel
        mockObligations
        mockNextUpdates(nextUpdatesViewModel, controllers.routes.HomeController.showAgent.url, true)(HtmlFormat.empty)

        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }

      "return Status INTERNAL_SERVER_ERROR (500) when we have no obligations" in new AgentTestsSetup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockNoObligations
        mockNoIncomeSourcesWithDeadlines()
        mockShowInternalServerError()

        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
