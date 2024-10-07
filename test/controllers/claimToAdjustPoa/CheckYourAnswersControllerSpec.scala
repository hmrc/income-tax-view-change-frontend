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
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustPoaCalculationService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PaymentOnAccountViewModel, PoAAmendmentData}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.CheckYourAnswers

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends MockAuthenticationPredicate with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector
  with MockClaimToAdjustPoaCalculationService {

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    authorisedFunctions = mockAuthService,
    auth = testAuthenticator,
    poaSessionService = mockPaymentOnAccountSessionService,
    checkYourAnswers = app.injector.instanceOf[CheckYourAnswers],
    claimToAdjustService = mockClaimToAdjustService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    ctaCalculationService = mockClaimToAdjustPoaCalculationService,
    auditingService = app.injector.instanceOf[AuditingService]
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val poa: Option[PaymentOnAccountViewModel] = Some(
    PaymentOnAccountViewModel(
      poaOneTransactionId = "poaOne-Id",
      poaTwoTransactionId = "poaTwo-Id",
      taxYear = TaxYear.makeTaxYearWithEndYear(2024),
      totalAmountOne = 5000.00,
      totalAmountTwo = 5000.00,
      relevantAmountOne = 5000.00,
      relevantAmountTwo = 5000.00,
      partiallyPaid = false,
      fullyPaid = false,
      previouslyAdjusted = None
    ))

  val emptySession: PoAAmendmentData = PoAAmendmentData(None, None)
  val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val validSessionIncrease: PoAAmendmentData = PoAAmendmentData(Some(Increase), Some(BigDecimal(1000.00)))

  def setupTest(enablePaymentsOnAccountFS: Boolean = true,
                sessionResponse: Either[Throwable, Option[PoAAmendmentData]],
                claimToAdjustResponse: Option[PaymentOnAccountViewModel]
               ): Unit = {
    if (enablePaymentsOnAccountFS) enable(AdjustPaymentsOnAccount) else disable(AdjustPaymentsOnAccount)
    setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    mockSingleBISWithCurrentYearAsMigrationYear()
    setupMockGetPaymentsOnAccount(claimToAdjustResponse)
    setupMockTaxYearCrystallised()
    setupMockPaymentOnAccountSessionService(Future.successful(sessionResponse))
  }

  def setUpFailedMongoTest(): Unit = {
     enable(AdjustPaymentsOnAccount)
    setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    mockSingleBISWithCurrentYearAsMigrationYear()
    setupMockGetPaymentsOnAccount(poa)
    setupMockTaxYearCrystallised()
    setupMockPaymentOnAccountSessionService(Future.failed(new Error("Error getting mongo session")))
  }

  "CheckYourAnswersController.show" should {

    s"return status $SEE_OTHER and redirect to the home page" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        setupTest(
          enablePaymentsOnAccountFS = false,
          sessionResponse = Right(Some(validSession)),
          claimToAdjustResponse = poa
        )

        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return status $SEE_OTHER and redirect to the You Cannot Go Back page" when {
      "FS is enabled and the journeyCompleted flag is set to true in session" in {
        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData(None, None, journeyCompleted = true))),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }

    s"return status: $OK when PoA tax year crystallized" when {
      "the session contains the new POA Amount and reason" in {
        setupTest(
          sessionResponse = Right(Some(validSession)),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
        status(resultAgent) shouldBe OK

        contentAsString(result).contains("Confirm and continue") shouldBe true
      }
      "the session contains the new POA Amount and reason, but the reason is INCREASE" in {
        setupTest(
          sessionResponse = Right(Some(validSessionIncrease)),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
        status(resultAgent) shouldBe OK

        contentAsString(result).contains("Confirm and save") shouldBe true
      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {
      "Payment On Account Session is missing" in {
        setupTest(
          sessionResponse = Right(None),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Payment On Account data is missing from session" in {
        setupTest(
          sessionResponse = Right(Some(emptySession)),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "POA data is missing" in {
        setupTest(
          sessionResponse = Right(Some(validSession)),
          claimToAdjustResponse = None
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "POA adjustment reason is missing from the session" in {
        setupTest(
          sessionResponse = Right(Some(validSession.copy(poaAdjustmentReason = None))),
          claimToAdjustResponse = None
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "the new POA amount is missing from the session" in {
        setupTest(
          sessionResponse = Right(Some(validSession.copy(newPoAAmount = None))),
          claimToAdjustResponse = poa
        )
        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Something goes wrong in payment on account session Service" in {
        setupTest(
          sessionResponse = Left(new Exception("Something went wrong")),
          claimToAdjustResponse = poa
        )

        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Failed future returned when retrieving mongo data" in {
        setUpFailedMongoTest()

        val result = TestCheckYourAnswersController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "CheckYourAnswersController.submit" should {
    "redirect to the home page" when {
      "FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        val result = TestCheckYourAnswersController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.submit(isAgent = true)(fakeRequestConfirmedClient())

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

        val result = TestCheckYourAnswersController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.submit(isAgent = true)(fakeRequestConfirmedClient())

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

        val result = TestCheckYourAnswersController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.submit(isAgent = true)(fakeRequestConfirmedClient())

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

        val result = TestCheckYourAnswersController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestCheckYourAnswersController.submit(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Exception is returned from ClaimToAdjustService" in {
        enable(AdjustPaymentsOnAccount)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockGetPaymentsOnAccountBuildFailure()

        val result = TestCheckYourAnswersController.submit(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = TestCheckYourAnswersController.submit(isAgent = true)(fakeRequestConfirmedClient())

        result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
        resultAgent.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
