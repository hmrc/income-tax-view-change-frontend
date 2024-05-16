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

import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.adjustPoa.SelectYourReasonFormProvider
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PaymentOnAccountViewModel, PoAAmendmentData}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, POST, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.claimToAdjustPoa.SelectYourReasonView

import scala.concurrent.{ExecutionContext, Future}

class SelectYourReasonControllerSpec  extends MockAuthenticationPredicate with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockCalculationListService
  with MockCalculationListConnector
  with MockFinancialDetailsConnector {

  object TestSelectYourReasonController extends SelectYourReasonController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = claimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[SelectYourReasonView],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    formProvider = app.injector.instanceOf[SelectYourReasonFormProvider],
    sessionService = mockPaymentOnAccountSessionService,
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  val poa: Option[PaymentOnAccountViewModel] = Some(
    PaymentOnAccountViewModel(
      poaOneTransactionId = "poaOne-Id",
      poaTwoTransactionId = "poaTwo-Id",
      taxYear = TaxYear.makeTaxYearWithEndYear(2024),
      paymentOnAccountOne = 5000.00,
      paymentOnAccountTwo = 5000.00,
      poARelevantAmountOne = 5000.00,
      poARelevantAmountTwo = 5000.00
    ))

  val poaTotalLessThanRelevant: Option[PaymentOnAccountViewModel] = poa.map(_.copy(
    paymentOnAccountOne = 1000.0,
    paymentOnAccountTwo = 1000.0))

  def setupTest(sessionResponse: Either[Throwable, Option[PoAAmendmentData]], claimToAdjustResponse: Option[PaymentOnAccountViewModel]): Unit = {
    enable(AdjustPaymentsOnAccount)
    setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    mockSingleBISWithCurrentYearAsMigrationYear()
    setupMockGetPaymentsOnAccount(claimToAdjustResponse)
    setupMockTaxYearCrystallised()
    setupMockPaymentOnAccountSessionService(Future.successful(sessionResponse))
  }

  "SelectYourReasonController.show" should {

    s"return status $SEE_OTHER and set Reason when new amount is greater than current amount" in {
      setupTest(
        sessionResponse = Right(Some(PoAAmendmentData(newPoAAmount = Some(20000.0)))),
        claimToAdjustResponse = poa)

      setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

      val result = TestSelectYourReasonController.show(isAgent = false, isChange = true)(fakeRequestWithNinoAndOrigin("PTA"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
    }

    s"return status: $OK when PoA tax year crystallized" when {

      "in change mode must pre-populate the answer" when {
        "user is agent" in {
          // TODO https://jira.tools.tax.service.gov.uk/browse/MISUV-7489
        }

        "user is not agent" in {
          // TODO https://jira.tools.tax.service.gov.uk/browse/MISUV-7489
        }
      }

      s"in normal mode" when {
        "user is agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = poa)
          val result = TestSelectYourReasonController.show(isAgent = true, isChange = false)(fakeRequestConfirmedClient())
          status(result) shouldBe OK
        }

        "user is not agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = poa)
          val result = TestSelectYourReasonController.show(isAgent = false, isChange = false)(fakeRequestWithNinoAndOrigin("PTA"))
          status(result) shouldBe OK
        }
      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {

      "Payment On Account Session data is missing" in {
        setupTest(
          sessionResponse = Right(None),
          claimToAdjustResponse = poa)
        val result = TestSelectYourReasonController.show(isAgent = false, isChange = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "POA data is missing" in {
        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData())),
          claimToAdjustResponse = None)
        val result = TestSelectYourReasonController.show(isAgent = false, isChange = false)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "Something goes wrong in payment on account session Service" in {
        setupTest(
          sessionResponse = Left(new Exception("Something went wrong")),
          claimToAdjustResponse = poa)

        val result = TestSelectYourReasonController.show(isAgent = false, isChange = false)(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "SelectYourReasonController.submit" should {

    s"return $BAD_REQUEST" when {

      s"no option is selected" in {

        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData())),
          claimToAdjustResponse = poa)

        val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, isChange = false).url)
          .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

        val result = TestSelectYourReasonController.submit(isAgent = false, isChange = false)(request)

        status(result) shouldBe BAD_REQUEST
      }
    }

    s"return $SEE_OTHER" when {

      "in normal mode" when {

        "if 'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {

          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = poa)

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, isChange = false).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, isChange = false)(request)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/enter-poa-amount")
        }

        "if 'totalAmount' is less than 'poaRelevantAmount'" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData(newPoAAmount = Some(5000.0)))),
            claimToAdjustResponse = poaTotalLessThanRelevant)

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, isChange = false).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, isChange = false)(request)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
        }
      }

      "in check mode" when {
        "if 'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {
          // TODO https://jira.tools.tax.service.gov.uk/browse/MISUV-7489
        }

        "if 'totalAmount' is less than 'poaRelevantAmount'" in {
          // TODO https://jira.tools.tax.service.gov.uk/browse/MISUV-7489
        }
      }
    }
  }
}