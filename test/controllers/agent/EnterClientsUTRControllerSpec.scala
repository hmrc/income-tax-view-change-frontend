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

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testArn, testMtditid, testNino}
import config.featureswitch.{AgentViewer, FeatureSwitching}
import controllers.agent.utils.SessionKeys
import forms.agent.ClientsUTRForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.MockClientRelationshipService
import mocks.views.MockEnterClientsUTR
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.agent.ClientRelationshipService._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException

class EnterClientsUTRControllerSpec extends TestSupport
  with MockEnterClientsUTR
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with MockClientRelationshipService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  object TestEnterClientsUTRController extends EnterClientsUTRController(
    enterClientsUTR,
    mockClientRelationshipService,
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
            val validUTR: String = "1234567890"

            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockCheckAgentClientRelationship(validUTR, testArn)(
              response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
            )

            val result = await(TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            )))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show().url)

            result.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
            result.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
            result.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
            result.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
            result.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
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
        "return an exception" when {
          "a client details not found error is returned from the relationship check" in {
            val validUTR: String = "1234567890"

            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockCheckAgentClientRelationship(validUTR, testArn)(
              response = Left(CitizenDetailsNotFound)
            )

            val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            ))

            intercept[InternalServerException](await(result)).message shouldBe "[EnterClientsUTRController][submit] - unable to verify client relationship"
          }
          "a business details not found error is returned from the relationship check" in {
            val validUTR: String = "1234567890"

            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockCheckAgentClientRelationship(validUTR, testArn)(
              response = Left(BusinessDetailsNotFound)
            )

            val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            ))

            intercept[InternalServerException](await(result)).message shouldBe "[EnterClientsUTRController][submit] - unable to verify client relationship"
          }
          "an agent client relationship not found error is returned from the relationship check" in {
            val validUTR: String = "1234567890"

            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockCheckAgentClientRelationship(validUTR, testArn)(
              response = Left(NoAgentClientRelationship)
            )

            val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            ))

            intercept[InternalServerException](await(result)).message shouldBe "[EnterClientsUTRController][submit] - unable to verify client relationship"
          }
          "an unexpected response is returned from the relationship check" in {
            val validUTR: String = "1234567890"

            enable(AgentViewer)
            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockCheckAgentClientRelationship(validUTR, testArn)(
              response = Left(UnexpectedResponse)
            )

            val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            ))

            intercept[InternalServerException](await(result)).message shouldBe "[EnterClientsUTRController][submit] - Unexpected response received"
          }
        }
      }
    }
  }

}

