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
package controllers.adjustPoa

import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.adjustPoa.SelectYourReasonFormProvider
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{OK, defaultAwaitTimeout, status}
import services.PaymentOnAccountSessionService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.adjustPoa.SelectYourReasonView

import scala.concurrent.ExecutionContext

class SelectYourReasonControllerSpec  extends MockAuthenticationPredicate with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector {

  object TestSelectYourReasonController extends SelectYourReasonController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[SelectYourReasonView],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    formProvider = app.injector.instanceOf[SelectYourReasonFormProvider],
    sessionService = mock[PaymentOnAccountSessionService],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "SelectYourReasonController.show" should {
    s"return status: $OK" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestSelectYourReasonController.show(isAgent = false, isChange = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestSelectYourReasonController.show(isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
  }
}