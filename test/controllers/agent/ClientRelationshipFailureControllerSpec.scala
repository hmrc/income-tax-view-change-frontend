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
import config.featureswitch.{AgentViewer, FeatureSwitching}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.views.MockClientRelationshipFailure
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired

class ClientRelationshipFailureControllerSpec extends TestSupport
  with MockClientRelationshipFailure
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with MockItvcErrorHandler {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  object TestClientRelationshipFailureController extends ClientRelationshipFailureController(
    clientRelationshipFailure,
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
        setupMockAgentAuthorisationException()

        val result = TestClientRelationshipFailureController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result = TestClientRelationshipFailureController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = TestClientRelationshipFailureController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the agent viewer feature switch is disabled" should {
      "return Not Found" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result = TestClientRelationshipFailureController.show()(fakeRequestWithClientDetails)

        status(result) shouldBe NOT_FOUND
      }
    }

    "the agent viewer feature switch is enabled" should {
      "return OK and display the client relationship failure page" in {
        enable(AgentViewer)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockClientRelationshipFailure(HtmlFormat.empty)

        val result = TestClientRelationshipFailureController.show()(fakeRequestWithClientDetails)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
  }

}
