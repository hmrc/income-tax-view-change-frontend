/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.claimToAdjustPoa

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.PaymentOnAccountSessionService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.PaymentsOnAccountAdjustedView

import scala.concurrent.{ExecutionContext, Future}

class PaymentsOnAccountAdjustedControllerSpec extends MockAuthenticationPredicate
  with TestSupport
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector {

  val mockPOASessionService = mock(classOf[PaymentOnAccountSessionService])

  object TestController extends PaymentsOnAccountAdjustedController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    view = app.injector.instanceOf[PaymentsOnAccountAdjustedView],
    sessionService = mockPOASessionService
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "PaymentsOnAccountAdjustedController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(newPoAAmount = Some(1200))))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return an error 500" when {
      "Error setting journey completed flag in mongo session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Left(new Error(""))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "PaymentOnAccount model is not built successfully" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountFailure()

        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestController.show(isAgent = true)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
