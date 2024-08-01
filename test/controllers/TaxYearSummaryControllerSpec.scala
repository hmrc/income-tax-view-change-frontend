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

import audit.mocks.MockAuditingService
import config.featureswitch._
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import mocks.MockItvcErrorHandler
import mocks.connectors.MockIncomeTaxCalculationConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockCalculationService, MockClaimToAdjustService, MockFinancialDetailsService, MockNextUpdatesService}
import models.admin.{AdjustPaymentsOnAccount, CodingOut, ForecastCalculation, MFACreditsAndDebits, NavBarFs}
import models.financialDetails.DocumentDetailWithDueDate
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{Message, Messages}
import models.obligations.{SingleObligationModel, ObligationsErrorModel, GroupedObligationsModel, ObligationsModel, StatusFulfilled}
import org.jsoup.Jsoup
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.Lang
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import services.DateService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testMtditid, testNino, testTaxYear, testYearPlusOne, testYearPlusTwo}
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.FinancialDetailsTestConstants._
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelErrorMessagesForAgent, liabilityCalculationModelErrorMessagesForIndividual, liabilityCalculationModelSuccessful, liabilityCalculationModelSuccessfulNotCrystallised}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import views.html.TaxYearSummary

import java.time.LocalDate
import scala.concurrent.Future

class TaxYearSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockFinancialDetailsService with FeatureSwitching with MockItvcErrorHandler
  with MockAuditingService with MockNextUpdatesService with MockIncomeTaxCalculationConnector with MockClaimToAdjustService {

  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  override def beforeEach(): Unit = {
    disableAllSwitches()
    super.beforeEach()
  }

  object TestTaxYearSummaryController extends TaxYearSummaryController(
    taxYearSummaryView = taxYearSummaryView,
    calculationService = mockCalculationService,
    financialDetailsService = mockFinancialDetailsService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    nextUpdatesService = mockNextUpdatesService,
    messagesApi = messagesApi,
    languageUtils = languageUtils,
    authorisedFunctions = mockAuthService,
    auditingService = mockAuditingService,
    claimToAdjustService = mockClaimToAdjustService,
    auth = testAuthenticator,
  )(appConfig,
    app.injector.instanceOf[DateService],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel, fullDocumentDetailWithDueDateModel.copy(
    dueDate = fullDocumentDetailWithDueDateModel.documentDetail.interestEndDate, isLatePaymentInterest = true))
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val class2NicsChargesList: List[DocumentDetailWithDueDate] = List(documentDetailClass2Nic)
  val payeChargesList: List[DocumentDetailWithDueDate] = List(documentDetailPaye)
  val taxYearsRefererBackLink: String = "http://www.somedomain.org/report-quarterly/income-and-expenses/view/tax-years"
  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/tax-years"
  val homeBackLink: String = "/report-quarterly/income-and-expenses/view"
  val agentHomeBackLink: String = "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
  val emptyCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, None)
  val populatedCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = true, Some(TaxYear(2023, 2024)))

  val testObligtionsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(fixedDate),
          periodKey = "Quarterly",
          StatusFulfilled
        )
      )
    )
  ))

  "The TaxYearSummary.renderTaxYearSummaryPage(year) action" when {
    def runForecastTest(crystallised: Boolean, calcDataNotFound: Boolean = false, forecastCalcFeatureSwitchEnabled: Boolean, taxYear: Int = testTaxYear,
                        shouldShowForecastData: Boolean): Unit = {
      disableAllSwitches()
      if (forecastCalcFeatureSwitchEnabled)
        enable(ForecastCalculation)
      else disable(ForecastCalculation)
      mockSingleBusinessIncomeSource()
      if (crystallised) {
        mockCalculationSuccessfulNew(testMtditid, taxYear = taxYear)
      } else if (calcDataNotFound) {
        mockCalculationNotFoundNew(testMtditid, year = taxYear)
      } else mockCalculationSuccessfulNotCrystallised(testMtditid, taxYear = taxYear)
      mockFinancialDetailsSuccess(taxYear = taxYear)
      mockgetNextUpdates(fromDate = LocalDate.of(taxYear - 1, 4, 6),
        toDate = LocalDate.of(taxYear, 4, 5))(
        response = testObligtionsModel
      )

      val calcModel = if (crystallised) liabilityCalculationModelSuccessful else liabilityCalculationModelSuccessfulNotCrystallised
      val calcOverview: Option[CalculationSummary] = if (calcDataNotFound) None else Some(CalculationSummary(calcModel))
      val expectedContent: String = taxYearSummaryView(
        taxYear, TaxYearSummaryViewModel(
          calcOverview,
          testChargesList,
          testObligtionsModel,
          codingOutEnabled = true,
          showForecastData = shouldShowForecastData,
          ctaViewModel = emptyCTAViewModel
        ),
        taxYearsBackLink,
      ).toString

      val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(taxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe expectedContent
      contentType(result) shouldBe Some("text/html")
    }

    "ForecastCalculation feature switch is enabled" should {
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
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
    }
    "user has valid POAs, in non crystallised tax years" should {
      "show the tax year summary page with the poa section" when {
        "AdjustPaymentsOnAccount FS is enabled and POAs are for the tax year on the page" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBusinessIncomeSource()
          mockCalculationSuccessfulNew(testMtditid)
          mockFinancialDetailsSuccess()
          mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
            toDate = LocalDate.of(testTaxYear, 4, 5))(
            response = testObligtionsModel
          )
          setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe OK
          contentAsString(result).contains("Adjust payments on account") shouldBe true
        }
      }
      "show the tax year summary page without the poa section" when {
        "AdjustPaymentsOnAccount FS is enabled and POAs are for the tax year of a different year" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBusinessIncomeSource()
          mockCalculationSuccessfulNew(testMtditid)
          mockFinancialDetailsSuccess()
          mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
            toDate = LocalDate.of(testTaxYear, 4, 5))(
            response = testObligtionsModel
          )
          setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe OK
          contentAsString(result).contains("Adjust payments on account") shouldBe true
        }
      }
    }
    "user has no valid POAs" should {
      "show the tax year summary page without the poa section" when {
        "AdjustPaymentsOnAccount FS is enabled" in {
          enable(AdjustPaymentsOnAccount)
          mockSingleBusinessIncomeSource()
          mockCalculationSuccessfulNew(testMtditid)
          mockFinancialDetailsSuccess()
          mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
            toDate = LocalDate.of(testTaxYear, 4, 5))(
            response = testObligtionsModel
          )
          setupMockGetPoaTaxYearForEntryPointCall(Right(None))

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe OK
          contentAsString(result).contains("Adjust payments on account") shouldBe false
        }
      }
      "show the tax year summary page without the poa section" when {
        "AdjustPaymentsOnAccount FS is disabled" in {
          disable(AdjustPaymentsOnAccount)
          mockSingleBusinessIncomeSource()
          mockCalculationSuccessfulNew(testMtditid)
          mockFinancialDetailsSuccess()
          mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
            toDate = LocalDate.of(testTaxYear, 4, 5))(
            response = testObligtionsModel
          )
          setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

          val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe OK
          contentAsString(result).contains("Adjust payments on account") shouldBe false
        }
      }
    }
    "claimToAdjustService.getPoaTaxYearForEntryPoint returns a Left containing an exception" should {
      "hit the recover block and redirect to the internal server error page" in {
        enable(AdjustPaymentsOnAccount)
        mockSingleBusinessIncomeSource()
        setupMockGetPoaTaxYearForEntryPointCall(Left(new Exception("TEST")))

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "all calls are returned correctly and Referer was a Home page" should {
      "show the Tax Year Summary Page and back link should be to the Home page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          homeBackLink).toString

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
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailClass2Nic.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            class2NicsChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }

      "include Paye in the charges list when Paye is present" in {
        enable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailPaye.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            payeChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
    }

    "the coding out feature switch is disabled" should {
      "not include Class 2 Nics in the charges list when Class 2 Nics is present" in {
        disable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailClass2Nic.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testEmptyChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink,
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }

      "not include Paye in the charges list when Paye is present" in {
        disable(CodingOut)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess(
          financialDetailsModelResponse = financialDetails(
            documentDetails = documentDetailPaye.documentDetail
          )
        )
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testEmptyChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink,

        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
    }

    "MFA Debits" should {
      def testMFADebits(MFAEnabled: Boolean): Assertion = {
        if (MFAEnabled) enable(MFACreditsAndDebits) else disable(MFACreditsAndDebits)

        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsSuccess(financialDetailsModelResponse = MFADebitsFinancialDetails)
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val charges = if (MFAEnabled) MFADebitsDocumentDetailsWithDueDates else testEmptyChargesList
        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          TaxYearSummaryViewModel(
            Some(calcOverview),
            charges,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel
          ),
          taxYearsBackLink
        ).toString
        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }

      "display MFA Debits when FS is enabled" in {
        testMFADebits(true)
      }
      "not display MFA Debits when FS is disabled" in {
        testMFADebits(false)
      }
    }

    s"getFinancialDetails returns a $NOT_FOUND" should {
      "show the Tax Year Summary Page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccessfulNew(testMtditid)
        mockFinancialDetailsNotFound()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )


        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testEmptyChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink
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
          response = ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
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
            testTaxYear, TaxYearSummaryViewModel(
              None,
              testChargesList,
              testObligtionsModel,
              codingOutEnabled = true,
              showForecastData = true,
              ctaViewModel = emptyCTAViewModel),
            taxYearsBackLink
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

    "liability Calculation has error messages" should {

      "filter out the variable value from messages for individuals" in {
        val actual = TestTaxYearSummaryController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = false)(Lang("GB"), messages)

        actual shouldBe liabilityCalculationModelErrorMessagesForIndividual.copy(messages = Some(Messages(
          errors = Some(List(
            Message("C55012", "5 January 2023"),
            Message("C15507", "£2000"),
            Message("C15510", "10"),
            Message("C55009", ""),
          ))
        )))
      }

      "filter out the variable value from messages for agents" in {
        val actual = TestTaxYearSummaryController.formatErrorMessages(liabilityCalculationModelErrorMessagesForAgent, messagesApi, isAgent = true)(Lang("GB"), messages)

        actual shouldBe liabilityCalculationModelErrorMessagesForIndividual.copy(messages = Some(Messages(
          errors = Some(List(
            Message("C55012", "5 January 2023"),
            Message("C15507", "£2000"),
            Message("C15510", "10"),
            Message("C55009", ""),
          ))
        )))
      }

      "show the Tax Year Summary Page with error messages for individual" in {
        disable(NavBarFs)
        mockSingleBusinessIncomeSource()
        mockCalculationWithErrorMessages(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )
        val errorMessageVariableValues = TestTaxYearSummaryController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = false)(Lang("GB"), messages)
        val calcOverview: CalculationSummary = CalculationSummary(errorMessageVariableValues)

        val expectedContent: String = taxYearSummaryView(
          testTaxYear, TaxYearSummaryViewModel(
            Some(calcOverview),
            testChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }
      "show the Tax Year Summary Page with error messages for agent" in {
        disable(NavBarFs)
        mockSingleBusinessIncomeSource()
        mockCalculationWithErrorMessages(testMtditid)
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
          toDate = LocalDate.of(testTaxYear, 4, 5))(
          response = testObligtionsModel
        )
        val errorMessageVariableValues = TestTaxYearSummaryController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = false)(Lang("GB"), messages)
        val calcOverview: CalculationSummary = CalculationSummary(errorMessageVariableValues)

        val expectedContent: String = taxYearSummaryView(
          testTaxYear,
          TaxYearSummaryViewModel(
            Some(calcOverview),
            testChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          taxYearsBackLink
        ).toString

        val result = TestTaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
        result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
      }


    }
  }

  "The TaxYearSummary.renderAgentTaxYearSummaryPage(year) action" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
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
        result.getMessage shouldBe "IncomeSourceDetailsModel not created"
      }
    }
    "there was a problem retrieving the calculation for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )
        mockCalculationErrorNew(nino = testNino, year = testYearPlusTwo)
        mockShowInternalServerError()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving the charges for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(testFinancialDetailsErrorModel)
        mockShowInternalServerError()

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving the updates for the user" should {
      "return technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsErrorModel(INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR")
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
        mockCalculationNotFoundNew(year = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )
        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }

    "all calls to retrieve data were successful" should {
      "show the tax year summary page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationSuccessfulNew(taxYear = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetailsModel(testYearPlusTwo))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(fakeRequestConfirmedClient(testNino))

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
        mockCalculationSuccessfulNew(taxYear = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetails(
          documentDetails = documentDetailClass2Nic.documentDetail
        ))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          testObligtionsModel
        )

        val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)

        val expectedContent: String = taxYearSummaryView(
          testYearPlusTwo,
          TaxYearSummaryViewModel(
            Some(calcOverview),
            class2NicsChargesList,
            testObligtionsModel,
            codingOutEnabled = true,
            ctaViewModel = emptyCTAViewModel),
          agentHomeBackLink,
          isAgent = true,
        ).toString

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(
          fakeRequestConfirmedClientWithReferer(clientNino = testNino, referer = homeBackLink))

        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedContent
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }

    "calls to retrieve data were successful with No Obligations and Referer was a Home page" should {
      "show the Tax Year Summary Page and back link to the Home page" in {
        enable(CodingOut)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationSuccessfulNew(taxYear = testYearPlusTwo)
        setupMockGetFinancialDetailsWithTaxYearAndNino(testYearPlusTwo, testNino)(financialDetails(
          documentDetails = documentDetailClass2Nic.documentDetail
        ))
        mockgetNextUpdates(fromDate = LocalDate.of(testYearPlusOne, 4, 6), toDate = LocalDate.of(testYearPlusTwo, 4, 5))(
          ObligationsModel(Nil)
        )

        val result: Future[Result] = TestTaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear = testYearPlusTwo)(
          fakeRequestConfirmedClientWithReferer(clientNino = testNino, referer = homeBackLink))

        status(result) shouldBe OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
      }
    }
  }
}
