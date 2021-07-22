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

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testMtditid, testNino}
import config.featureswitch.FeatureSwitching
import controllers.agent.utils.SessionKeys
import forms.agent.ClientsUTRForm
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.MockClientDetailsService
import mocks.views.agent.MockEnterClientsUTR
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.agent.ClientDetailsService._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.{BearerTokenExpired, Enrolment}
import uk.gov.hmrc.http.InternalServerException

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
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = TestEnterClientsUTRController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "redirect them to the error page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.AgentErrorController.show().url)
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
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
      "the user has timed out" should {
        "redirect to the session timeout page" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

          val result = TestEnterClientsUTRController.submit()(fakeRequestWithTimeoutSession)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
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
			"redirect to the confirm client details page and add client details to session without checking the relationship" when {
				"the utr entered is valid" in {
					val validUTR: String = "1234567890"

					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
					mockClientDetails(validUTR)(
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
					verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
					verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
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
					redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show().url)
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
					redirectLocation(result) shouldBe Some(controllers.agent.routes.UTRErrorController.show().url)
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

					intercept[InternalServerException](await(result)).message shouldBe "[EnterClientsUTRController][submit] - Unexpected response received"
				}
			}
    }
  }

}

