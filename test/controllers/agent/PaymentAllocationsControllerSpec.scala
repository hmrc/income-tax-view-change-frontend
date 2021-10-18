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

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testNinoAgent}
import assets.PaymentAllocationsTestConstants._
import audit.mocks.MockAuditingService
import config.featureswitch.{FeatureSwitching, PaymentAllocation}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockIncomeSourceDetailsService, MockPaymentAllocationsService}
import mocks.views.agent.MockPaymentAllocationView
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.PaymentAllocationsService
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments}
import org.mockito.Mockito.when
import models.core.Nino
import org.mockito.ArgumentMatchers.any
import scala.concurrent.Future

class PaymentAllocationsControllerSpec extends TestSupport with MockPaymentAllocationView with MockFrontendAuthorisedFunctions
  with FeatureSwitching with MockPaymentAllocationsService with MockItvcErrorHandler with MockAuditingService with MockIncomeSourceDetailsService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PaymentAllocation)
  }

  class Setup {
    val docNumber = "docNumber1"

    val paymentAllocation: PaymentAllocationsService = mock[PaymentAllocationsService]


    val controller: PaymentAllocationsController = new PaymentAllocationsController(
      paymentAllocationView = paymentAllocationView,
      paymentAllocationsService = mockPaymentAllocationsService,
      authorisedFunctions = mockAuthService,
      mockAuditingService,
      mockIncomeSourceDetailsService
    )(
      appConfig,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      mockItvcErrorHandler
    )
  }

  "The PaymentAllocationsController.viewPaymentAllocation function" when {

    "the user is not authenticated" should {
      "redirect the user to authenticate" in new Setup {
        setupMockAgentAuthorisationException()

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "there are no client details in session" should {
      "redirect to the Enter Client UTR page" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
      }
    }

    "the auth check fails to find a valid agent-client relationship" should {
      "redirect to the Agent Client Relationship error page" in new Setup {
        setupMockAgentAuthorisationException(InsufficientEnrolments())

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show().url)
      }
    }

    "the PaymentAllocation feature switch is disabled" should {
      "return Not Found" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe NOT_FOUND
      }
    }

    "the PaymentAllocation feature switch is enabled" should {
      "Successfully retrieve a user's payment allocation and display the page" in new Setup {
        enable(PaymentAllocation)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        setupMockGetPaymentAllocationSuccess(testNinoAgent, docNumber)(paymentAllocationViewModel)

        mockPaymentAllocationView(
          paymentAllocationViewModel,
          controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url
        )(HtmlFormat.empty)

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
      }

      "Successfully retrieving a user's lpi payment allocation" in new Setup {
        enable(PaymentAllocation)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        setupMockGetPaymentAllocationSuccess(testNinoAgent, docNumber)(paymentAllocationViewModelLpi)

        mockPaymentAllocationView(
          paymentAllocationViewModelLpi,
          controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url
        )(HtmlFormat.empty)

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
      }

      "Fail to retrieve a user's payment allocation and return a 500" in new Setup {
        enable(PaymentAllocation)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBusinessIncomeSource()
        setupMockGetPaymentAllocationError(testNinoAgent, docNumber)
        mockShowInternalServerError()

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

}
