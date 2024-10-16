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
import config.featureswitch.FeatureSwitching
import controllers.agent.sessionUtils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.views.agent.MockConfirmClient
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testArn, testCredId, testMtditidAgent, testNino, testSaUtrId}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments}

class ConfirmClientUTRControllerSpec extends TestSupport
  with MockConfirmClient
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with MockAuthenticationPredicate
  with MockItvcErrorHandler {

  object TestConfirmClientUTRController extends ConfirmClientUTRController(
    confirmClient,
    mockAuthService,
    mockAuditingService,
    testAuthenticator
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    mockItvcErrorHandler,
    ec
  )

  "show" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockEnroledAgentAuthorisationException()

        val result = TestConfirmClientUTRController.show()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockEnroledAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestConfirmClientUTRController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = TestConfirmClientUTRController.show()(fakeRequestWithClientDetails)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "there are no client details in session" should {
      "redirect to the Enter Client UTR page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

        val result = TestConfirmClientUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show.url)
      }
    }

    "the auth check fails to find a valid agent-client relationship" should {
      "redirect to the Agent Client Relationship error page" in {
        setupMockAgentAuthorisationException(InsufficientEnrolments())

        val result = TestConfirmClientUTRController.show()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show.url)
      }
    }

    "return OK and display confirm Client details page" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockConfirmClient(HtmlFormat.empty)

      val result = TestConfirmClientUTRController.show()(fakeRequestWithClientDetails)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
    }
  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockEnroledAgentAuthorisationException()

        val result = TestConfirmClientUTRController.submit()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockEnroledAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestConfirmClientUTRController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = TestConfirmClientUTRController.submit()(fakeRequestWithClientDetails)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "there are no client details in session" should {
      "redirect to the Enter Client UTR page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

        val result = TestConfirmClientUTRController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show.url)
      }
    }

    lazy val request = fakeRequestWithClientDetails.addingToSession(SessionKeys.confirmedClient -> "false")

    "redirect to Home page and add confirmedClient: true flag to session" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

      val result = TestConfirmClientUTRController.submit()(fakeRequestWithClientDetails)

      verifyExtendedAudit(ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNino, mtditid = testMtditidAgent, arn = testArn, saUtr = testSaUtrId, credId = Some(testCredId)))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      result.futureValue.session(request).get(SessionKeys.confirmedClient) shouldBe Some("true")
    }
  }

}
