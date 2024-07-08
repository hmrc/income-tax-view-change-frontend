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
import forms.adjustPoa.SelectYourReasonFormProvider
import mocks.connectors.{MockCalculationListConnector, MockFinancialDetailsConnector}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{AllowanceOrReliefHigher, Increase, MainIncomeLower, MoreTaxedAtSource, OtherIncomeLower, PaymentOnAccountViewModel, PoAAmendmentData}
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.claimToAdjustPOA.ClaimToAdjustPOATestConstants.testPoa1Maybe
import testUtils.{TestSupport, ViewSpec}
import views.html.claimToAdjustPoa.SelectYourReasonView

import scala.concurrent.{ExecutionContext, Future}

class SelectYourReasonControllerSpec extends MockAuthenticationPredicate with TestSupport
  with FeatureSwitching
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockCalculationListService
  with MockCalculationListConnector
  with ViewSpec
  with MockFinancialDetailsConnector {

  object TestSelectYourReasonController extends SelectYourReasonController(
    authorisedFunctions = mockAuthService,
    claimToAdjustService = mockClaimToAdjustService,
    auth = testAuthenticator,
    view = app.injector.instanceOf[SelectYourReasonView],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    formProvider = app.injector.instanceOf[SelectYourReasonFormProvider],
    poaSessionService = mockPaymentOnAccountSessionService,
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
      totalAmountOne = 5000.00,
      totalAmountTwo = 5000.00,
      relevantAmountOne = 5000.00,
      relevantAmountTwo = 5000.00,
      poAPartiallyPaid = false,
      poAFullyPaid = false,
      poaPreviouslyAdjusted = None
    ))

  val poaTotalLessThanRelevant: Option[PaymentOnAccountViewModel] = poa.map(_.copy(
    totalAmountOne = 1000.0,
    totalAmountTwo = 1000.0))

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
        claimToAdjustResponse = testPoa1Maybe)

      setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

      val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
    }

    s"return status $SEE_OTHER" when {
      "Adjust Payments On Account FS is Disabled" in {
        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData(newPoAAmount = Some(20000.0)))),
          claimToAdjustResponse = testPoa1Maybe)

        disable(AdjustPaymentsOnAccount)

        setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

        val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestSelectYourReasonController.show(isAgent = true, mode = NormalMode)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "Adjust Payments On Account FS is Enabled but journeyCompleted flag is true" in {
        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData(None, newPoAAmount = Some(20000.0), journeyCompleted = true))),
          claimToAdjustResponse = testPoa1Maybe)

        setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

        val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent = TestSelectYourReasonController.show(isAgent = true, mode = NormalMode)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resultAgent) shouldBe SEE_OTHER
        redirectLocation(resultAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }

    s"return status: $OK when PoA tax year crystallized" when {

      "in check mode must pre-populate the answer with AllowanceOrReliefHigher" when {
        "the user previously selected the AllowanceOrReliefHigher checkbox" in {
          setupTest(
            sessionResponse = Right(Some(
              PoAAmendmentData(newPoAAmount = Some(200.0), poaAdjustmentReason = Some(AllowanceOrReliefHigher))
            )),
            claimToAdjustResponse = testPoa1Maybe
          )

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(AllowanceOrReliefHigher)

          val result = TestSelectYourReasonController.show(isAgent = false, mode = CheckMode)(fakeRequestWithNinoAndOrigin("PTA"))
          val resultAgent = TestSelectYourReasonController.show(isAgent = true, mode = CheckMode)(fakeRequestConfirmedClient())
          status(result) shouldBe OK
          status(resultAgent) shouldBe OK
          Jsoup.parse(contentAsString(result)).select("#value-3[checked]").toArray should have length 1
          Jsoup.parse(contentAsString(resultAgent)).select("#value-3[checked]").toArray should have length 1
        }
      }

      s"in normal mode and session data has no poaAdjustmentReason data" when {
        "user is agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = testPoa1Maybe)
          val result = TestSelectYourReasonController.show(isAgent = true, mode = NormalMode)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
        }

        "user is not agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = testPoa1Maybe)
          val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe OK
        }
      }

      s"in normal mode and session data has valid poaAdjustmentReason data" when {
        "user is agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData(poaAdjustmentReason = Some(MoreTaxedAtSource)))),
            claimToAdjustResponse = testPoa1Maybe)
          val result = TestSelectYourReasonController.show(isAgent = true, mode = NormalMode)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          Jsoup.parse(contentAsString(result)).select("#value-4[checked]").toArray should have length 1
        }

        "user is not agent" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData(poaAdjustmentReason = Some(OtherIncomeLower)))),
            claimToAdjustResponse = testPoa1Maybe)
          val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe OK
          Jsoup.parse(contentAsString(result)).select("#value-2[checked]").toArray should have length 1
        }
      }
    }

    s"return status: $INTERNAL_SERVER_ERROR" when {

      "Payment On Account Session data is missing" in {
        setupTest(
          sessionResponse = Right(None),
          claimToAdjustResponse = testPoa1Maybe)
        val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "POA data is missing" in {
        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData())),
          claimToAdjustResponse = None)
        val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "Something goes wrong in payment on account session Service" in {
        setupTest(
          sessionResponse = Left(new Exception("Something went wrong")),
          claimToAdjustResponse = testPoa1Maybe)

        val result = TestSelectYourReasonController.show(isAgent = false, mode = NormalMode)(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "SelectYourReasonController.submit" should {

    s"return $BAD_REQUEST" when {

      s"no option is selected" in {

        setupTest(
          sessionResponse = Right(Some(PoAAmendmentData())),
          claimToAdjustResponse = testPoa1Maybe)

        val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, mode = NormalMode).url)
          .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

        val result = TestSelectYourReasonController.submit(isAgent = false, mode = NormalMode)(request)

        status(result) shouldBe BAD_REQUEST
      }
    }

    s"return $SEE_OTHER" when {

      "in normal mode" when {

        "if 'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {

          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = testPoa1Maybe)

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, mode = NormalMode).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, mode = NormalMode)(request)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/enter-poa-amount")
        }

        "if 'totalAmount' is less than 'poaRelevantAmount'" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData(newPoAAmount = Some(5000.0)))),
            claimToAdjustResponse = poaTotalLessThanRelevant)

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, mode = NormalMode).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, mode = NormalMode)(request)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
        }
      }

      "in check mode" when {
        "if 'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData())),
            claimToAdjustResponse = testPoa1Maybe
          )

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, mode = CheckMode).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val requestAgent = fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, mode = CheckMode)(request)
          val resultAgent = TestSelectYourReasonController.submit(isAgent = true, mode = CheckMode)(requestAgent)

          status(result) shouldBe SEE_OTHER
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
          redirectLocation(resultAgent) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/adjust-poa/check-your-answers")
        }

        "if 'totalAmount' is less than 'poaRelevantAmount'" in {
          setupTest(
            sessionResponse = Right(Some(PoAAmendmentData(newPoAAmount = Some(5000.0)))),
            claimToAdjustResponse = poaTotalLessThanRelevant
          )

          setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

          val request = FakeRequest(POST, routes.SelectYourReasonController.submit(isAgent = false, mode = CheckMode).url)
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val requestAgent = fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody("value" -> "MainIncomeLower")
            .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")

          val result = TestSelectYourReasonController.submit(isAgent = false, mode = CheckMode)(request)
          val resultAgent = TestSelectYourReasonController.submit(isAgent = true, mode = CheckMode)(requestAgent)

          status(result) shouldBe SEE_OTHER
          status(resultAgent) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/adjust-poa/check-your-answers")
          redirectLocation(resultAgent) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/adjust-poa/check-your-answers")
        }
      }
    }
  }
}