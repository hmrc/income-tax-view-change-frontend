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

import connectors.{BusinessDetailsConnector, CalculationListConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import mocks.auth.MockAuthActions
import mocks.connectors.MockIncomeTaxCalculationConnector
import mocks.services.{MockCalculationService, MockClaimToAdjustService, MockFinancialDetailsService, MockNextUpdatesService}
import models.admin._
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{LiabilityCalculationError, Message, Messages}
import models.obligations._
import models.taxyearsummary.{MtdSoftware, TaxYearSummaryChargeItem}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, Status}
import play.api.test.Helpers.{status, _}
import services._
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{financialDetails, _}
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelErrorMessagesForIndividual, liabilityCalculationModelSuccessful, liabilityCalculationModelSuccessfulNotCrystallised}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import views.html.TaxYearSummaryView

import java.time.LocalDate
import scala.concurrent.Future

class TaxYearSummaryControllerSpec
  extends MockAuthActions
    with MockCalculationService
    with MockFinancialDetailsService
    with MockNextUpdatesService
    with MockIncomeTaxCalculationConnector
    with MockClaimToAdjustService
    with ChargeConstants {

  lazy val mockCalculationListConnector: CalculationListConnector = mock(classOf[CalculationListConnector])
  lazy val mockTaxYearSummaryService: TaxYearSummaryService = mock(classOf[TaxYearSummaryService])

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[CalculationService].toInstance(mockCalculationService),
        api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService),
        api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
        api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[CalculationListConnector].toInstance(mockCalculationListConnector),
        api.inject.bind[TaxYearSummaryService].toInstance(mockTaxYearSummaryService),
      ).build()

  lazy val taxYearSummaryView: TaxYearSummaryView = app.injector.instanceOf[TaxYearSummaryView]

  lazy val testController: TaxYearSummaryController = app.injector.instanceOf[TaxYearSummaryController]

  val testCharge: ChargeItem = chargeItemModel()

  val testChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(testCharge.copy(accruingInterestAmount = None)),
    TaxYearSummaryChargeItem.fromChargeItem(testCharge, dueDate = testCharge.interestEndDate, isLatePaymentInterest = true)
  )

  val testEmptyChargesList: List[TaxYearSummaryChargeItem] = List.empty
  val class2NicsChargesList: List[TaxYearSummaryChargeItem] = List(chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), accruingInterestAmount = None)).map(TaxYearSummaryChargeItem.fromChargeItem)
  val payeChargesList: List[TaxYearSummaryChargeItem] = List(chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted), accruingInterestAmount = None)).map(TaxYearSummaryChargeItem.fromChargeItem)
  val taxYearsRefererBackLink: String = "http://www.somedomain.org/report-quarterly/income-and-expenses/view/tax-years"
  val taxYearsBackLink: Boolean => String = isAgent => {
    if (isAgent) {
      controllers.routes.TaxYearsController.showAgentTaxYears().url
    } else {
      controllers.routes.TaxYearsController.showTaxYears(None).url
    }
  }
  val homeBackLink: Boolean => String = isAgent => {
    "/report-quarterly/income-and-expenses/view" + {
      if (isAgent) "/agents/client-income-tax" else ""
    }
  }
  val emptyCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(None)
  val populatedCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(Some(TaxYear(2023, 2024)))
  lazy val ctaLink: Boolean => String = isAgent => {
    "/report-quarterly/income-and-expenses/view" + {
      if (isAgent) "/agents" else ""
    } + "/adjust-poa/start"
  }

  val testObligtionsModel: ObligationsModel =
    ObligationsModel(Seq(
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

  mtdAllRoles.foreach { mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.renderAgentTaxYearSummaryPage(testTaxYear) else testController.renderTaxYearSummaryPage(testTaxYear)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      .withHeaders(HeaderNames.REFERER -> taxYearsBackLink(isAgent))

    s"render${if (isAgent) "Agent"}TaxYearSummaryPage" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the tax year summary page" that {

            "shows calculations tabs" when {

              "downstream returns only a latest calculation" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid, taxYear = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                mockGetNextUpdates(
                  fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }

              "downstream returns both the latest and previous calculations" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccessWithAmendment(testMtditid, taxYear = testTaxYear, previousCalc = Some(liabilityCalculationModelSuccessful))
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }

              "downstream returns both the latest calculation but the previous calculation doesn't exist" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccessWithAmendment(testMtditid, taxYear = testTaxYear, previousCalc = Some(LiabilityCalculationError(204, "not found")))
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }
            }

            "shows the Forecast tab before crystallisation" when {

              "crystallised is false and the show forecast data is true" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousNotCrystallised(testMtditid, taxYear = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))
                val taxYearSummary = TaxYearSummaryViewModel(
                  Some(CalculationSummary(liabilityCalculationModelSuccessfulNotCrystallised)),
                  None,
                  testChargesList,
                  testObligtionsModel,
                  showForecastData = true,
                  ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                  pfaEnabled = false
                )
                val expectedContent: String =
                  taxYearSummaryView(
                    testTaxYear, taxYearSummary,
                    taxYearsBackLink(isAgent),
                    ctaLink = ctaLink(isAgent),
                    isAgent = isAgent,
                    taxYearViewScenarios = MtdSoftware,
                    viewTaxCalcLink = Some("some fake url"),
                    selfAssessmentLink = "some fake url",
                    contactHmrcLink = "some fake url",
                  ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }
            }
            "does NOT show the Forecast tab after crystallisation" when {

              "crystallisation is true and show forecast data is false" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid, taxYear = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(CalculationSummary(liabilityCalculationModelSuccessful)),
                    None,
                    testChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = ""
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
              }
            }

            "shows the Forecast tab" when {

              "no calc data is returned" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousNotFound(testMtditid, taxYear = testTaxYear)
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))
                mockFinancialDetailsSuccess(taxYear = testTaxYear)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    None,
                    None,
                    testChargesList,
                    testObligtionsModel,
                    showForecastData = true,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
              }
            }

            "has the poa section" when {

              "POAs are for the tax year on the page" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess()

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe true
              }

              "FilterCodedOutPoas FS is enabled and there are some not coded out" in {
                enable(FilterCodedOutPoas)
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = None))

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe true
              }

              "there are coded out POA charges but FilterCodedOutPoas FS disabled" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = Some(100)))

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe true
              }
            }
            "doesn't have a the poa section" when {
              "POAs are for the tax year of a different year" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess()

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe true
              }
              "There are no valid POAs" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess()

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe false
              }

              "FilterCodedOutPoas FS is enabled and POA charges are coded out" in {
                enable(FilterCodedOutPoas)
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = Some(100)))

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe false
              }
            }

            "Review and Reconcile debit charges in the charges table" when {
              "the user has Review and Reconcile debit charges" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithReviewAndReconcileDebits)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                def chargeSummaryUrl(id: String) = if (isAgent) {
                  controllers.routes.ChargeSummaryController.showAgent(testTaxYear, id).url
                } else {
                  controllers.routes.ChargeSummaryController.show(testTaxYear, id).url
                }

                status(result) shouldBe OK
                Jsoup.parse(contentAsString(result)).getElementById("accrues-interest-tag").text() shouldBe "Accrues interest"
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-0").text() shouldBe "First payment on account: extra amount from your tax return"
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-0").attr("href") shouldBe chargeSummaryUrl("RARDEBIT01")
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-1").text() shouldBe "Second payment on account: extra amount from your tax return"
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-1").attr("href") shouldBe chargeSummaryUrl("RARDEBIT02")
              }
            }

            "Penalties in the charges table" when {

              "the user has penalties and the penalties FS is enabled" in {

                enable(PenaltiesAndAppeals)
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithAllThreePenalties)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                def chargeSummaryUrl(id: String) = if (isAgent) {
                  controllers.routes.ChargeSummaryController.showAgent(testTaxYear, id).url
                } else {
                  controllers.routes.ChargeSummaryController.show(testTaxYear, id).url
                }

                status(result) shouldBe OK
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-0").text() shouldBe "Late submission penalty"
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-0").attr("href") shouldBe chargeSummaryUrl("LSP")
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-1").text() shouldBe "First late payment penalty"
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-1").attr("href") shouldBe chargeSummaryUrl("LPP1")
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-2").text() shouldBe "Second late payment penalty"
                if (isAgent) {
                  Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-2").attr("href") shouldBe appConfig.incomeTaxPenaltiesFrontendLPP2CalculationAgent("chargeRef123")
                } else {
                  Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-2").attr("href") shouldBe appConfig.incomeTaxPenaltiesFrontendLPP2Calculation("chargeRef123")
                }
              }
            }
            "Not show penalties in the charges table" when {

              "the penalties FS is disabled" in {
                disable(PenaltiesAndAppeals)
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithAllThreePenalties)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val result = action(fakeRequest)

                Option(Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-0")) shouldBe None
              }
            }

            "has a back link to the home page" in {

              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockLatestAndPreviousSuccess(testMtditid)
              mockFinancialDetailsSuccess()

              mockGetNextUpdates(
                fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5))(
                response = testObligtionsModel
              )

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              setupMockGetPoaTaxYearForEntryPointCall(Right(None))

              when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                .thenReturn(MtdSoftware)

              val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
              val expectedContent: String =
                taxYearSummaryView(
                  taxYear = testTaxYear,
                  viewModel = TaxYearSummaryViewModel(
                    calculationSummary = Some(calcOverview),
                    previousCalculationSummary = None,
                    charges = testChargesList,
                    obligations = testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel,
                    LPP2Url = "",
                    pfaEnabled = false),
                  backUrl = homeBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",


                ).toString

              val result = action(fakeGetRequestBasedOnMTDUserType(mtdUserRole))
              status(result) shouldBe Status.OK
              contentAsString(result) shouldBe expectedContent
              contentType(result) shouldBe Some("text/html")
              result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
              result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
            }

            "include Class 2 Nics in the charges list" when {
              "Class 2 Nics is present" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(
                  financialDetailsModelResponse = financialDetails(
                    documentDetails = documentDetailClass2Nic.documentDetail,
                    financialDetails = financialDetail(mainTransaction = "4910")
                  )
                )

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(calcOverview),
                    None,
                    class2NicsChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",


                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "include Paye in the charges list" when {

              "Paye is present" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(
                  financialDetailsModelResponse = financialDetails(
                    documentDetails = documentDetailPaye.documentDetail,
                    financialDetails = financialDetail(mainTransaction = "4910", codedOutStatus = Some("I"))
                  )
                )

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(calcOverview),
                    None,
                    payeChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = ""
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "displays MFA debits" when {

              "the user has MFA debit charge" in {

                setupMockSuccess(mtdUserRole)
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = MFADebitsFinancialDetails)

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val mfaCharges: List[TaxYearSummaryChargeItem] = List(
                  chargeItemModel(transactionId = "MFADEBIT01", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None),
                  chargeItemModel(transactionId = "MFADEBIT02", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None),
                  chargeItemModel(transactionId = "MFADEBIT03", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None)
                ).map(TaxYearSummaryChargeItem.fromChargeItem)

                val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
                val charges = mfaCharges
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear,
                  TaxYearSummaryViewModel(
                    Some(calcOverview),
                    None,
                    charges,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "no charges" when {
              "the financial charges returns not found" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousSuccess(testMtditid)
                mockFinancialDetailsNotFound()
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(calcOverview),
                    None,
                    testEmptyChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",


                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }
            }

            "does not have calculation summary" when {

              "calculation service returned not found" in {

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockLatestAndPreviousNotFound(testMtditid)
                mockFinancialDetailsSuccess()

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                val expectedContent: String = Jsoup.parse(taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    None,
                    None,
                    testChargesList,
                    testObligtionsModel,
                    showForecastData = true,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",
                ).toString()).text()

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                Jsoup.parse(contentAsString(result)).text() shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }
            }

            "show the Tax Year Summary Page with error messages" when {
              "liability Calculation has error messages" in {

                disable(NavBarFs)
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))
                mockLatestAndPreviousWithErrorMessages(testMtditid)
                mockFinancialDetailsSuccess()

                when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                  .thenReturn(Future.successful(singleBusinessIncome))

                mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val errorMessageVariableValues = testController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = false)(messages)
                val calcOverview: CalculationSummary = CalculationSummary(errorMessageVariableValues)

                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(calcOverview),
                    None,
                    testChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel, LPP2Url = "",
                    pfaEnabled = false),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent,
                  taxYearViewScenarios = MtdSoftware,
                  viewTaxCalcLink = Some("some fake url"),
                  selfAssessmentLink = "some fake url",
                  contactHmrcLink = "some fake url",


                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
                result.futureValue.session.get(gatewayPage) shouldBe Some("taxYearSummary")
                result.futureValue.session.get(calcPagesBackPage) shouldBe Some("ITVC")
              }
            }
          }

          "render the error page" when {
            "getPoaTaxYearForEntryPoint returns an error" in {

              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              setupMockGetPoaTaxYearForEntryPointCall(Left(new Exception("TEST")))
              mockFinancialDetailsSuccess()

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                .thenReturn(MtdSoftware)

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "getFinancialDetails returns an error" in {

              setupMockSuccess(mtdUserRole)
              mockFinancialDetailsFailed()

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            "getNextUpdates returns an error" in {

              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockFinancialDetailsNotFound()

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                .thenReturn(MtdSoftware)

              mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5))(
                response = ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
              )

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            if (mtdUserRole == MTDIndividual) {
              "provided with a negative tax year" in {
                setupMockSuccess(mtdUserRole)
                mockPropertyIncomeSource()

                when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                  .thenReturn(MtdSoftware)

                val invalidTaxYearAction =
                  if (isAgent) {
                    testController.renderAgentTaxYearSummaryPage(-testTaxYear)
                  } else {
                    testController.renderTaxYearSummaryPage(-testTaxYear)
                  }

                val result = invalidTaxYearAction(fakeRequest)
                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              }
            }
            "the calculation returned from the calculation service was an error" in {

              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockFinancialDetailsSuccess()

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              mockGetNextUpdates(
                fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5)
              )(response = testObligtionsModel)

              mockCalculationErrorNew(testMtditid)

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            "user has a second late payment penalty without a chargeReference, so url cannot be generated" in {

              enable(PenaltiesAndAppeals)
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockLatestAndPreviousSuccess(testMtditid)
              mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithLPP2NoChargeRef)

              when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
                .thenReturn(Future.successful(singleBusinessIncome))

              when(mockTaxYearSummaryService.determineCannotDisplayCalculationContentScenario(any(), any())(any()))
                .thenReturn(MtdSoftware)

              mockGetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5))(
                response = testObligtionsModel
              )

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }

    if (mtdUserRole != MTDSupportingAgent) {
      "formatErrorMessages" when {
        s"the $mtdUserRole is authenticated" that {
          "has liability Calculation error messages" should {
            "filter out the variable value from messages" in {

              val actual = testController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = isAgent)(messages)

              actual shouldBe
                liabilityCalculationModelErrorMessagesForIndividual.copy(messages = Some(Messages(
                  errors = Some(List(
                    Message("C55012", "5 January 2023"),
                    Message("C15507", "£2000"),
                    Message("C15510", "10"),
                    Message("C55009", ""),
                  ))
                )))
            }
          }
        }
      }
    }
  }
}
