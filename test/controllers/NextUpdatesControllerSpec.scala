/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants
import testConstants.MessagesLookUp.{NoNextUpdates, Obligations => obligationsMessages}
import audit.AuditingService
import auth.FrontendAuthorisedFunctions
import mocks.auth.MockFrontendAuthorisedFunctions
import config.{ItvcErrorHandler}
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockIncomeSourceDetailsService, MockNextUpdatesService}
import mocks.MockItvcErrorHandler
import mocks.views.agent.MockNextUpdates
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, NextUpdatesResponseModel, ObligationsModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.NextUpdatesService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import uk.gov.hmrc.auth.core.BearerTokenExpired
import views.html.{NextUpdates, NoNextUpdates}

import scala.concurrent.{Future}

class NextUpdatesControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockNextUpdatesService with MockNextUpdates with MockItvcErrorHandler with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsService {

  trait AgentTestsSetup {
    val controller = new controllers.NextUpdatesController(
      app.injector.instanceOf[NoNextUpdates],
      app.injector.instanceOf[NextUpdates],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicateNoCache,
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[AuditingService],
      mockNextUpdatesService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[BtaNavBarPredicate],
      appConfig,
      mockAuthService,
    )(
      app.injector.instanceOf[MessagesControllerComponents],
      mockItvcErrorHandler,
      ec
    )
  }

  object TestNextUpdatesController extends NextUpdatesController(
    app.injector.instanceOf[NoNextUpdates],
    app.injector.instanceOf[NextUpdates],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicateNoCache,
    app.injector.instanceOf[services.IncomeSourceDetailsService],
    app.injector.instanceOf[AuditingService],
    mockNextUpdatesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[BtaNavBarPredicate],
    appConfig,
    app.injector.instanceOf[FrontendAuthorisedFunctions],
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    mockItvcErrorHandler,
    ec
  )

  val NextUpdatesService: NextUpdatesService = mock[NextUpdatesService]

  val date: LocalDate = LocalDate.now

  val obligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(BaseTestConstants.testSelfEmploymentId, List(NextUpdateModel(date, date, date, "Quarterly", Some(date), "#001"))),
    NextUpdatesModel(BaseTestConstants.testPropertyIncomeId, List(NextUpdateModel(date, date, date, "EOPS", Some(date), "EOPS")))
  ))

  def mockObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(NextUpdatesService.getNextUpdates(matches(true))(any(), any()))
      .thenReturn(Future.successful(obligationsModel))
  }

  def mockNoObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(NextUpdatesService.getNextUpdates(matches(true))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq())))
  }

  /* INDIVIDUAL **/
  "The NextUpdatesController.getNextUpdates function" when {

    "the Next Updates feature switch is disabled" should {

      lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
      lazy val document = Jsoup.parse(contentAsString(result))

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "successfully retrieves a set of Business NextUpdates and Previous Obligations from the NextUpdates service" should {

          "return Status OK (200)" in {
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
            mockObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "successfully retrieves a set of Property NextUpdates and Previous from the NextUpdates service" should {

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            mockPropertyIncomeSource()
            mockPropertyIncomeSourceWithDeadlines()
            mockObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "successfully retrieves a set of both Business & Property NextUpdates and Previous Obligations from the NextUpdates service" should {

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            mockBothIncomeSourcesBusinessAligned()
            mockBothIncomeSourcesBusinessAlignedWithDeadlines()
            mockObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "successfully retrieves a set of only Business NextUpdates and no Previous Obligations from the NextUpdates service" should {

          "return Status OK (200)" in {
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
            mockNoObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "successfully retrieves a set of only Property NextUpdates and no Previous from the NextUpdates service" should {

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            mockPropertyIncomeSource()
            mockPropertyIncomeSourceWithDeadlines()
            mockNoObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "successfully retrieves a set of only both Business & Property NextUpdates and no Previous Obligations from the NextUpdates service" should {

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            mockBothIncomeSourcesBusinessAligned()
            mockBothIncomeSourcesBusinessAlignedWithDeadlines()
            mockNoObligations
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NextUpdates page" in {
            document.title shouldBe obligationsMessages.nextTitle
          }
        }

        "receives an Error from the NextUpdates Service" should {

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)

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

          lazy val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)" in {
            mockNoIncomeSources()
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the NoNextUpdates page" in {
            document.title shouldBe NoNextUpdates.title
          }

          s"have the heading '${NoNextUpdates.heading}'" in {
            document.select("h1").text() shouldBe NoNextUpdates.heading
          }

          s"have the correct no next updates message '${NoNextUpdates.noUpdates}'" in {
            document.select("p.govuk-body").text shouldBe NoNextUpdates.noUpdates
          }
        }

      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          setupMockAuthorisationException()
          val result = TestNextUpdatesController.getNextUpdates()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }

  }

  /* AGENT **/
  "The NextUpdatesController.getNextUpdatesAgent function" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in new AgentTestsSetup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new AgentTestsSetup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new AgentTestsSetup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the user has all correct details" should {
      "return Status OK (200) when we have obligations" in new AgentTestsSetup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSourceWithDeadlines
        mockSingleBusinessIncomeSource()
        mockObligations
        mockNextUpdates(obligationsModel, controllers.agent.routes.HomeController.show().url, true)(HtmlFormat.empty)

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }
      "return Status INTERNAL_SERVER_ERROR (500) when we have no obligations" in new AgentTestsSetup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockNoObligations
        mockNoIncomeSourcesWithDeadlines
        mockShowInternalServerError()

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
