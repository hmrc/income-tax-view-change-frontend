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
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import scala.concurrent.{ExecutionContext, Future}

class WhatYouNeedToKnowControllerSpec extends MockAuthenticationPredicate
  with TestSupport
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector{

  object TestWhatYouNeedToKnowController extends WhatYouNeedToKnowController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    view = app.injector.instanceOf[WhatYouNeedToKnow]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "WhatYouNeedToKnowController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestWhatYouNeedToKnowController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestWhatYouNeedToKnowController.show(isAgent = true)(fakeRequestConfirmedClient())

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

        val result = TestWhatYouNeedToKnowController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestWhatYouNeedToKnowController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return an error 500" when {
      "PaymentOnAccount model is not built successfully" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        val result = TestWhatYouNeedToKnowController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestWhatYouNeedToKnowController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountFailure()

        val result = TestWhatYouNeedToKnowController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestWhatYouNeedToKnowController.show(isAgent = true)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
