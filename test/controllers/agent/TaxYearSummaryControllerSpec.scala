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

package controllers.agent

import audit.mocks.MockAuditingService
import config.featureswitch.{CodingOut, FeatureSwitching, ForecastCalculation}
import forms.utils.SessionKeys.gatewayPage
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService}
import models.financialDetails.DocumentDetailWithDueDate
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.nextUpdates.{NextUpdatesErrorModel, ObligationsModel}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import testConstants.FinancialDetailsTestConstants._
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelSuccessFull, liabilityCalculationModelSuccessFullNotCrystallised}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxYearSummary

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class TaxYearSummaryControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with MockFinancialDetailsService
  with FeatureSwitching with MockCalculationService with MockIncomeSourceDetailsService
  with MockNextUpdatesService with MockItvcErrorHandler with MockAuditingService {

  val testYear: Int = 2020
  object mockDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse("2020-03-29")
    override def getCurrentTaxYearEnd(currentDate: LocalDate): Int = 2020
  }
  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    dueDate = fullDocumentDetailWithDueDateModel.documentDetail.interestEndDate, isLatePaymentInterest = true),
    fullDocumentDetailWithDueDateModel)
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val class2NicsChargesList: List[DocumentDetailWithDueDate] = List(documentDetailClass2Nic)
  val payeChargesList: List[DocumentDetailWithDueDate] = List(documentDetailPaye)
  val testObligationsModel: ObligationsModel = ObligationsModel(Nil)
  val homeBackLink: String = "/report-quarterly/income-and-expenses/view/agents/income-tax-account"
  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/agents/tax-years"

  val controller: TaxYearSummaryController = new TaxYearSummaryController(
    taxYearSummary = taxYearSummaryView,
    authorisedFunctions = mockAuthService,
    calculationService = mockCalculationService,
    financialDetailsService = mockFinancialDetailsService,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    nextUpdatesService = mockNextUpdatesService,
    auditingService = mockAuditingService,
    mockDateService
  )(appConfig,
    app.injector.instanceOf[LanguageUtils],
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[ExecutionContext],
    itvcErrorHandler = mockItvcErrorHandler
  )

  "forecast calculation tests" when {
    def runForecastTest(crystallised: Boolean, calcDataNotFound: Boolean = false, forecastCalcFeatureSwitchEnabled: Boolean, taxYear: Int = testYear,
                        shouldShowForecastData: Boolean): Unit = {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      if (forecastCalcFeatureSwitchEnabled)
        enable(ForecastCalculation)
      else disable(ForecastCalculation)
      mockSingleBusinessIncomeSource()
      if (crystallised) {
        mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = taxYear)
      } else if(calcDataNotFound) {
        mockCalculationNotFoundNew(nino = "AA111111A", year = taxYear)
      } else mockCalculationSuccessFullNotCrystallised(nino = "AA111111A", taxYear = taxYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(taxYear, "AA111111A")(financialDetailsModel(taxYear))
      mockgetNextUpdates(fromDate = LocalDate.of(taxYear - 1, 4, 6),
        toDate = LocalDate.of(taxYear, 4, 5))(
        response = testObligationsModel
      )

      val calcModel = if (crystallised) liabilityCalculationModelSuccessFull else liabilityCalculationModelSuccessFullNotCrystallised
      val calcOverview: Option[TaxYearSummaryViewModel] = if(calcDataNotFound) None else Some(TaxYearSummaryViewModel(calcModel))
      val expectedContent: String = taxYearSummaryView(
        taxYear,
        calcOverview,
        testChargesList,
        testObligationsModel,
        homeBackLink,
        codingOutEnabled = true,
        showForecastData = shouldShowForecastData,
        isAgent = true
      ).toString

      val result = controller.show(taxYear)(fakeRequestConfirmedClient())

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
    "tax year is current year" should {
      "show the Forecast tab" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = true, taxYear = 2020, shouldShowForecastData = true)
      }
    }
    "tax year is NOT current year" should {
      "NOT show the Forecast tab" in {
        runForecastTest(crystallised = false, forecastCalcFeatureSwitchEnabled = true, taxYear = 2017, shouldShowForecastData = false)
      }
    }
  }


  "the user is not authenticated" should {
    "redirect them to sign in" in {
      setupMockAgentAuthorisationException(withClientPredicate = false)

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithActiveSession)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
    }
  }
  "the user has timed out" should {
    "redirect to the session timeout page" in {
      setupMockAgentAuthorisationException(exception = BearerTokenExpired())

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithClientDetails)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
    }
  }
  "the user does not have an agent reference number" should {
    "return Ok with technical difficulties" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
      mockShowOkTechnicalDifficulties()

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithActiveSession)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
    }
  }
  "there was a problem retrieving income source details for the user" should {
    "throw an internal server exception" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockErrorIncomeSource()
      mockShowInternalServerError()
      val result = controller.show(taxYear = testYear)(fakeRequestConfirmedClient()).failed.futureValue
      result shouldBe an[InternalServerException]
      result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
    }
  }
  "there was a problem retrieving the calculation for the user" should {
    "return technical difficulties" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )
      mockCalculationErrorNew(nino = "AA111111A", year = testYear)
      mockShowInternalServerError()

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some(HTML)
    }
  }
  "there was a problem retrieving the charges for the user" should {
    "return technical difficulties" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(testFinancialDetailsErrorModel)
      mockShowInternalServerError()

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some(HTML)
    }
  }
  "there was a problem retrieving the updates for the user" should {
    "return technical difficulties" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR")
      )
      mockShowInternalServerError()

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some(HTML)
    }
  }
  "no calculation data was returned" should {
    "show the tax year summary page" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationNotFoundNew(nino = "AA111111A", year = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )
      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }
  }

  "all calls to retrieve data were successful" should {
    "show the tax year summary page" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }
  }

  "the coding out feature switch is enabled" should {
    "include Class 2 Nics in the charges list when Class 2 Nics is present" in {
      enable(CodingOut)

      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetails(
        documentDetails = documentDetailClass2Nic.documentDetail
      ))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
      val expectedContent: String = taxYearSummaryView(
        testYear,
        Some(calcOverview),
        class2NicsChargesList,
        testObligationsModel,
        taxYearsBackLink,
        isAgent = true,
        codingOutEnabled = true
      ).toString

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClientWithReferer(referer = taxYearsBackLink))

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedContent
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }

    "include Paye in the charges list when Paye is present" in {
      enable(CodingOut)

      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetails(
        documentDetails = documentDetailPaye.documentDetail
      ))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
      val expectedContent: String = taxYearSummaryView(
        testYear,
        Some(calcOverview),
        payeChargesList,
        testObligationsModel,
        taxYearsBackLink,
        isAgent = true,
        codingOutEnabled = true
      ).toString

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClientWithReferer(referer = taxYearsBackLink))

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedContent
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }
  }

  "the coding out feature switch is disabled" should {
    "not include Class 2 Nics in the charges list when Class 2 Nics is present" in {
      disable(CodingOut)

      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetails(
        documentDetails = documentDetailClass2Nic.documentDetail
      ))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
      val expectedContent: String = taxYearSummaryView(
        testYear,
        Some(calcOverview),
        testEmptyChargesList,
        testObligationsModel,
        taxYearsBackLink,
        isAgent = true,
        codingOutEnabled = true
      ).toString

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClientWithReferer(referer = taxYearsBackLink))

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedContent
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }

    "not include Paye in the charges list when Paye is present" in {
      disable(CodingOut)

      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetails(
        documentDetails = documentDetailPaye.documentDetail
      ))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
      val expectedContent: String = taxYearSummaryView(
        testYear,
        Some(calcOverview),
        testEmptyChargesList,
        testObligationsModel,
        taxYearsBackLink,
        isAgent = true,
        codingOutEnabled = true
      ).toString

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClientWithReferer(referer = taxYearsBackLink))

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedContent
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }
  }

  "all calls to retrieve data were successful and Referer was a Home page" should {
    "show the Tax Year Summary Page and back link should be to the Home page" in {
      enable(CodingOut)

      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockBothIncomeSources()
      mockCalculationSuccessFullNew(nino = "AA111111A", taxYear = testYear)
      setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetails(
        documentDetails = documentDetailClass2Nic.documentDetail
      ))
      mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
        ObligationsModel(Nil)
      )

      val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalculationModelSuccessFull)
      val expectedContent: String = taxYearSummaryView(
        testYear,
        Some(calcOverview),
        class2NicsChargesList,
        testObligationsModel,
        homeBackLink,
        isAgent = true,
        codingOutEnabled = true
      ).toString

      val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClientWithReferer(referer = homeBackLink))

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedContent
      result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
    }
  }
}
