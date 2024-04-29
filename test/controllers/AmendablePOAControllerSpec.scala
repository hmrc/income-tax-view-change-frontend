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

package controllers

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockCalculationListService, MockClaimToAdjustService}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.AmendablePaymentOnAccount

import scala.concurrent.{ExecutionContext, Future}

class AmendablePOAControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with TestSupport with FeatureSwitching with MockClaimToAdjustService with MockCalculationListService {

  object TestAmendablePOAController extends AmendablePOAController(
    authorisedFunctions = mockAuthService,
    calculationListService = mockCalculationListService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[AmendablePaymentOnAccount],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "AmendablePOAController.show" should {
    s"return status: ${Status.OK}" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        status(resultAgent) shouldBe Status.OK
      }
    }

    s"return status: ${Status.INTERNAL_SERVER_ERROR}" when {
      "PaymentOnAccount model is not returned successfully" in {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountFailure()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "PaymentOnAccount model is returned successfully but tax year is crystallised" in {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
