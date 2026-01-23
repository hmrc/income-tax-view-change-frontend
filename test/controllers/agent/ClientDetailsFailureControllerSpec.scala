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

import mocks.auth.MockAuthActions
import mocks.views.agent.MockClientRelationshipFailure
import play.api
import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.agentAuthRetrievalSuccess
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import views.html.agent.errorPages.ClientRelationshipFailure

class ClientDetailsFailureControllerSpec extends MockAuthActions
  with MockClientRelationshipFailure
   {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClientRelationshipFailure].toInstance(clientRelationshipFailure)
    )
     .build()

  lazy val testController = app.injector.instanceOf[ClientRelationshipFailureController]

  "show" when {
    val action = testController.show()
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthException(mockFAF)()

        val result = action(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        val result = action(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "redirect them to the error page" in {
        setupMockAgentAuthException(mockFAF)(InsufficientEnrolments())

        val result = action(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
      }
    }

    "return OK and display the client relationship failure page" in {
      setupMockAgentAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
      mockBusinessIncomeSource()
      mockClientRelationshipFailure(HtmlFormat.empty)

      val result = action(fakeRequestWithClientDetails)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
    }
  }

}
