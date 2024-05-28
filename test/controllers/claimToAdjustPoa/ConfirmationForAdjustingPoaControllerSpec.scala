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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService, MockSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{MainIncomeLower, PoAAmendmentData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, validateMockitoUsage, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustPoaCalculationService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationForAdjustingPoaControllerSpec extends MockAuthenticationPredicate
  with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector
  with MockSessionService
  with MockPaymentOnAccountSessionService{

  val mockPOASessionService = mock(classOf[PaymentOnAccountSessionService])
  val mockPoaCalculationService = mock(classOf[ClaimToAdjustPoaCalculationService])

  val poa: PoAAmendmentData = PoAAmendmentData(
    None,
    Some(20.0)
  )

  val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val sessionResponse: Either[Throwable, Option[PoAAmendmentData]] = Right(Some(validSession))

  object TestConfirmationForAdjustingPoaController extends ConfirmationForAdjustingPoaController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    calculationService = mockPoaCalculationService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    view = app.injector.instanceOf[ConfirmationForAdjustingPoa],
    sessionService = mockPOASessionService,

  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "ConfirmationForAdjustingPoaController.show" should {
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.createSession(any(), any())).thenReturn(Future(Right(())))
        setupMockPaymentOnAccountSessionService(Future.successful(sessionResponse))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
  }

}
