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

import audit.AuditingService
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.auth.{MockAuthActions, MockOldAuthActions}
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services._
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{MainIncomeLower, PoaAmendmentData}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.claimToAdjustPoa.ClaimToAdjustPoaCalculationService
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants
import testUtils.TestSupport
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationForAdjustingPoaControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockClaimToAdjustPoaCalculationService {

  val poa: PoaAmendmentData = PoaAmendmentData(
    None,
    Some(20.0)
  )

  val validSession: PoaAmendmentData = PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val emptySession: PoaAmendmentData = PoaAmendmentData(None, None)

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[ClaimToAdjustPoaCalculationService].toInstance(mockClaimToAdjustPoaCalculationService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  val testController = fakeApplication().injector.instanceOf[ConfirmationForAdjustingPoaController]

  "ConfirmationForAdjustingPoaController.show" should {
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "redirect to the You Cannot Go Back page" when {
      "FS is enabled and the journeyCompleted flag is set to true in session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }
    }
    "return an error 500" when {
      "Payment On Account Session is missing" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccount(None)
        setupMockTaxYearNotCrystallised()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }

      "Payment On Account data is missing from session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(emptySession))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetAmendablePoaViewModelFailure()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = TestConfirmationForAdjustingPoaController.show(isAgent = true)(fakeRequestConfirmedClient())
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "ConfirmationForAdjustingPoaController.submit" should {
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "redirect to success page" when {
      "data to API 1773 successfully sent" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockRecalculateSuccess()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent = false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent = true).url)
      }
    }
    "redirect to API error page" when {
      "data to API 1773 failed to be sent" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()
        setupMockRecalculateFailure()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent = false).url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent = true).url)
      }
    }
    "redirect an error 500" when {
      "Payment On Account Session data is missing" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccount(None)
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockGetPaymentsOnAccountBuildFailure()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestConfirmationForAdjustingPoaController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = TestConfirmationForAdjustingPoaController.submit(isAgent = true)(fakeRequestConfirmedClient())
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
