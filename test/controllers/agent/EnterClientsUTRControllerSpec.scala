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

import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testAgentDelegatedEnrolment, testAuthSuccessResponse, testAuthSuccessResponseDelegatedAuth, testMtditid, testMtditidAgent, testNino, testSaUtr}
import config.featureswitch.FeatureSwitching
import controllers.agent.utils.SessionKeys
import forms.agent.ClientsUTRForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.MockClientDetailsService
import mocks.views.agent.MockEnterClientsUTR
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify, when}
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.agent.ClientDetailsService._
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AffinityGroup, BearerTokenExpired, ConfidenceLevel, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class EnterClientsUTRControllerSpec extends TestSupport
  with MockEnterClientsUTR
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with MockClientDetailsService
  with FeatureSwitching {

  object TestEnterClientsUTRController extends EnterClientsUTRController(
    enterClientsUTR,
    mockClientDetailsService,
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
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestEnterClientsUTRController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "the user does not have an agent reference number" should {
      "redirect them to the error page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show.url)
      }
    }
    "return Ok and display the page to the user without checking client relationship information" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      mockEnterClientsUTR(HtmlFormat.empty)

      val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
      verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
    }

  }

  "submit" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
      "the user has timed out" should {
        "redirect to the session timeout page" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithTimeoutSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        }
      }
      "the user does not have an agent reference number" should {
        "return Ok with technical difficulties" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
          mockShowOkTechnicalDifficulties()

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession)

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
      "redirect to the confirm client details page" when {
        "the utr entered is valid and there is a client/agent relationship" in {
          val validUTR: String = "1234567890"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          setupMockAgentDelegatedEnrolmentSuccess(Redirect(routes.ConfirmClientUTRController.show))

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))
          println(session(result).get(SessionKeys.clientMTDID))
          println(result)
          println(result.futureValue)
          println(result.futureValue.session)
          whenReady(result) { test => println(test.session) }

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show.url)
          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
          verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
        }
        "the utr entered contains spaces and is valid" in {
          val validUTR: String = "1234567890"
          val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

          //          setupMockAgentAuthRetrievalDelegatedEnrolmentSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          //          setupMockAgentAuthRetrievalDelegatedEnrolmentSuccess(testAgentDelegatedEnrolment, withClientPredicate = false)
          mockClientDetails(validUTR)(
            response = Right(ClientDetails(Some("John"), Some("Doe"), testNino, testMtditid))
          )

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> utrWithSpaces
          ))


          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.ConfirmClientUTRController.show.url)

          result.futureValue.session.get(SessionKeys.clientFirstName) shouldBe Some("John")
          result.futureValue.session.get(SessionKeys.clientLastName) shouldBe Some("Doe")
          result.futureValue.session.get(SessionKeys.clientUTR) shouldBe Some(validUTR)
          result.futureValue.session.get(SessionKeys.clientNino) shouldBe Some(testNino)
          result.futureValue.session.get(SessionKeys.clientMTDID) shouldBe Some(testMtditid)
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT"))
          verify(mockAuthService, times(1)).authorised(Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", testMtditid).withDelegatedAuthRule("mtd-it-auth"))
        }
      }

      "return a bad request" when {
        "the submitted utr is invalid" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          mockEnterClientsUTR(HtmlFormat.empty)

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> "invalid"
          ))

          status(result) shouldBe BAD_REQUEST
          contentType(result) shouldBe Some(HTML)
        }
      }

      "redirect to the UTR Error page" when {
        "a client details not found error is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          mockClientDetails(validUTR)(
            response = Left(CitizenDetailsNotFound)
          )

          val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show.url)
        }
        "redirect to client relationship failure page" when {
          "there is no client/agent relationship" in {
            val validUTR: String = "1234567890"

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

            val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              ClientsUTRForm.utr -> validUTR
            ))


            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show.url)
          }
        }


        "a business details not found error is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          mockClientDetails(validUTR)(
            response = Left(BusinessDetailsNotFound)
          )

          val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show.url)
        }
      }

      "return an exception" when {
        "an unexpected response is returned from the client lookup" in {
          val validUTR: String = "1234567890"

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          mockClientDetails(validUTR)(
            response = Left(UnexpectedResponse)
          )

          val result = TestEnterClientsUTRController.submit(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            ClientsUTRForm.utr -> validUTR
          ))
          result.failed.futureValue shouldBe an[InternalServerException]
          result.failed.futureValue.getMessage shouldBe "[EnterClientsUTRController][submit] - Unexpected response received"
        }
      }
    }
  }
}

