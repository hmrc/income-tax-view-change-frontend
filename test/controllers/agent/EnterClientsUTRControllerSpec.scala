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

import audit.models.EnterClientUTRAuditModel
import controllers.agent.sessionUtils.SessionKeys
import forms.agent.ClientsUTRForm
import mocks.auth.MockAuthActions
import mocks.services.MockClientDetailsService
import mocks.views.agent.MockEnterClientsUTR
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import play.api
import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.agent.ClientDetailsService._
import testConstants.BaseTestConstants.{agentAuthRetrievalSuccess, testArn, testCredId, testMtditid, testNino}
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import views.html.agent.EnterClientsUTR

class EnterClientsUTRControllerSpec extends MockAuthActions
  with MockEnterClientsUTR
  with MockClientDetailsService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[EnterClientsUTR].toInstance(enterClientsUTR)
    ).build()

  lazy val testEnterClientsUTRController = app.injector.instanceOf[EnterClientsUTRController]

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthException()

        val result = testEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        val result = testEnterClientsUTRController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "the user does not have an agent reference number" should {
      "redirect them to the error page" in {
        setupMockAgentAuthException(InsufficientEnrolments())

        val result = testEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
      }
    }

    "return Ok and display the page to the user without checking client relationship information" in {
      setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
      mockEnterClientsUTR(HtmlFormat.empty)

      val result = testEnterClientsUTRController.show()(fakeRequestWithActiveSession)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(authPredicateForAgent))
    }

  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthException()

        val result = testEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
      "the user has timed out" should {
        "redirect to the session timeout page" in {
          val result = testEnterClientsUTRController.submit()(fakeRequestWithTimeoutSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        }
      }
      "the user does not have an agent reference number" should {
        "return Ok with technical difficulties" in {
          setupMockAgentAuthException(InsufficientEnrolments())

          val result = testEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
        }
      }
      "redirect to the confirm client details page" when {
        "the utr entered is valid, there is a client/primary agent relationship and the POST request to session data service is successful" in {
          val validUTR: String = "1234567890"
          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockPrimaryAgentAuthRetrievalSuccess(testMtditid)

          val result = testEnterClientsUTRController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          verifyExtendedAudit(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtditid, arn = Some(testArn), saUtr = validUTR, credId = Some(testCredId), Some(false)))
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show().url)
          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(authPredicateForAgent))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth"))
        }

        "the utr entered is valid, there is a client/secondary agent relationship and the POST request to session data service is successful" in {
          val validUTR: String = "1234567890"
          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockNoPrimaryDelegatedEnrolmentForMTDItId(testMtditid)

          setupMockSecondaryAgentAuthRetrievalSuccess(testMtditid)

          val result = testEnterClientsUTRController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          verifyExtendedAudit(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtditid, arn = Some(testArn), saUtr = validUTR, credId = Some(testCredId), Some(true)))
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show().url)
          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(authPredicateForAgent))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth"))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT-SUPP").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth-supp"))
        }
        "the utr contains has spaces, there is a client/primary agent relationship, is valid and  the POST request to session data service is successful" in {
          val validUTR: String = "1234567890"
          val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockPrimaryAgentAuthRetrievalSuccess(testMtditid)

          val result = testEnterClientsUTRController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> utrWithSpaces
          ))


          status(result) shouldBe SEE_OTHER
          verifyExtendedAudit(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtditid, arn = Some(testArn), saUtr = validUTR, credId = Some(testCredId), Some(false)))
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show().url)

          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(authPredicateForAgent))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth"))
        }
        "the utr entered has spaces, there is a client/secondary agent relationship, is valid and the POST request to session data service is successful" in {
          val validUTR: String = "1234567890"
          val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockNoPrimaryDelegatedEnrolmentForMTDItId(testMtditid)

          setupMockSecondaryAgentAuthRetrievalSuccess(testMtditid)

          val result = testEnterClientsUTRController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> utrWithSpaces
          ))


          status(result) shouldBe SEE_OTHER
          verifyExtendedAudit(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtditid, arn = Some(testArn), saUtr = validUTR, credId = Some(testCredId), Some(true)))
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show().url)

          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(authPredicateForAgent))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth"))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT-SUPP").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth-supp"))

        }
      }

      "return a bad request" when {
        "the submitted utr is invalid" in {
          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
          mockEnterClientsUTR(HtmlFormat.empty)

          val result = testEnterClientsUTRController.submit()(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> "invalid"
          ))

          status(result) shouldBe BAD_REQUEST
          contentType(result) shouldBe Some(HTML)
        }
      }

      "redirect to the UTR Error page" when {
        "a client details not found error is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
          mockClientDetails(validUTR)(
            response = Left(CitizenDetailsNotFound)
          )

          val result = testEnterClientsUTRController.submit(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show().url)
        }

        "a business details not found error is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
          mockClientDetails(validUTR)(
            response = Left(BusinessDetailsNotFound)
          )

          val result = testEnterClientsUTRController.submit(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show().url)
        }

        "client details exist but there is no agent/client relationship" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockNoPrimaryDelegatedEnrolmentForMTDItId(testMtditid)
          setupMockNoSecondaryDelegatedEnrolmentForMTDItId(testMtditid)

          val result = testEnterClientsUTRController.submit(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          verifyExtendedAudit(EnterClientUTRAuditModel(isSuccessful = false, nino = testNino, mtditid = testMtditid, arn = Some(testArn), saUtr = validUTR, credId = Some(testCredId), None))
          redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show().url)
        }
      }

      "return an exception" when {
        "an unexpected response is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthSuccess(agentAuthRetrievalSuccess)
          mockClientDetails(validUTR)(
            response = Left(APIError)
          )

          val result = testEnterClientsUTRController.submit(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}

