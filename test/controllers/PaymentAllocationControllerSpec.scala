/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers


import audit.mocks.MockAuditingService
import config.featureswitch.{FeatureSwitching, PaymentAllocation, CutOverCredits}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockPaymentAllocationsService}
import mocks.views.agent.MockPaymentAllocationView
import models.core.Nino
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, PaymentAllocationError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testNinoAgent}
import testConstants.PaymentAllocationsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments}
import views.html.PaymentAllocation

import scala.concurrent.Future

class PaymentAllocationControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler with MockPaymentAllocationsService with MockPaymentAllocationView
  with MockIncomeSourceDetailsService with MockAuditingService with TestSupport {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  trait Setup {
    val docNumber = "docNumber1"

    val controller = new PaymentAllocationsController(
      app.injector.instanceOf[PaymentAllocation],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      mockPaymentAllocationsService,
      app.injector.instanceOf[NavBarPredicate],
      mockAuditingService
    )(app.injector.instanceOf[MessagesControllerComponents],
      ec,
      appConfig)
  }

  "The PaymentAllocationsControllerSpec.viewPaymentAllocation function" should {
    val successfulResponse = Right(paymentAllocationViewModel)

    "behave appropriately when the feature switch is on" when {
      "Successfully retrieving a user's payment allocation" in new Setup {
        enable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
          .thenReturn(Future.successful(successfulResponse))

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
      }

      "Successfully retrieving a user's lpi payment allocation" in new Setup {
        enable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
          .thenReturn(Future.successful(Right(paymentAllocationViewModelLpi)))

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
      }

      "Failing to retrieve a user's payment allocation" in new Setup {
        enable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
          .thenReturn(Future.successful(Left(PaymentAllocationError())))

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "behave appropriately when the feature switch is off" when {
      "trying to access payments allocation" in new Setup {
        disable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
          .thenReturn(Future.successful(successfulResponse))

        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        mockErrorIncomeSource()
        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" should {
      "redirect the user to the login page" in new Setup {
        setupMockAuthorisationException()
        val result = controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }

  "The PaymentAllocationsController.viewPaymentAllocationAgent function" when {

    "the user is not authenticated" should {
      "redirect the user to authenticate" in new Setup {
        setupMockAgentAuthorisationException()

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired(), withClientPredicate = false)

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment)
        mockShowOkTechnicalDifficulties()

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "there are no client details in session" should {
      "redirect to the Enter Client UTR page" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
      }
    }

    "the auth check fails to find a valid agent-client relationship" should {
      "redirect to the Agent Client Relationship error page" in new Setup {
        setupMockAgentAuthorisationException(InsufficientEnrolments())

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show().url)
      }
    }

    "the PaymentAllocation feature switch is disabled" should {
      "return Not Found" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
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
          controllers.routes.PaymentHistoryController.showAgent().url, saUtr= None, CutOverCreditsEnabled = false, isAgent = true
        )(HtmlFormat.empty)

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
      }

      "Successfully retrieving a user's lpi payment allocation" in new Setup {
        enable(PaymentAllocation)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        setupMockGetPaymentAllocationSuccess(testNinoAgent, docNumber)(paymentAllocationViewModelLpi)

        mockPaymentAllocationView(
          paymentAllocationViewModelLpi,
          controllers.routes.PaymentHistoryController.showAgent().url, saUtr= None, CutOverCreditsEnabled = false, isAgent = true
        )(HtmlFormat.empty)

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
      }

      "Fail to retrieve a user's payment allocation and return a 500" in new Setup {
        enable(PaymentAllocation)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBusinessIncomeSource()
        setupMockGetPaymentAllocationError(testNinoAgent, docNumber)
        mockShowInternalServerError()

        val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "PaymentAllocation FS enabled and CutOverCredit FS disabled" should {
        "Successfully retrieve a user's payment allocation with credit and redirect to not found page" in new Setup {
          enable(PaymentAllocation)
          disable(CutOverCredits)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockSingleBusinessIncomeSource()
          setupMockGetPaymentAllocationSuccess(testNinoAgent, docNumber)(paymentAllocationViewModelNoPayment)

          mockPaymentAllocationView(
            paymentAllocationViewModelNoPayment,
            controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url, saUtr = None,
            CutOverCreditsEnabled = false, isAgent = true
          )(HtmlFormat.empty)

          val result = controller.viewPaymentAllocationAgent(documentNumber = docNumber)(fakeRequestConfirmedClient())
          status(result) shouldBe SEE_OTHER
        }
      }
    }
  }
}
