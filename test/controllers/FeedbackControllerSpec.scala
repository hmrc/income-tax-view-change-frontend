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

import connectors.FeedbackConnector
import controllers.feedback.FeedbackController
import enums.MTDIndividual
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{agentAuthRetrievalSuccess, testAuthSuccessResponse}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import uk.gov.hmrc.auth.core.InvalidBearerToken

import scala.concurrent.Future

class FeedbackControllerSpec extends MockAuthActions
  with ImplicitDateFormatter {

  lazy val mockFeedbackConnector: FeedbackConnector = mock(classOf[FeedbackConnector])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[FeedbackConnector].toInstance(mockFeedbackConnector)
    ).build()

  lazy val testController = app.injector.instanceOf[FeedbackController]

  val fields = Map(
    "feedback-name" -> "name",
    "feedback-email" -> "test@test.com",
    "feedback-rating" -> "2",
    "feedback-comments" -> "comments",
    "csrfToken" -> "token"
  )

  "show()" when {
    val action = testController.show()
    "the user is an enrolled authenticated individual" should {
      "render the feedback page" in {
        setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(testAuthSuccessResponse())
        setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
        val result: Future[Result] = action(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Send your feedback - Manage your Self Assessment - GOV.UK"
      }
    }
    testAuthFailures(action)(fakeRequestWithActiveSession)
  }

  "showAgent()" when {
    val action = testController.showAgent()
    val fakeRequest = fakeRequestWithActiveSession
    "the user is an agent" should {
      "render the feedback page" in {
        setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
        val result: Future[Result] = action(fakeRequest)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Send your feedback - Manage your Self Assessment - GOV.UK"
      }
      testAuthFailures(action)(fakeRequest)
    }
  }

  "submit()" when {
    val action = testController.submit()
    val fakeRequest = fakePostRequestBasedOnMTDUserType(MTDIndividual)

    "the user is an enrolled authenticated individual" should {
      "submit the form and redirect to the thank you page" when {
        "the form has no errors" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(testAuthSuccessResponse())
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockFeedbackConnector.submit(any())(any())).thenReturn(Future.successful(Right(())))
          val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(fields.toSeq: _*))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.feedback.routes.FeedbackController.thankYou().url)
        }
      }
      "return a BadRequest" when {
        "the form is incorrectly filled" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(testAuthSuccessResponse())
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          val result: Future[Result] = action(fakeRequest)
          status(result) shouldBe Status.BAD_REQUEST
        }
      }

      "render the error page" when {
        "the submit feedback call fails" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(testAuthSuccessResponse())
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockFeedbackConnector.submit(any())(any())).thenReturn(Future.successful(Left(500)))
          val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(fields.toSeq: _*))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }
      testAuthFailures(action)(fakeRequest)
    }
  }

  "submitAgent()" when {
    "the user is an agent" should {
      val action = testController.submitAgent()
      val fakeRequest = fakePostRequestWithActiveSession
      "submit the form and redirect to the thank you page" when {
        "the form has no errors" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
          when(mockFeedbackConnector.submit(any())(any())).thenReturn(Future.successful(Right(())))
          val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(fields.toSeq: _*))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.feedback.routes.FeedbackController.thankYouAgent().url)
        }
      }
      "return a BadRequest" when {
        "the form is incorrectly filled" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          val result: Future[Result] = action(fakeRequest)
          status(result) shouldBe Status.BAD_REQUEST
        }
      }

      "render the error page" when {
        "the submit feedback call fails" in {
          setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockFeedbackConnector.submit(any())(any())).thenReturn(Future.successful(Left(500)))
          val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(fields.toSeq: _*))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
      testAuthFailures(action)(fakeRequest)
    }
  }

  "thankyou" when {
    val action = testController.thankYou
    "the user is an enrolled authenticated individual" should {
      "render the thank you page" in {
        setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(testAuthSuccessResponse())
        setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
        val result: Future[Result] = action(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Thank you - Manage your Self Assessment - GOV.UK"
      }
    }
    testAuthFailures(action)(fakeRequestWithActiveSession)
  }

  "thankyouAgent" when {
    val action = testController.thankYouAgent
    val fakeRequest = fakeRequestWithActiveSession
    "the user is an agent" should {
      "render the thank you page" in {
        setupMockAuthorisedUserNoCheckAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
        val result: Future[Result] = action(fakeRequest)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Thank you - Manage your Self Assessment - GOV.UK"
      }
      testAuthFailures(action)(fakeRequest)
    }
  }

  private def testAuthFailures(action: Action[AnyContent])(fakeRequest: FakeRequest[AnyContentAsEmpty.type]) = {
    "redirect to sign in" when {
      "the user is not authenticated" in {
        setupMockUserAuthNoCheckException(mockFAF)(new InvalidBearerToken)

        val result: Future[Result] = action(fakeRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "redirect to the session timeout page" when {
      "the user has timed out" in {
        val result: Future[Result] = action(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
  }
}
