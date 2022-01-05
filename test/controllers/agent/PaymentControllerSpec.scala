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

package controllers.agent

import testConstants.BaseTestConstants._
import testConstants.PaymentDataTestConstants._
import audit.mocks.MockAuditingService
import audit.models.InitiatePayNowAuditModel
import connectors.agent.PayApiConnector
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class PaymentControllerSpec extends TestSupport
  with MockIncomeSourceDetailsPredicate
  with MockItvcErrorHandler
  with MockAuthenticationPredicate
  with MockAuditingService {


  class SetupTestPaymentController(response: Future[PaymentJourneyResponse]) {

    val mockPayApiConnector: PayApiConnector = mock[PayApiConnector]

    when(mockPayApiConnector.startPaymentJourney(ArgumentMatchers.eq(testSaUtrId.toString), ArgumentMatchers.eq(BigDecimal(10000)))
    (ArgumentMatchers.any[HeaderCarrier])).thenReturn(response)

    val testController = new PaymentController(
      mockIncomeSourceDetailsService,
      mockPayApiConnector,
      mockAuditingService,
      mockAuthService)(
      appConfig,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      mockItvcErrorHandler)
  }

  "The PaymentController.paymentHandoff action" should {

    "redirect the user to the login page" when {

      "called with an unauthenticated user" in new SetupTestPaymentController(Future.successful(PaymentJourneyModel("id", "redirect-url"))) {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = testController.paymentHandoff(testAmountInPence)()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "redirect the user to the payments page" when {

      "a successful payments journey is started with audit events" in new SetupTestPaymentController(Future.successful(
        PaymentJourneyModel("id", "redirect-url"))) {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = testController.paymentHandoff(testAmountInPence)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("redirect-url")
        verifyExtendedAudit(InitiatePayNowAuditModel(testMtditidAgent, Some(testNinoAgent), Some(testSaUtrId.toString),
          Some(testCredId), Some(testUserTypeAgent)))
      }
    }

    "an SA UTR is missing from the user" in new SetupTestPaymentController(Future.successful(PaymentJourneyModel("id", "redirect-url"))) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockSingleBusinessIncomeSource()

      val result: Future[Result] = testController.paymentHandoff(testAmountInPence)(fakeRequestWithActiveSession.withSession(utils.SessionKeys.clientMTDID -> "XAIT00000000015"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.agent.routes.EnterClientsUTRController.show().url)
    }

    "return an error" when {

      "an error response is returned by the connector" in new SetupTestPaymentController(Future.successful(PaymentJourneyErrorResponse(
        INTERNAL_SERVER_ERROR, "Error Message"))) {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = testController.paymentHandoff(testAmountInPence)(fakeRequestConfirmedClient())

        result.failed.futureValue shouldBe an[Exception]
      }

      "an exception is returned by the connector" in new SetupTestPaymentController(Future.failed(new Exception("Exception Message"))) {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = testController.paymentHandoff(testAmountInPence)(fakeRequestConfirmedClient())

        result.failed.futureValue shouldBe an[Exception]
        result.failed.futureValue.getMessage shouldBe "Exception Message"

      }
    }
  }
}
