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

import audit.models.ConfirmClientDetailsAuditModel
import authV2.AuthActionsTestData
import controllers.agent.sessionUtils.SessionKeys
import mocks.auth.MockAuthActions
import mocks.views.agent.MockConfirmClient
import models.sessionData.SessionDataModel
import models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import play.api
import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import testConstants.BaseTestConstants.{testMtditid, testNino}
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments}
import views.html.agent.confirmClient

class ConfirmClientUTRControllerSpec extends MockAuthActions
  with MockConfirmClient {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[confirmClient].toInstance(mockConfirmClient),
    ).build()

  val testConfirmClientUTRController = app.injector.instanceOf[ConfirmClientUTRController]

  Map("primary agent" -> false, "supporting agent" -> true).foreach { case (agentType, isSupportingAgent) =>
    val fakeRequest = fakeRequestUnconfirmedClient(isSupportingAgent = isSupportingAgent)
    "show" when {
      s"the user is a $agentType" that {
        "is not authenticated" should {
          "redirect the user to authenticate" in {
            setupMockAgentWithClientAuthorisationException(isSupportingAgent = isSupportingAgent)

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
          }
        }

        s"has timed out" should {
          "redirect to the session timeout page" in {
            setupMockAgentWithClientAuthorisationException(exception = BearerTokenExpired(), isSupportingAgent)

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }

        s"does not have an agent reference number" should {
          "redirect to agent error controller" in {
            setupMockAgentWithClientAuthorisationException(exception = InsufficientEnrolments("HMRC-AS-AGENT is missing"), isSupportingAgent)

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show.url)
          }
        }

        s"does not have client details in session" should {
          "redirect to the Enter Client UTR page" in {
            setupMockGetSessionDataNotFound()

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show.url)
          }
        }

        "fails the auth check to find a valid agent-client relationship" should {
          "redirect to the Agent Client Relationship error page" in {
            setupMockAgentWithClientAuthorisationException(exception = InsufficientEnrolments("HMRC-MTD-IT is missing"), isSupportingAgent)

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show.url)
          }
        }

        "that is fully authenticated" should {
          "return OK and display confirm Client details page" in {
            setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent)
            mockConfirmClientResponse(HtmlFormat.empty)

            val result = testConfirmClientUTRController.show()(fakeRequest)

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
          }
        }
      }
    }

    "submit" when {
      s"the user is a $agentType" that {
        "is not authenticated" should {
          "redirect the user to authenticate" in {
            setupMockAgentWithClientAuthorisationException(isSupportingAgent = isSupportingAgent)

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
          }
        }

        "has timed out" should {
          "redirect to the session timeout page" in {
            setupMockAgentWithClientAuthorisationException(exception = BearerTokenExpired(), isSupportingAgent)

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }

        "does not have an agent reference number" should {
          "redirect to agent error controller" in {
            setupMockAgentWithClientAuthorisationException(exception = InsufficientEnrolments("HMRC-AS-AGENT is missing"), isSupportingAgent)

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show.url)
          }
        }

        "has no client details in session" should {
          "redirect to the Enter Client UTR page" in {
            setupMockGetSessionDataNotFound()

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show.url)
          }
        }

        "fails the auth check to find a valid agent-client relationship" should {
          "redirect to the Agent Client Relationship error page" in {
            setupMockAgentWithClientAuthorisationException(exception = InsufficientEnrolments("HMRC-MTD-IT is missing"), isSupportingAgent)

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show.url)
          }
        }

        "is fully authenticated" should {
          "redirect to Home page and relevant data added to session or sent to session data service successfully" in {
            setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent)

            setupMockPostSessionData(Right(SessionDataPostSuccess(OK)))

            val result = testConfirmClientUTRController.submit()(fakeRequest)

            val expectedAudit = ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNino, mtditid = AuthActionsTestData.mtdId, arn = AuthActionsTestData.arn, saUtr = AuthActionsTestData.saUtr, credId = Some(AuthActionsTestData.credentials.providerId))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
            if (!appConfig.isSessionDataStorageEnabled) {
              session(result).get(SessionKeys.confirmedClient) shouldBe Some("true")
            }
            else{
              val sessionDataModel: SessionDataModel = SessionDataModel(AuthActionsTestData.mtdId, testNino, AuthActionsTestData.saUtr, isSupportingAgent)
              verify(mockSessionDataService, times(1)).postSessionData(ArgumentMatchers.eq(sessionDataModel))(any())
            }
            verifyExtendedAuditSent(expectedAudit)
          }
          if (appConfig.isSessionDataStorageEnabled) {
            "throw an error if session data service flag is true and request to session data service is unsuccessful" in {
              setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent)

              setupMockPostSessionData(Left(SessionDataPostFailure(INTERNAL_SERVER_ERROR, "POST to session data service was unsuccessful TEST")))

              val result = testConfirmClientUTRController.submit()(fakeRequest)

              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
    }
  }
}
