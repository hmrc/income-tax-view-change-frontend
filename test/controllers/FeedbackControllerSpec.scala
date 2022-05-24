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

import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.feedback.FeedbackController
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import testConstants.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.feedback.{Feedback, FeedbackThankYou}

import scala.concurrent.Future

class FeedbackControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with MockItvcErrorHandler with ImplicitDateFormatter with TestSupport with FeatureSwitching {

  val mockErrorHandler: ItvcErrorHandler = mock[ItvcErrorHandler]
  val mockFeedbackView: Feedback = mock[Feedback]
  val mockThankYouView: FeedbackThankYou = mock[FeedbackThankYou]
  val mockHttpClient: HttpClient = mock[HttpClient]

  object TestFeedbackController extends FeedbackController()(
    app.injector.instanceOf[FrontendAppConfig],
    ec,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NavBarPredicate],
    mockFeedbackView,
    mockThankYouView,
    mockItvcHeaderCarrierForPartialsConverter,
    mockHttpClient,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[MessagesControllerComponents],
    mockErrorHandler,
    mockItvcErrorHandler
  )

  ".show" when {
    "called with an authenticated HMRC-MTD-IT user and successfully retrieved income source" when {
      "and firstAccountingPeriodEndDate is missing from income sources" should {
        "return an Internal Server Error (500)" in {

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)

          lazy val result = TestFeedbackController.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }


      "successfully retrieves income sources and and display feedback page" should {
        "return an OK (200)" in {
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockFeedbackView(any(), any(), any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)

          lazy val result = TestFeedbackController.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          session(result).get("Referer") shouldBe Some("/test/url")

        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {

        setupMockAuthorisationException()
        val result = TestFeedbackController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  ".submit" when {
    "successfully submitted form with no errors" should {
      "return an SEE_OTHER (303)" in {
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "test")))

        when(mockThankYouView(any(), any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)

        lazy val result = TestFeedbackController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(fields.toSeq: _*))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.feedback.routes.FeedbackController.thankYou().url)
      }
    }

    "successfully submitted form with no errors for agent" should {
      "return an SEE_OTHER (303)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        mockBothIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "test")))

        when(mockThankYouView(any(), any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)

        lazy val result = TestFeedbackController.submitAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(fields.toSeq: _*))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.feedback.routes.FeedbackController.thankYouAgent().url)
      }
    }
  }

  "show agent" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestFeedbackController.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result: Future[Result] = TestFeedbackController.showAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return SEE_OTHER with agent error controller redirect" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)

        val result: Future[Result] = TestFeedbackController.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
      }
    }
    "all data is returned successfully" should {
      "show the tax years page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        mockBothIncomeSources()

        when(mockFeedbackView(any(), any(), any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = TestFeedbackController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        session(result).get("Referer") shouldBe Some("/test/url")
      }
    }
  }

  val fields = Map(
    "feedback-name" -> "name",
    "feedback-email" -> "test@test.com",
    "feedback-rating" -> "2",
    "feedback-comments" -> "comments",
    "csrfToken" -> "token"
  )

}
