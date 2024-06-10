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

package controllers.claimToAdjustPoa

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.ChargeHistoryConnector
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService}
import models.admin.AdjustPaymentsOnAccount
import org.mockito.Mockito._
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import scala.concurrent.{ExecutionContext, Future}

class AmendablePOAControllerSpec
  extends MockAuthenticationPredicate
    with TestSupport
    with MockClaimToAdjustService
    with MockCalculationListService
    with MockCalculationListConnector
    with MockFinancialDetailsConnector {

  object TestAmendablePOAController extends AmendablePOAController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[AmendablePaymentOnAccount],
    chargeHistoryConnector = mock(classOf[ChargeHistoryConnector]),
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "AmendablePOAController.show" should {
    s"return status: $OK" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
    s"return status: $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {
      "PaymentOnAccount model is not built successfully" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountFailure()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
