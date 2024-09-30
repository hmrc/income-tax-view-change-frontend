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
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import scala.concurrent.{ExecutionContext, Future}

class AmendablePOAControllerSpec
  extends MockAuthenticationPredicate
    with TestSupport
    with MockClaimToAdjustService
    with MockCalculationListService
    with MockCalculationListConnector
    with MockFinancialDetailsConnector
    with MockPaymentOnAccountSessionService {

  val getMongoResponseJourneyIncomplete: Option[PoAAmendmentData] = Some(PoAAmendmentData())
  val getMongoResponseJourneyComplete: Option[PoAAmendmentData] = Some(PoAAmendmentData(None, None, journeyCompleted = true))

  object TestAmendablePOAController extends AmendablePOAController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = mockClaimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[AmendablePaymentOnAccount],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    poaSessionService = mockPaymentOnAccountSessionService
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  "AmendablePOAController.show" should {
    s"return status: $OK" when {
      "PaymentOnAccount model is returned successfully with PoA tax year crystallized and no active session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future(Right(None)))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))

        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
      "PoA data is all fine, and we have an active session but is journey completed flag is false" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyComplete)))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))

        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK

      }
      "PoA data is all fine, and we have an active session but is journey completed flag is true" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyIncomplete)))

        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
    s"return status: $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent().url)
      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future(Right(Some(PoAAmendmentData()))))
        setupMockGetAmendablePoaViewModelFailure()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
      "Error creating mongo session" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Left(new Error("Error"))))

        setupMockGetPaymentOnAccountViewModel()
        setupMockTaxYearNotCrystallised()

        val result = TestAmendablePOAController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestAmendablePOAController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
        verifyMockCreateSession(2)
      }
    }
  }
}
