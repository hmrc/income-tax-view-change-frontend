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

import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import mocks.auth.MockAuthActions
import mocks.connectors.MockIncomeTaxCalculationConnector
import mocks.services.{MockCalculationService, MockClaimToAdjustService, MockFinancialDetailsService, MockNextUpdatesService}
import models.admin._
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{Message, Messages}
import models.obligations._
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.jsoup.Jsoup
import play.api
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, Status}
import play.api.test.Helpers.{status, _}
import services.{CalculationService, ClaimToAdjustService, FinancialDetailsService, NextUpdatesService}
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{financialDetails, _}
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelErrorMessagesForIndividual, liabilityCalculationModelSuccessful, liabilityCalculationModelSuccessfulNotCrystallised}
import views.html.TaxYearSummary

import java.time.LocalDate

class TaxYearSummaryControllerSpec extends MockAuthActions with MockCalculationService
  with MockFinancialDetailsService with MockNextUpdatesService with MockIncomeTaxCalculationConnector
  with MockClaimToAdjustService with ChargeConstants {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService),
      api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService)
    ).build()

  lazy val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  lazy val testController = app.injector.instanceOf[TaxYearSummaryController]

  val testCharge = chargeItemModel()

  val testChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(testCharge.copy(latePaymentInterestAmount = None)),
    TaxYearSummaryChargeItem.fromChargeItem(testCharge, dueDate = testCharge.interestEndDate, isLatePaymentInterest = true)
  )

  val testEmptyChargesList: List[TaxYearSummaryChargeItem] = List.empty
  val class2NicsChargesList: List[TaxYearSummaryChargeItem] = List(chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), latePaymentInterestAmount = None)).map(TaxYearSummaryChargeItem.fromChargeItem)
  val payeChargesList: List[TaxYearSummaryChargeItem] = List(chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted), latePaymentInterestAmount = None)).map(TaxYearSummaryChargeItem.fromChargeItem)
  val taxYearsRefererBackLink: String = "http://www.somedomain.org/report-quarterly/income-and-expenses/view/tax-years"
  val taxYearsBackLink: Boolean => String = isAgent => {
    if (isAgent) {
      controllers.routes.TaxYearsController.showAgentTaxYears().url
    } else {
      controllers.routes.TaxYearsController.showTaxYears(None).url
    }
  }
  val homeBackLink: Boolean => String = isAgent => {
    "/report-quarterly/income-and-expenses/view" + {if(isAgent) "/agents/client-income-tax" else ""}
  }
  val emptyCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, None)
  val populatedCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = true, Some(TaxYear(2023, 2024)))
  lazy val ctaLink: Boolean => String = isAgent => {
    "/report-quarterly/income-and-expenses/view" + {if (isAgent) "/agents" else ""} + "/adjust-poa/start"
  }

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

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.renderAgentTaxYearSummaryPage(testTaxYear) else testController.renderTaxYearSummaryPage(testTaxYear)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      .withHeaders(
        HeaderNames.REFERER -> taxYearsBackLink(isAgent)
      )
    s"render${if (isAgent) "Agent"}TaxYearSummaryPage" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the tax year summary page" that {
            "shows the Forecast tab before crystallisation" when {
              "crystallised is false and the show forecast data is true" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNotCrystallised(testMtditid, taxYear = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                val taxYearSummary = TaxYearSummaryViewModel(
                  Some(CalculationSummary(liabilityCalculationModelSuccessfulNotCrystallised)),
                  testChargesList,
                  testObligtionsModel,
                  showForecastData = true,
                  ctaViewModel = emptyCTAViewModel
                )
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, taxYearSummary,
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
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
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid, taxYear = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(CalculationSummary(liabilityCalculationModelSuccessful)),
                    testChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
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
                mockSingleBusinessIncomeSource()
                mockCalculationNotFoundNew(testMtditid, year = testTaxYear)
                mockFinancialDetailsSuccess(taxYear = testTaxYear)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    None,
                    testChargesList,
                    testObligtionsModel,
                    showForecastData = true,
                    ctaViewModel = emptyCTAViewModel
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
              }
            }
            "has the poa section" when {
              "AdjustPaymentsOnAccount FS is enabled and POAs are for the tax year on the page" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess()
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe true
              }

              "FilterCodedOutPoas FS is enabled and there are some not coded out" in {
                enable(AdjustPaymentsOnAccount)
                enable(FilterCodedOutPoas)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = None))
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe true
              }

              "there are coded out POA charges but FilterCodedOutPoas FS disabled" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = Some(100)))
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe true
              }
            }
            "doesn't have a the poa section" when {
              "AdjustPaymentsOnAccount FS is enabled and POAs are for the tax year of a different year" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess()
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)

                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe true
              }
              "AdjustPaymentsOnAccount FS is enabled and there are no valid POAs" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess()
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                val result = action(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe false
              }
              "AdjustPaymentsOnAccount FS is disabled" in {
                disable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess()
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result).contains("Adjust payments on account") shouldBe false
              }

              "FilterCodedOutPoas FS is enabled and POA charges are coded out" in {
                enable(AdjustPaymentsOnAccount)
                enable(FilterCodedOutPoas)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModel(amountCodedOut = Some(100)))
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                val result = action(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result).contains("First payment on account") shouldBe false
              }
            }

            "Review and Reconcile debit charges in the charges table" when {
              "the user has Review and Reconcile debit charges" in {
                enable(ReviewAndReconcilePoa)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithReviewAndReconcileDebits)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                val result = action(fakeRequest)
                def chargeSummaryUrl(id: String) = if(isAgent) {
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

            "does not Review and Reconcile debit charges in the charges table" when {
              "tReviewAndReconcilePoa FS is disabled" in {
                disable(ReviewAndReconcilePoa)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithReviewAndReconcileDebitsOverdue)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                val result = action(fakeRequest)
                status(result) shouldBe OK
                Option(Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-0")).isDefined shouldBe false
                Option(Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-0")).isDefined shouldBe false
                Option(Jsoup.parse(contentAsString(result)).getElementById("paymentTypeText-1")).isDefined shouldBe false
                Option(Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-1")).isDefined shouldBe false
              }
            }

            "Penalties in the charges table" when {
              "the user has penalties and the penalties FS is enabled" in {
                enable(PenaltiesAndAppeals)
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = financialDetailsWithAllThreePenalties)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                val result = action(fakeRequest)
                def chargeSummaryUrl(id: String) = if(isAgent) {
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
                Jsoup.parse(contentAsString(result)).getElementById("paymentTypeLink-2").attr("href") shouldBe ""
              }
            }
            "Not show penalties in the charges table" when {
              "the penalties FS is disabled" in {

              }
            }

            "has a back link to the home page" in {
              setupMockSuccess(mtdUserRole)
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
                  ctaViewModel = emptyCTAViewModel),
                homeBackLink(isAgent),
                ctaLink = ctaLink(isAgent),
                isAgent = isAgent).toString

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
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(
                  financialDetailsModelResponse = financialDetails(
                    documentDetails = documentDetailClass2Nic.documentDetail,
                    financialDetails = financialDetail(mainTransaction = "4910")
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
                    ctaViewModel = emptyCTAViewModel),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "include Paye in the charges list" when {
              "Paye is present" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(
                  financialDetailsModelResponse = financialDetails(
                    documentDetails = documentDetailPaye.documentDetail,
                    financialDetails = financialDetail(mainTransaction = "4910")
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
                    ctaViewModel = emptyCTAViewModel),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "displays MFA debits" when {
              "the user has MFA debit charge" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBusinessIncomeSource()
                mockCalculationSuccessfulNew(testMtditid)
                mockFinancialDetailsSuccess(financialDetailsModelResponse = MFADebitsFinancialDetails)
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )

                val mfaCharges: List[TaxYearSummaryChargeItem] = List(
                  chargeItemModel(transactionId = "MFADEBIT01", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, latePaymentInterestAmount = None),
                  chargeItemModel(transactionId = "MFADEBIT02", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, latePaymentInterestAmount = None),
                  chargeItemModel(transactionId = "MFADEBIT03", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, latePaymentInterestAmount = None)
                ).map(TaxYearSummaryChargeItem.fromChargeItem)

                val calcOverview: CalculationSummary = CalculationSummary(liabilityCalculationModelSuccessful)
                val charges = mfaCharges
                val expectedContent: String = taxYearSummaryView(
                  testTaxYear,
                  TaxYearSummaryViewModel(
                    Some(calcOverview),
                    charges,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel
                  ),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
                ).toString

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
              }
            }

            "no charges" when {
              "the finincial charges returns not found" in {
                setupMockSuccess(mtdUserRole)
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
                    ctaViewModel = emptyCTAViewModel),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
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
                    showForecastData = true,
                    ctaViewModel = emptyCTAViewModel),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent
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
                mockSingleBusinessIncomeSource()
                mockCalculationWithErrorMessages(testMtditid)
                mockFinancialDetailsSuccess()
                mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                  toDate = LocalDate.of(testTaxYear, 4, 5))(
                  response = testObligtionsModel
                )
                val errorMessageVariableValues = testController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = false)(messages)
                val calcOverview: CalculationSummary = CalculationSummary(errorMessageVariableValues)

                val expectedContent: String = taxYearSummaryView(
                  testTaxYear, TaxYearSummaryViewModel(
                    Some(calcOverview),
                    testChargesList,
                    testObligtionsModel,
                    ctaViewModel = emptyCTAViewModel),
                  taxYearsBackLink(isAgent),
                  ctaLink = ctaLink(isAgent),
                  isAgent = isAgent).toString

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
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              setupMockGetPoaTaxYearForEntryPointCall(Left(new Exception("TEST")))
              mockFinancialDetailsSuccess()

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "getFinancialDetails returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              mockFinancialDetailsFailed()

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            "getNextUpdates returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()
              mockFinancialDetailsNotFound()
              mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5))(
                response = ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
              )

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            if(mtdUserRole == MTDIndividual) {
              "provided with a negative tax year" in {
                setupMockSuccess(mtdUserRole)
                mockPropertyIncomeSource()

                val invalidTaxYearAction = if (isAgent) {
                  testController.renderAgentTaxYearSummaryPage(-testTaxYear)
                } else {
                  testController.renderTaxYearSummaryPage(-testTaxYear)
                }

                val result = invalidTaxYearAction(fakeRequest)
                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              }
            }
            "the calculation returned from the calculation service was an error" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdUserRole)
              mockSingleBusinessIncomeSource()

              mockFinancialDetailsSuccess()
              mockgetNextUpdates(fromDate = LocalDate.of(testTaxYear - 1, 4, 6),
                toDate = LocalDate.of(testTaxYear, 4, 5))(
                response = testObligtionsModel)

              mockCalculationErrorNew(testMtditid)

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }

    if (mtdUserRole != MTDSupportingAgent) {
      "formatErrorMessages" when {
        s"the $mtdUserRole is authenticated" that {
          "has liability Calculation error messages" should {
            "filter out the variable value from messages" in {
              val actual = testController.formatErrorMessages(liabilityCalculationModelErrorMessagesForIndividual, messagesApi, isAgent = isAgent)(messages)

              actual shouldBe liabilityCalculationModelErrorMessagesForIndividual.copy(messages = Some(Messages(
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
