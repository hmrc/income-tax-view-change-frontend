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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants.testPoa1Maybe
import testUtils.TestSupport
import views.html.claimToAdjustPoa.YouCannotGoBackView

import scala.concurrent.{ExecutionContext, Future}

class YouCannotGoBackControllerSpec extends MockAuthenticationPredicate
  with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService {

  object TestYouCannotGoBackController extends YouCannotGoBackController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = mockClaimToAdjustService,
    poaSessionService = mockPaymentOnAccountSessionService,
    view = app.injector.instanceOf[YouCannotGoBackView],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    auth = testAuthenticator,
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  def setupTest(): Unit = {
    enable(AdjustPaymentsOnAccount)
    setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    mockSingleBISWithCurrentYearAsMigrationYear()
  }

  "YouCannotGoBackController.show" should {
    s"return status $OK" when {
      "AdjustPaymentsOnAccount FS is enabled and journeyComplete is true" in {
        setupTest()

        setupMockGetPaymentsOnAccount(testPoa1Maybe)
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
      "AdjustPaymentsOnAccount FS is enabled and journeyComplete is false" in {
        setupTest()

        setupMockGetPaymentsOnAccount(testPoa1Maybe)
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData()))))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        status(resultAgent) shouldBe OK
      }
    }
    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        setupTest()

        disable(AdjustPaymentsOnAccount)

        setupMockGetPaymentsOnAccount(testPoa1Maybe)
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent().url)
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "No POAs can be found" in {
        setupTest()

        setupMockGetPaymentsOnAccount(None)
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "No active session can be found" in {
        setupTest()

        setupMockGetPaymentsOnAccount(testPoa1Maybe)
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
      "Call to mongo fails" in {
        setupTest()

        setupMockGetPaymentsOnAccount(testPoa1Maybe)
        setupMockPaymentOnAccountSessionService(Future.failed(new Error("")))

        val result = TestYouCannotGoBackController.show(isAgent = false)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestYouCannotGoBackController.show(isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
