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
import mocks.services._
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{MainIncomeLower, PoAAmendmentData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
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
  with MockPaymentOnAccountSessionService
  with MockClaimToAdjustPoaCalculationService{

  val poa: PoAAmendmentData = PoAAmendmentData(
    None,
    Some(20.0)
  )

  val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val emptySession: PoAAmendmentData = PoAAmendmentData(None, None)

  object TestConfirmationForAdjustingPoaController extends ConfirmationForAdjustingPoaController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = mockClaimToAdjustService,
    auth = testAuthenticator,
    ctaCalculationService = mockClaimToAdjustPoaCalculationService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    view = app.injector.instanceOf[ConfirmationForAdjustingPoa],
    poaSessionService = mockPaymentOnAccountSessionService,

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
    "redirect to the You Cannot Go Back page" when {
      "FS is enabled and the journeyCompleted flag is set to true in session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
    "return an error 500" when {
      "Payment On Account Session is missing" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount(None)
        setupMockTaxYearNotCrystallised()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Payment On Account data is missing from session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(emptySession))))

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetAmendablePoaViewModelFailure()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "ConfirmationForAdjustingPoaController.submit" should {
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "redirect to success page" when {
      "data to API 1773 successfully sent" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockRecalculateSuccess()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())

        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent = false).url)
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent = true).url)
      }
    }
    "redirect to API error page" when {
      "data to API 1773 failed to be sent" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockRecalculateFailure()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())

        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent = false).url)
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent = true).url)
      }
    }
    "redirect an error 500" when {
      "Payment On Account Session data is missing" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccount(None)
        setupMockTaxYearNotCrystallised()

        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
