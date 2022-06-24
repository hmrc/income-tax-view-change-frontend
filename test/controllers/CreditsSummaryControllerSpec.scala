/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.featureswitch.{FeatureSwitching, MFACreditsAndDebits}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockNextUpdatesService}
import models.financialDetails.DocumentDetail
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testMtditid, testTaxYear, testYearPlusTwo}
import testConstants.FinancialDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import views.html.CreditsSummary

class CreditsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockFinancialDetailsService with FeatureSwitching with MockItvcErrorHandler
  with MockNextUpdatesService with MockIncomeSourceDetailsPredicate {

  val creditsSummaryView: CreditsSummary = app.injector.instanceOf[CreditsSummary]

  object TestCreditsSummaryController extends CreditsSummaryController(
    creditsSummaryView,
    mockAuthService,
    mockIncomeSourceDetailsService,
    mockFinancialDetailsService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NavBarPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[AgentItvcErrorHandler]
  )

  val testCharges: List[DocumentDetail] = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = Some(-1400.00),
      paymentLotItem = None,
      paymentLot = None
    )
  )

  val paymentRefundHistoryBackLink: String = "/report-quarterly/income-and-expenses/view/payment-refund-history"
  val agentHomeBackLink: String = "/report-quarterly/income-and-expenses/view/agents/client-income-tax"

  "MFACreditsAndDebits feature switch is enabled" should {
    "all calls are returned correctly and Referer was a Payment Refund History page" should {
      "show the Credits Summary Page and back link should be to the Payment Refund History page" in {
        enable(MFACreditsAndDebits)
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsSuccess(financialDetailCreditChargeMFA)

        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testMtditid),
          enableMfaCreditsAndDebits = true,
          charges = creditAndRefundDocumentDetailListMFA,
          calendarYear = testTaxYear
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = paymentRefundHistoryBackLink))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }
    }

    s"getFinancialDetails returns a $NOT_FOUND" should {
      "show Credits Summary page" in {
        enable(MFACreditsAndDebits)
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsNotFound()

        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testMtditid),
          enableMfaCreditsAndDebits = true,
          charges = List.empty,
          calendarYear = testTaxYear
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = paymentRefundHistoryBackLink))

        status(result) shouldBe Status.OK

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }
    }

    "getFinancialDetails returns an error" should {
      "show the internal server error page" in {
        enable(MFACreditsAndDebits)
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsFailed()

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }

    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Called with an Authenticated HMRC-MTD-IT User" when {
      "provided with a negative tax year" should {
        "return Internal Service Error (500)" in {
          mockPropertyIncomeSource()

          val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "MFACreditsAndDebits feature switch is disabled" should {
    "all calls are returned correctly and Referer was a Payment Refund History page" should {
      "redirect to the Home page" in {
        disable(MFACreditsAndDebits)
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsSuccess(financialDetailCreditChargeMFA)

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
  }

  "The CreditsSummaryController.showAgentCreditsSummary(year) action" when {

    // todo do we need this test
   /* "the user is not authenticated" should {
      "redirect them to sign in" in {
        enable(MFACreditsAndDebits)
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestCreditsSummaryController.showAgentCreditsSummary(testTaxYear)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }*/
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result = TestCreditsSummaryController.showAgentCreditsSummary(testTaxYear)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestCreditsSummaryController.showAgentCreditsSummary(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        mockShowInternalServerError()
        val result = TestCreditsSummaryController.showAgentCreditsSummary(testTaxYear)(fakeRequestConfirmedClient()).failed.futureValue
        result shouldBe an[InternalServerException]
        result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }
    "there was a problem retrieving the charges for the user" should {
      "return technical difficulties" in {
        enable(MFACreditsAndDebits)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(testFinancialDetailsErrorModel)
        mockShowInternalServerError()

        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
  }
}
