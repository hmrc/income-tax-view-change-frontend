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
import mocks.auth.{MockAuthActions, MockOldAuthActions}
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockDateService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoaAmendmentData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, DateService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants
import testUtils.TestSupport
import views.html.claimToAdjustPoa.PoaAdjustedView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class PoaAdjustedControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockPaymentOnAccountSessionService
  with MockDateService {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService),
      api.inject.bind[DateService].toInstance(mockDateService)
    ).build()

  val testController = fakeApplication().injector.instanceOf[PoaAdjustedController]

  val startOfTaxYear: LocalDate = LocalDate.of(2023,4,7)
  val endOfTaxYear: LocalDate = LocalDate.of(2024,4,4)

  "PoaAdjustedController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockDateService.getCurrentDate).thenReturn(startOfTaxYear)
        when(mockPoaSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPoaSessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }
    }
    "not redirect to the You Cannot Go Back page" when {
      "FS is enabled and journeyCompleted flag is set to true" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockDateService.getCurrentDate).thenReturn(endOfTaxYear)
        when(mockPoaSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPoaSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(None, newPoaAmount = Some(5000), journeyCompleted = true)))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }
    }
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockPoaSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPoaSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return an error 500" when {
      "Error setting journey completed flag in mongo session" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        when(mockPoaSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Left(new Error(""))))

        setupMockGetPaymentsOnAccount()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "PaymentOnAccount model is not built successfully" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetAmendablePoaViewModelFailure()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = TestController.show(isAgent = true)(fakeRequestConfirmedClient())
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
