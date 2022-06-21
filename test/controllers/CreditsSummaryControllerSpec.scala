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

import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockNextUpdatesService}
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.nextUpdates.ObligationsModel
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.FinancialDetailsTestConstants._
import testUtils.TestSupport
import views.html.CreditsSummary

class CreditsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockFinancialDetailsService with FeatureSwitching with MockItvcErrorHandler
  with MockAuditingService with MockNextUpdatesService with MockIncomeSourceDetailsPredicate {

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
    MockIncomeSourceDetailsPredicate /*,
    mockAuditingService*/
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[AgentItvcErrorHandler]
  )

  val testCharges: List[DocumentDetail] = List(documentDetailModel().copy(documentDescription = Some("TRM New Charge"),
    documentText = Some("documentText")))

  val paymentRefundHistoryBackLink: String = "/report-quarterly/income-and-expenses/view/payment-refund-history"


  // TODO refactor it
  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel, fullDocumentDetailWithDueDateModel.copy(
    dueDate = fullDocumentDetailWithDueDateModel.documentDetail.interestEndDate, isLatePaymentInterest = true))
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val class2NicsChargesList: List[DocumentDetailWithDueDate] = List(documentDetailClass2Nic)
  val payeChargesList: List[DocumentDetailWithDueDate] = List(documentDetailPaye)
  val testObligtionsModel: ObligationsModel = ObligationsModel(Nil)
//  val taxYearsRefererBackLink: String = "http://www.somedomain.org/report-quarterly/income-and-expenses/view/tax-years"
//  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/tax-years"

  val agentHomeBackLink: String = "/report-quarterly/income-and-expenses/view/agents/client-income-tax"

  /*"The TaxYearSummary.renderTaxYearSummaryPage(year) action" when {
    def runForecastTest(crystallised: Boolean, calcDataNotFound: Boolean = false, forecastCalcFeatureSwitchEnabled: Boolean, taxYear: Int = testTaxYear,
                        shouldShowForecastData: Boolean): Unit = {
      if (forecastCalcFeatureSwitchEnabled)
        enable(ForecastCalculation)
      else disable(ForecastCalculation)
      mockSingleBusinessIncomeSource()
      if (crystallised) {
        mockCalculationSuccessFullNew(testMtditid, taxYear = taxYear)
      } else if (calcDataNotFound) {
        mockCalculationNotFoundNew(testMtditid, year = taxYear)
      } else mockCalculationSuccessFullNotCrystallised(testMtditid, taxYear = taxYear)
      mockFinancialDetailsSuccess(taxYear = taxYear)
      mockgetNextUpdates(fromDate = LocalDate.of(taxYear - 1, 4, 6),
        toDate = LocalDate.of(taxYear, 4, 5))(
        response = testObligtionsModel
      )

      val calcModel = if (crystallised) liabilityCalculationModelSuccessFull else liabilityCalculationModelSuccessFullNotCrystallised
      val calcOverview: Option[TaxYearSummaryViewModel] = if (calcDataNotFound) None else Some(TaxYearSummaryViewModel(calcModel))
      val expectedContent: String = taxYearSummaryView(
        taxYear,
        calcOverview,
        testChargesList,
        testObligtionsModel,
        taxYearsBackLink,
        codingOutEnabled = true,
        showForecastData = shouldShowForecastData
      ).toString

      val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(taxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe expectedContent
      contentType(result) shouldBe Some("text/html")
    }
  }*/

  // TODO needs to be implemented
  "MFACreditsAndDebits feature switch is enabled" should {
    "all calls are returned correctly and Referer was a Payment Refund History page" should {
      "show the Credits Summary Page and back link should be to the Payment Refund History page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess(financialDetailCreditAndRefundCharge)

        /*val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull) */
        val expectedContent: String = creditsSummaryView(
          backUrl = paymentRefundHistoryBackLink,
          utr = Some(testMtditid),
          enableMfaCreditsAndDebits = true,
//            testTaxYear,
          charges = testCharges,
        ).toString

        val result = TestCreditsSummaryController.showCreditsSummary(testTaxYear)(fakeRequestWithActiveAndRefererToPaymentRefundHistoryPage)

        status(result) shouldBe Status.OK

        contentAsString(result) shouldBe expectedContent
//        contentType(result) shouldBe Some("text/html")
//        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
//        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
    }
  }

   /* "ForecastCalculation feature switch is enabled" should {
      "show the Forecast tab before crystallisation" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = true, shouldShowForecastData = true)
      }
      "NOT show the Forecast tab after crystallisation" in {
        runForecastTest(crystallised = true, forecastCalcFeatureSwitchEnabled = true, shouldShowForecastData = false)
      }
      "show the Forecast tab when no calc data is returned" in {
        runForecastTest(crystallised = false, calcDataNotFound = true, forecastCalcFeatureSwitchEnabled = true, shouldShowForecastData = true)
      }
    }
    "ForecastCalculation feature switch is disabled" should {
      "NOT show the Forecast tab before crystallisation" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = false, shouldShowForecastData = false)
      }
      "NOT show the Forecast tab when no calc data is returned" in {
        runForecastTest(crystallised = false, calcDataNotFound = true, forecastCalcFeatureSwitchEnabled = false, shouldShowForecastData = false)
      }
    }
    "all calls are returned correctly" should {
      "show the Tax Year Summary Page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          testChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
    }

    "all calls are returned correctly and Referer was a Home page" should {
      "show the Tax Year Summary Page and back link should be to the Home page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          testChargesList,
          testObligtionsModel,
          homeBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveAndRefererToHomePage)

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
    }

    "the coding out feature switch is enabled" should {
      "include Class 2 Nics in the charges list when Class 2 Nics is present" in {
        enable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailClass2Nic.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          class2NicsChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }

      "include Paye in the charges list when Paye is present" in {
        enable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailPaye.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          payeChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
    }

    "the coding out feature switch is disabled" should {
      "not include Class 2 Nics in the charges list when Class 2 Nics is present" in {
        disable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailClass2Nic.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          testEmptyChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }

      "not include Paye in the charges list when Paye is present" in {
        disable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailPaye.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          testEmptyChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
    }

    s"getFinancialDetails returns a $NOT_FOUND" should {
      "show the Tax Year Summary Page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessFullNew(testMtditid)
        mockFinancialDetailsNotFound()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )


        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          Some(calcOverview),
          testEmptyChargesList,
          testObligtionsModel,
          taxYearsBackLink,
          codingOutEnabled = true
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
    }

    "getFinancialDetails returns an error" should {
      "show the internal server error page" in {
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsFailed()

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }

    "getNextUpdates returns an error" should {
      "show the internal server error page" in {
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsNotFound()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = NextUpdatesErrorModel(500, "INTERNAL_SERVER_ERROR")
        )

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }

    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Called with an Authenticated HMRC-MTD-IT User" when {
      "provided with a negative tax year" should {
        "return Internal Service Error (500)" in {
          mockPropertyIncomeSource()

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(-testTaxYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "the calculation returned from the calculation service was not found" should {
        "show tax year summary page with expected content" in {
          enable(ForecastCalculation)
          mockSingleBusinessIncomeSource()
          mockCalculationNotFoundNew(testMtditid)
          mockFinancialDetailsSuccess()
          mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
            toDate = LocalDate.of(testTaxYear, 4, 5))(
            response = testObligtionsModel
          )

          val expectedContent: String = Jsoup.parse(taxYearSummaryView(
            testTaxYear,
            None,
            testChargesList,
            testObligtionsModel,
            taxYearsBackLink,
            codingOutEnabled = true,
            showForecastData = true
          ).toString()).text()

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe Status.OK
          Jsoup.parse(contentAsString(result)).text() shouldBe expectedContent
          contentType(result) shouldBe Some("text/html")
          result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
          result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
        }
      }

      "the calculation returned from the calculation service was an error" should {
        "return the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationErrorNew(testMtditid)

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }
    }
  }
  "The TaxYearSummary.renderAgentTaxYearSummaryPage(year) action" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        mockShowInternalServerError()
        val result = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient()).failed.futureValue
        result shouldBe an[InternalServerException]
        result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }
    "there was a problem retrieving the calculation for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )
        mockCalculationErrorNew(nino = "AA111111A", year = testYearPlusTwo)
        mockShowInternalServerError()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving the charges for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(testFinancialDetailsErrorModel)
        mockShowInternalServerError()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving the updates for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR")
        )
        mockShowInternalServerError()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
    "no calculation data was returned" should {
      "show the tax year summary page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationNotFoundNew(nino = "AA111111A", year = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )
        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }

    "all calls to retrieve data were successful" should {
      "show the tax year summary page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }

    "all calls to retrieve data were successful and Referer was a Home page" should {
      "show the Tax Year Summary Page and back link should be to the Home page" in {
        enable(CodingOut)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, "AA111111A")(financialDetails(
          documentDetails = documentDetailClass2Nic.documentDetail
        ))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )

        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
        val expectedContent: String = taxYearSummaryView(
          testYearPlusTwo,
          Some(calcOverview),
          class2NicsChargesList,
          testObligtionsModel,
          agentHomeBackLink,
          isAgent = true,
          codingOutEnabled = true
        ).toString

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClientWithReferer(referer = homeBackLink))

        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedContent
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }
  }*/
}
