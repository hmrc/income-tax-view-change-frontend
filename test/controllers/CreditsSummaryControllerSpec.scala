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

package controllers

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockCalculationService, MockCreditHistoryService, MockFinancialDetailsService, MockNextUpdatesService}
import models.financialDetails.{BalanceDetails, DocumentDetail}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{calendarYear2018, testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testIndividualAuthSuccessWithSaUtrResponse, testSaUtrId, testYearPlusTwo}
import testConstants.FinancialDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import views.html.CreditsSummary


class CreditsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockFinancialDetailsService with MockItvcErrorHandler
  with MockNextUpdatesService with MockIncomeSourceDetailsPredicate with MockCreditHistoryService {

  val creditsSummaryView: CreditsSummary = app.injector.instanceOf[CreditsSummary]

  object TestCreditsSummaryController extends CreditsSummaryController(
    creditsSummaryView,
    mockAuthService,
    mockIncomeSourceDetailsService,
    mockCreditHistoryService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NavBarPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    testAuthenticator
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[MessagesApi],
    ec,
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockAuditingService
  )

  val testCharges: List[DocumentDetail] = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = -1400.00,
      paymentLotItem = None,
      paymentLot = None
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  val paymentRefundHistoryBackLink: String = "/report-quarterly/income-and-expenses/view/payment-refund-history"
  val agentHomeBackLink: String = "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
  lazy val creditAndRefundUrl: String = controllers.routes.CreditAndRefundController.show().url
  lazy val defaultCreditsSummaryUrl: String = controllers.routes.CreditsSummaryController.showCreditsSummary(2018, None).url

  "CreditsSummaryController.handleRequest" should {
    "all calls are returned correctly and Referer was a Payment Refund History page" should {
      "show the Credits Summary Page with a list of MFA/CutOver/BC credits and back link should be to the Payment Refund History page" in {
        val chargesList = creditAndRefundCreditDetailListMFAWithCutoverAndBCC

        mockSingleBusinessIncomeSource()
        mockCreditHistoryService(chargesList)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testSaUtrId),
          charges = chargesList,
          maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.availableCredit,
          calendarYear = calendarYear2018
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSessionWithReferer(referer = paymentRefundHistoryBackLink))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }

      "show the Credits Summary Page and back link should be to the Payment Refund History page and the money Money in your account section should not be available when available credit is Some(0.00)" in {
        val emptyBalanceDetails = BalanceDetails(0.00, 0.00, 0.00, Some(0.0), None, None, None, None)
        val chargesList = creditAndRefundCreditDetailListMFA.map(_.copy(balanceDetails = Some(emptyBalanceDetails)))

        mockSingleBusinessIncomeSource()
        mockCreditHistoryService(chargesList)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testSaUtrId),
          charges = chargesList,
          maybeAvailableCredit = None,
          calendarYear = calendarYear2018
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSessionWithReferer(referer = paymentRefundHistoryBackLink))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }
    }

    "all calls are returned correctly and Referer was a Credit and Refund page" should {
      "show the Credits Summary Page and back link should be to the Credit and Refund page" in {
        val chargesList = creditAndRefundCreditDetailListMFA

        mockSingleBusinessIncomeSource()
        mockCreditHistoryService(chargesList)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = creditsSummaryView(
          backUrl = creditAndRefundUrl,
          utr = Some(testSaUtrId),
          charges = chargesList,
          maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.availableCredit,
          calendarYear = calendarYear2018
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSessionWithReferer(referer = creditAndRefundUrl))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }
    }

    "all calls are returned correctly and Referer was a Credits Summary page when referrer is not provided" should {
      "show the Credits Summary Page and back link should be to the Credits Summary page" in {
        val chargesList = creditAndRefundCreditDetailListMFA

        mockSingleBusinessIncomeSource()
        mockCreditHistoryService(chargesList)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = creditsSummaryView(
          backUrl = defaultCreditsSummaryUrl,
          utr = Some(testSaUtrId),
          charges = chargesList,
          maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.availableCredit,
          calendarYear = calendarYear2018
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSessionWithReferer(referer = ""))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
      }
    }

    "all calls are returned correctly and Referer was a Payment Refund History page" should {
      "show the Credits Summary Page with multiple records ordered properly and back link should be to the Payment Refund History page" in {
        val chargesList = creditAndRefundCreditDetailListMultipleChargesMFA

        mockSingleBusinessIncomeSource()
        mockCreditHistoryService(chargesList)
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testSaUtrId),
          charges = chargesList,
          maybeAvailableCredit = financialDetailCreditCharge.balanceDetails.availableCredit,
          calendarYear = calendarYear2018
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSessionWithReferer(referer = paymentRefundHistoryBackLink))

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some(HTML)
        contentAsString(result) shouldBe expectedContent
      }
    }

    "getCreditsHistory returns an error" should {
      "show the internal server error page" in {
        mockSingleBusinessIncomeSource()
        mockCreditHistoryFailed()

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }

    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()

        val result = TestCreditsSummaryController.showCreditsSummary(calendarYear2018)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "The CreditsSummaryController.showAgentCreditsSummary(year) action" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear2018)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear2018)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear2018)(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        mockShowInternalServerError()
        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear2018)(fakeRequestConfirmedClient()).failed.futureValue
        result shouldBe an[InternalServerException]
        result.getMessage shouldBe "IncomeSourceDetailsModel not created"
      }
    }
    "there was a problem retrieving the charges for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(testFinancialDetailsErrorModel)

        mockCreditHistoryFailed()


        val result = TestCreditsSummaryController.showAgentCreditsSummary(calendarYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
  }
}
