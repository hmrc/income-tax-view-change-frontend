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

package controllers.agent

import controllers.agent.sessionUtils.SessionKeys
import mocks.auth.MockAuthActions
import mocks.views.agent.MockUTRError
import play.api
import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.agentAuthRetrievalSuccess
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments}
import views.html.agent.errorPages.UTRErrorView

class UTRErrorControllerSpec extends MockAuthActions
  with MockUTRError {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[UTRErrorView].toInstance(utrError),
    ).build()

  lazy val testUTRErrorController: UTRErrorController = app.injector.instanceOf[UTRErrorController]


  "show" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockAgentAuthException()

        val result = testUTRErrorController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session time out page" in {
        setupMockAgentAuthException(BearerTokenExpired())
        val result = testUTRErrorController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return SEE_OTHER to AgentErrorController" in {
        setupMockAgentAuthException(InsufficientEnrolments())

        val result = testUTRErrorController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
      }
    }

    "return OK and display the UTR Error page" in {
      setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
      mockUTRErrorResponse(HtmlFormat.empty)

      val result = testUTRErrorController.show()(fakeRequestWithClientUTR)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
    }
  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

        val result = testUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthException(BearerTokenExpired())
        val result = testUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return SEE_OTHER to AgentErrorController" in {
        setupMockAgentAuthException(InsufficientEnrolments())

        val result = testUTRErrorController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
      }
    }

    "redirect to the Enter Client UTR page and remove the clientUTR from session" in {
      setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
      mockUTRErrorResponse(HtmlFormat.empty)

      val result = testUTRErrorController.submit()(fakeRequestWithClientUTR)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
      result.futureValue.session(fakeRequestWithClientUTR).get(SessionKeys.clientUTR) shouldBe None
    }
  }
}
