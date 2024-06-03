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
import controllers.agent.utils
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoAAmendmentData, PoAAmountViewModel}
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, defaultAwaitTimeout, redirectLocation, status}
import services.PaymentOnAccountSessionService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.EnterPoAAmountView

import scala.concurrent.{ExecutionContext, Future}

class EnterPoAAmountControllerSpec extends MockAuthenticationPredicate
  with TestSupport
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector {

  val mockPOASessionService = mock(classOf[PaymentOnAccountSessionService])

  object TestEnterPoAAmountController extends EnterPoAAmountController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    view = app.injector.instanceOf[EnterPoAAmountView],
    sessionService = mockPOASessionService
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val poaViewModelDecreaseJourney = PoAAmountViewModel(
    poaPreviouslyAdjusted = false,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000,
    totalAmountTwo = 5000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000
  )

  val poaViewModelIncreaseJourney = PoAAmountViewModel( //Increase OR Decrease journey
    poaPreviouslyAdjusted = false,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 4000,
    totalAmountTwo = 4000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000
  )

  def getPostRequest(isAgent: Boolean, mode: Mode, poaAmount: String) = {
    if (isAgent) {
      FakeRequest(POST, routes.EnterPoAAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession(
          utils.SessionKeys.clientFirstName -> "Test",
          utils.SessionKeys.clientLastName -> "User",
          utils.SessionKeys.clientUTR -> "1234567890",
          utils.SessionKeys.clientMTDID -> "XAIT00000000015",
          utils.SessionKeys.clientNino -> "AA111111A",
          utils.SessionKeys.confirmedClient -> "true"
        )
    }
    else {
      FakeRequest(POST, routes.EnterPoAAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")
    }
  }

  "EnterPoAAmountController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoAAmendmentData(Some(MainIncomeLower))))))

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        setupMockTaxYearNotCrystallised()

        val result = TestEnterPoAAmountController.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestEnterPoAAmountController.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())

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

        val result = TestEnterPoAAmountController.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestEnterPoAAmountController.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return an error 500" when {
      "Error retrieving mongo session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.getMongo(any(), any())).thenReturn(Future(Left(new Error(""))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestEnterPoAAmountController.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestEnterPoAAmountController.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "User does not have an active mongo session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPOASessionService.getMongo(any(), any())).thenReturn(Future(Right(None)))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestEnterPoAAmountController.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestEnterPoAAmountController.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModelFailure()

        val result = TestEnterPoAAmountController.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestEnterPoAAmountController.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "EnterPoAAmountController.submit" should {
    "redirect to the check your answers page" when {
      "The user is on the decrease only journey and form returned with no errors" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelDecreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "3000"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "3000"))
        val resultChange = TestEnterPoAAmountController.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "3000"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        status(resultChange) shouldBe SEE_OTHER
        redirectLocation(resultChange) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
      }
      "The user is on the increase/decrease journey and chooses to increase" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))
        when(mockPOASessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "4500"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "4500"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
      }
      "CHANGE Only: The user is on the increase/decrease journey and has come from CYA" when {
        "They had previously decreased, and are now increasing" in {
          enable(AdjustPaymentsOnAccount)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBISWithCurrentYearAsMigrationYear()

          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(100))))))
          when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPOASessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          val result = TestEnterPoAAmountController.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "4500"))
          val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "4500"))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }
        "They had previously decreased, and are still decreasing" in {
          enable(AdjustPaymentsOnAccount)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBISWithCurrentYearAsMigrationYear()

          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(100))))))
          when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPOASessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          val result = TestEnterPoAAmountController.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "1000"))
          val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "1000"))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }
        "They had previously increased, and are still increasing" in {
          enable(AdjustPaymentsOnAccount)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBISWithCurrentYearAsMigrationYear()

          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(4600))))))
          when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPOASessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          val result = TestEnterPoAAmountController.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "4500"))
          val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "4500"))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }
      }
    }
    "redirect to the Select Reason page" when {
      "The user is on the increase/decrease journey and chooses to decrease" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "2000"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "2000"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(false, NormalMode).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(true, NormalMode).url)
      }
      "CHANGE Only: The user is on the increase/decrease journey and has come from CYA" when {
        "They had previously increased, and are now decreasing" in {
          enable(AdjustPaymentsOnAccount)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBISWithCurrentYearAsMigrationYear()

          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(Some(Increase), Some(4500))))))
          when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPOASessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          val result = TestEnterPoAAmountController.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "500"))
          val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "500"))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(false, CheckMode).url)
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(true, CheckMode).url)
        }
      }
    }
    "redirect back to the Enter PoA Amount page with a 500 response" when {
      "No PoA Amount is input" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, ""))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, ""))

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is not a valid number" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "test"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "invalid"))

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is higher than relevant amount" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "6000"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "6000"))

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is equal to previous PoA amount" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPOASessionService.setNewPoAAmount(any())(any(),any())).thenReturn(Future(Right(())))

        val result = TestEnterPoAAmountController.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "4000"))
        val resultAgent = TestEnterPoAAmountController.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "4000"))

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
    }
  }

}
