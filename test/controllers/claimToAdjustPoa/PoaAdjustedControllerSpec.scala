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
import mocks.auth.MockAuthActions
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.{MockCalculationListService, MockClaimToAdjustService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants
import testUtils.TestSupport
import views.html.claimToAdjustPoa.PoaAdjustedView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class PoaAdjustedControllerSpec extends TestSupport
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector
  with MockAuthActions {

  val mockPOASessionService = mock(classOf[PaymentOnAccountSessionService])
  val mockDateService: DateService = mock(classOf[DateService])

  object TestController extends PoaAdjustedController(
    authActions = mockAuthActions,
    claimToAdjustService = mockClaimToAdjustService,
    view = app.injector.instanceOf[PoaAdjustedView],
    poaSessionService = mockPOASessionService,
    dateService = mockDateService
  )(
    controllerComponents = app.injector.instanceOf[MessagesControllerComponents],
    individualErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    agentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val startOfTaxYear: LocalDate = LocalDate.of(2023,4,7)
  val endOfTaxYear: LocalDate = LocalDate.of(2024,4,4)

  "PoaAdjustedController.show" should {
    "return Ok" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockDateService.getCurrentDate).thenReturn(startOfTaxYear)
        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPOASessionService.getMongo(any(),any())).thenReturn(Future(Right(Some(PoAAmendmentData(newPoAAmount = Some(1200))))))

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
        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPOASessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoAAmendmentData(None, newPoAAmount = Some(5000), journeyCompleted = true)))))

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

        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
        when(mockPOASessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoAAmendmentData(newPoAAmount = Some(1200))))))

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
        when(mockPOASessionService.setCompletedJourney(any(), any())).thenReturn(Future(Left(new Error(""))))

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
