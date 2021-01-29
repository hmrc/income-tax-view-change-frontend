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
import controllers.agent.utils.SessionKeys
import forms.agent.ClientsUTRForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.views.MockEnterClientsUTR
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired

class EnterClientsUTRControllerSpec extends TestSupport
  with MockEnterClientsUTR
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  object TestEnterClientsUTRController extends EnterClientsUTRController(
    enterClientsUTR,
    mockAuthService
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    mockItvcErrorHandler,
    ec
  )

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException()

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result = TestEnterClientsUTRController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the agent viewer feature switch is disabled" should {
      "return Not Found" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe NOT_FOUND
      }
    }
    "the agent viewer feature switch is enabled" should {
      "return Ok and display the page to the user" in {
        enable(AgentViewer)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockEnterClientsUTR(HtmlFormat.empty)

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException()

        val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
      "the user has timed out" should {
        "redirect to the session timeout page" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithTimeoutSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
        }
      }
      "the user does not have an agent reference number" should {
        "return Ok with technical difficulties" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
          mockShowOkTechnicalDifficulties()

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
      "the agent viewer feature switch is disabled" should {
        "return Not Found" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockNotFound()

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

          status(result) shouldBe NOT_FOUND
        }
      }
      "the agent viewer feature switch is enabled" should {
        "redirect to the confirm client details page and add client details to session" when {
          "the utr entered is valid" in {
            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

            val result = await(TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> "1234567890"
            )))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EnterClientsUTRController.show().url) //TODO: Update this when the next page is available

            result.session.get(SessionKeys.clientFirstName) shouldBe Some("first name")
            result.session.get(SessionKeys.clientLastName) shouldBe Some("last name")
            result.session.get(SessionKeys.clientUTR) shouldBe Some("1234567890")
            result.session.get(SessionKeys.clientNino) shouldBe Some("nino")
            result.session.get(SessionKeys.clientMTDID) shouldBe Some("mtditid")
          }
        }
        "return a bad request" when {
          "the submitted utr is invalid" in {
            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockEnterClientsUTR(HtmlFormat.empty)

            val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> "invalid"
            ))

            status(result) shouldBe BAD_REQUEST
            contentType(result) shouldBe Some(HTML)
          }
        }
      }
    }
  }

}

