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
import config.ItvcErrorHandler
import config.featureswitch.{CodingOut, FeatureSwitching, ForecastCalculation}
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicateNoCache}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockNextUpdatesService}
import models.financialDetails.DocumentDetailWithDueDate
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.nextUpdates.{NextUpdatesErrorModel, ObligationsModel}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.FinancialDetailsTestConstants.{documentDetailClass2Nic, documentDetailPaye, financialDetails, fullDocumentDetailWithDueDateModel}
import testConstants.MessagesLookUp
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelSuccessFull, liabilityCalculationModelSuccessFullNotCrystallised}
import testUtils.TestSupport
import views.html.TaxYearSummary

import java.time.LocalDate

class TaxYearSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicateNoCache
  with MockFinancialDetailsService with FeatureSwitching
  with MockAuditingService with MockNextUpdatesService {

  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  object mockDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse("2018-03-29")
    override def getCurrentTaxYearEnd(currentDate: LocalDate): Int = 2018
  }

  object TestTaxYearSummaryController$ extends TaxYearSummaryController(
    taxYearSummaryView,
    MockAuthenticationPredicate,
    mockCalculationService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockFinancialDetailsService,
    app.injector.instanceOf[ItvcErrorHandler],
    MockIncomeSourceDetailsPredicateNoCache,
    app.injector.instanceOf[NinoPredicate],
    mockNextUpdatesService,
    app.injector.instanceOf[BtaNavBarPredicate],
    mockAuditingService,
    mockDateService
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  lazy val messagesLookUp = new MessagesLookUp.Calculation(testTaxYear)

  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel, fullDocumentDetailWithDueDateModel.copy(
    dueDate = fullDocumentDetailWithDueDateModel.documentDetail.interestEndDate, isLatePaymentInterest = true))
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val class2NicsChargesList: List[DocumentDetailWithDueDate] = List(documentDetailClass2Nic)
  val payeChargesList: List[DocumentDetailWithDueDate] = List(documentDetailPaye)
  val testObligtionsModel: ObligationsModel = ObligationsModel(Nil)
  val taxYearsRefererBackLink: String = "http://www.somedomain.org/report-quarterly/income-and-expenses/view/tax-years"
  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/tax-years"
  val homeBackLink: String = "/report-quarterly/income-and-expenses/view"

  "The TaxYearSummary.renderTaxYearSummaryPage(year) action" when {
    def runForecastTest(crystallised: Boolean, forecastCalcFeatureSwitchEnabled: Boolean, taxYear: Int = testTaxYear,
                        shouldShowForecastData: Boolean): Unit = {
      if (forecastCalcFeatureSwitchEnabled)
        enable(ForecastCalculation)
      else disable(ForecastCalculation)
      mockSingleBusinessIncomeSource()
      if (crystallised) {
        mockCalculationSuccessFullNew(testMtditid, taxYear = taxYear)
      } else mockCalculationSuccessFullNotCrystallised(testMtditid, taxYear = taxYear)
      mockFinancialDetailsSuccess(taxYear = taxYear)
      mockgetNextUpdates(fromDate = LocalDate.of(taxYear - 1, 4, 6),
        toDate = LocalDate.of(taxYear, 4, 5))(
        response = testObligtionsModel
      )

      val calcModel = if (crystallised) liabilityCalculationModelSuccessFull else liabilityCalculationModelSuccessFullNotCrystallised
      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(calcModel)
      val expectedContent: String = taxYearSummaryView(
        taxYear,
        Some(calcOverview),
        testChargesList,
        testObligtionsModel,
        taxYearsBackLink,
        codingOutEnabled = true,
        showForecastData = shouldShowForecastData
      ).toString

      val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(taxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

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
    }
    "ForecastCalculation feature switch is disabled" should {
      "NOT show the Forecast tab before crystallisation" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = false, shouldShowForecastData = false)
      }
    }
    "tax year is current year" should {
      "show the Forecast tab" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = true, taxYear = 2018, shouldShowForecastData = true)
      }
    }
    "tax year is NOT current year" should {
      "NOT show the Forecast tab" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = true, taxYear = 2017, shouldShowForecastData = false)
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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearSummary")
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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveAndRefererToHomePage)

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearSummary")
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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearSummary")
      }
    }

    "getFinancialDetails returns an error" should {
      "show the internal server error page" in {
        mockSingleBusinessIncomeSource()
        mockFinancialDetailsFailed()

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

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

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }

    "Called with an Unauthenticated User" should {
      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()

        val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Called with an Authenticated HMRC-MTD-IT User" when {
      "provided with a negative tax year" should {
        "return Internal Service Error (500)" in {
          mockPropertyIncomeSource()

          val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(-testTaxYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "the calculation returned from the calculation service was not found" should {
        "show tax year summary page with expected content" in {
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
            codingOutEnabled = true
          ).toString()).text()

          val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSessionWithReferer(referer = taxYearsBackLink))

          status(result) shouldBe Status.OK
          Jsoup.parse(contentAsString(result)).text() shouldBe expectedContent
          contentType(result) shouldBe Some("text/html")
          result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearSummary")
        }
      }

      "the calculation returned from the calculation service was an error" should {
        "return the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationErrorNew(testMtditid)

          val result = TestTaxYearSummaryController$.renderTaxYearSummaryPage(testTaxYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }
    }
  }
}
