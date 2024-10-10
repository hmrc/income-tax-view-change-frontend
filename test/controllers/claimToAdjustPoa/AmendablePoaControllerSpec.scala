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
import mocks.auth.MockAuthActions
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testUtils.TestSupport
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import scala.concurrent.{ExecutionContext, Future}

class AmendablePoaControllerSpec
  extends TestSupport
    with MockClaimToAdjustService
    with MockCalculationListService
    with MockCalculationListConnector
    with MockFinancialDetailsConnector
    with MockPaymentOnAccountSessionService
    with MockAuthActions {

  val getMongoResponseJourneyIncomplete: Option[PoAAmendmentData] = Some(PoAAmendmentData())
  val getMongoResponseJourneyComplete: Option[PoAAmendmentData] = Some(PoAAmendmentData(None, None, journeyCompleted = true))

  object TestAmendablePoaController$ extends AmendablePoaController(
    authActions = mockAuthActions,
    claimToAdjustService = mockClaimToAdjustService,
    view = app.injector.instanceOf[AmendablePaymentOnAccount],
    poaSessionService = mockPaymentOnAccountSessionService
  )(
    controllerComponents = app.injector.instanceOf[MessagesControllerComponents],
    individualErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    agentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "AmendablePOAController.show" should {
    s"return status: $OK" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized and no active session" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(None)))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestAmendablePoaController$.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }

      "PoA data is all fine, and we have an active session but is journey completed flag is false" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyComplete)))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestAmendablePoaController$.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }

      "PoA data is all fine, and we have an active session but is journey completed flag is true" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyIncomplete)))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe OK

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestAmendablePoaController$.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe OK
      }
    }
    s"return status: $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestAmendablePoaController$.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)

      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoAAmendmentData()))))
        setupMockGetAmendablePoaViewModelFailure()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      "Error creating mongo session" in {
        enable(AdjustPaymentsOnAccount)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Left(new Error("Error"))))
        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestAmendablePoaController$.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR

        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent = TestAmendablePoaController$.show(isAgent = true)(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR

        verifyMockCreateSession(2)
      }
    }
  }
}
