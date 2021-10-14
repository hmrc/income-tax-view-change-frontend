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

package controllers.agent

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import config.featureswitch.FeatureSwitching
import controllers.agent.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.views.agent.MockUTRError
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.{BearerTokenExpired, Enrolment}

class UTRErrorControllerSpec extends TestSupport
  with MockUTRError
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with MockItvcErrorHandler{

  object TestUTRErrorController extends UTRErrorController(
    utrError,
    mockAuthService
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    mockItvcErrorHandler,
    ec
  )

  "show" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestUTRErrorController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session time out page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestUTRErrorController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment,  withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestUTRErrorController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

		"redirect to the Enter Client UTR page when there is no client UTR in session" in {
			setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
			mockUTRError(HtmlFormat.empty)

			val result = TestUTRErrorController.show()(fakeRequestWithActiveSession)

			status(result) shouldBe SEE_OTHER
			redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
		}

		"return OK and display the UTR Error page" in {
			setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
			mockUTRError(HtmlFormat.empty)

			val result = TestUTRErrorController.show()(fakeRequestWithClientUTR)

			status(result) shouldBe OK
			contentType(result) shouldBe Some(HTML)
			verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
			verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
		}
  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

		"redirect to the Enter Client UTR page and remove the clientUTR from session" in {
			setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

			val result = TestUTRErrorController.submit()(fakeRequestWithClientUTR)

			status(result) shouldBe SEE_OTHER
			redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
			result.futureValue.session(fakeRequestWithClientUTR).get(SessionKeys.clientUTR) shouldBe None
			verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
			verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
		}
  }

}
