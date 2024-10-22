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
import controllers.agent.sessionUtils
import mocks.auth.MockOldAuthActions
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PaymentOnAccountViewModel, PoaAmendmentData}
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.EnterPoaAmountView

import scala.concurrent.{ExecutionContext, Future}

class EnterPoaAmountControllerSpec extends TestSupport
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector
  with MockPaymentOnAccountSessionService
  with MockOldAuthActions {

  object TestEnterPoaAmountController$ extends EnterPoaAmountController(
    authActions = mockAuthActions,
    claimToAdjustService = mockClaimToAdjustService,
    view = app.injector.instanceOf[EnterPoaAmountView],
    poaSessionService = mockPaymentOnAccountSessionService
  )(
    controllerComponents = app.injector.instanceOf[MessagesControllerComponents],
    individualErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    agentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val poaViewModelDecreaseJourney = PaymentOnAccountViewModel(
    previouslyAdjusted = None,
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000,
    totalAmountTwo = 5000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000,
    partiallyPaid = false,
    fullyPaid = false
  )

  val poaViewModelIncreaseJourney = PaymentOnAccountViewModel( //Increase OR Decrease journey
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    previouslyAdjusted = None,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 4000,
    totalAmountTwo = 4000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000,
    partiallyPaid = false,
    fullyPaid = false
  )

  def getPostRequest(isAgent: Boolean, mode: Mode, poaAmount: String) = {
    if (isAgent) {
      FakeRequest(POST, routes.EnterPoaAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession(
          sessionUtils.SessionKeys.clientFirstName -> "Test",
          sessionUtils.SessionKeys.clientLastName -> "User",
          sessionUtils.SessionKeys.clientUTR -> "1234567890",
          sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
          sessionUtils.SessionKeys.clientNino -> "AA111111A",
          sessionUtils.SessionKeys.confirmedClient -> "true"
        )
    }
    else {
      FakeRequest(POST, routes.EnterPoaAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")
    }
  }

  "EnterPoaAmountController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized does not exist in session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower))))))
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK
        Jsoup.parse(contentAsString(result)).select("#poa-amount").attr("value") shouldBe ""

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
        Jsoup.parse(contentAsString(resultAgent)).select("#poa-amount").attr("value") shouldBe ""
      }
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized and newPoaAmount exists in session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1111.22)))))))
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK
        Jsoup.parse(contentAsString(result)).select("#poa-amount").attr("value") shouldBe "1111.22"

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
        Jsoup.parse(contentAsString(resultAgent)).select("#poa-amount").attr("value") shouldBe "1111.22"
      }
    }
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "redirect to the You Cannot Go Back page" when {
      "FS is enabled and the journeyCompleted flag is set to true in session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), None, journeyCompleted = true)))))
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }
    "return an error 500" when {
      "Error retrieving mongo session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Left(new Error(""))))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Retrieving mongo session fails" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future.failed(new Error("")))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "User does not have an active mongo session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(None)))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData()))))
        setupMockGetPoaAmountViewModelFailure()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.show(isAgent = false, NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = TestEnterPoaAmountController$.show(isAgent = true, NormalMode)(fakeRequestConfirmedClient())
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "EnterPoaAmountController.submit" should {
    "redirect to the check your answers page" when {
      "The user is on the decrease only journey and form returned with no errors" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelDecreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "1234.56"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

        val resultChange = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "1234.56"))
        status(resultChange) shouldBe SEE_OTHER
        redirectLocation(resultChange) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "1234.56"))
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
      }

      "The user is on the increase/decrease journey and chooses to increase" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
        when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "4500"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "4500"))
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
      }

      "CHANGE Only: The user is on the increase/decrease journey and has come from CYA" when {
        "They had previously decreased, and are now increasing" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBISWithCurrentYearAsMigrationYear()
          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
          when(mockPaymentOnAccountSessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100))))))
          when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          val result = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "4500"))
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
          val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "4500"))
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }

        "They had previously decreased, and are still decreasing" in {
          enable(AdjustPaymentsOnAccount)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          mockSingleBISWithCurrentYearAsMigrationYear()

          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPaymentOnAccountSessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100))))))
          when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          val result = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "1000"))
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
          val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "1000"))
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }

        "They had previously increased, and are still increasing" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBISWithCurrentYearAsMigrationYear()
          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

          when(mockPaymentOnAccountSessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(4600))))))
          when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          val result = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "4500"))
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(false).url)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
          val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "4500"))
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(true).url)
        }
      }
    }
    "redirect to the Select Reason page" when {
      "The user is on the increase/decrease journey and chooses to decrease" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "1234.56"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(false, NormalMode).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "1234.56"))
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(true, NormalMode).url)
      }

      "CHANGE Only: The user is on the increase/decrease journey and has come from CYA" when {
        "They had previously increased, and are now decreasing" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBISWithCurrentYearAsMigrationYear()
          setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
          when(mockPaymentOnAccountSessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(Increase), Some(4500))))))
          when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
          when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          val result = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "500"))
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(false, CheckMode).url)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
          val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "500"))
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(true, CheckMode).url)
        }
      }
    }
    "redirect back to the Enter PoA Amount page with a 500 response" when {
      "No PoA Amount is input" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, ""))
        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, ""))
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is not a valid number" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "test"))
        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "invalid"))
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is higher than relevant amount" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "6000"))
        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "6000"))
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }
      "Input PoA Amount is equal to previous PoA amount" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "4000"))
        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "4000"))
        status(resultAgent) shouldBe BAD_REQUEST
        redirectLocation(resultAgent) shouldBe None
      }

      "Error setting new poa amount in mongo" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelDecreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Left(new Error("Error setting poa amount"))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "1234.56"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "1234.56"))
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(resultAgent) shouldBe None
      }
      "Error setting adjustment reason in mongo" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
        when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Left(new Error("Error setting adjustment reason"))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, NormalMode)(getPostRequest(isAgent = false, NormalMode, "4500"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, NormalMode)(getPostRequest(isAgent = true, NormalMode, "4500"))
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(resultAgent) shouldBe None
      }
      "Error getting adjustment reason from mongo" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

        when(mockPaymentOnAccountSessionService.getMongo(any(),any())).thenReturn(Future(Left(new Error("Error getting mongo data"))))
        when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(),any())).thenReturn(Future(Right(())))
        when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(),any())).thenReturn(Future(Right(())))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestEnterPoaAmountController$.submit(isAgent = false, CheckMode)(getPostRequest(isAgent = false, CheckMode, "1000"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestEnterPoaAmountController$.submit(isAgent = true, CheckMode)(getPostRequest(isAgent = true, CheckMode, "1000"))
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
        redirectLocation(resultAgent) shouldBe None
      }
    }
  }
}
