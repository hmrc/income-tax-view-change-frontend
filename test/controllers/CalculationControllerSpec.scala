/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate
import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import assets.EstimatesTestConstants._
import assets.FinancialDetailsTestConstants.{documentDetailModel, documentDetailWithDueDateModel, fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import assets.FinancialTransactionsTestConstants.transactionModel
import assets.IncomeSourceDetailsTestConstants.singleBusinessIncome
import assets.MessagesLookUp
import audit.mocks.MockAuditingService
import audit.models.BillsAuditing.BillsAuditModel
import auth.MtdItUser
import config.ItvcErrorHandler
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, TaxYearOverviewUpdate}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockFinancialTransactionsService, MockReportDeadlinesService}
import models.calculation.CalcOverview
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail}
import models.reportDeadlines.{ObligationsModel, ReportDeadlinesErrorModel}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testUtils.TestSupport

class CalculationControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with MockFinancialTransactionsService with MockFinancialDetailsService
  with FeatureSwitching with MockAuditingService with MockReportDeadlinesService {

  object TestCalculationController extends CalculationController(
    MockAuthenticationPredicate,
    mockCalculationService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockFinancialTransactionsService,
    mockFinancialDetailsService,
    app.injector.instanceOf[ItvcErrorHandler],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockReportDeadlinesService,
    mockAuditingService
  )(appConfig,
    languageUtils,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ImplicitDateFormatterImpl])

  lazy val messagesLookUp = new MessagesLookUp.Calculation(testYear)

  val testIncomeBreakdown: Boolean = false
  val testDeductionBreakdown: Boolean = false
  val testTaxDue: Boolean = false
  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel)
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val testObligtionsModel: ObligationsModel = ObligationsModel(Nil)
  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/tax-years"

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYearOverviewUpdate)
    disable(NewFinancialDetailsApi)
  }


  "The CalculationController.renderTaxYearOverviewPage(year) action" when {

    "TaxYearOverviewUpdate FS is enabled" should {
      "show the updated Tax Year Overview Page" in {
        enable(TaxYearOverviewUpdate)
        mockSingleBusinessIncomeSource()
        mockCalculationSuccess()
        mockFinancialDetailsSuccess()
        mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6),
          toDate = LocalDate.of(testYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, Some(transactionModel()))
        val expectedContent: String = views.html.taxYearOverview(
          testYear,
          Some(calcOverview),
          testChargesList,
          testObligtionsModel,
          mockImplicitDateFormatter,
          taxYearsBackLink).toString


        val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
      }

      s"getFinancialDetails returns a $NOT_FOUND" should {
        "show the updated Tax Year Overview Page" in {
          enable(TaxYearOverviewUpdate)
          mockSingleBusinessIncomeSource()
          mockCalculationSuccess()
          mockFinancialDetailsNotFound()
          mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6),
            toDate = LocalDate.of(testYear, 4, 5))(
            response = testObligtionsModel
          )


          val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, Some(transactionModel()))
          val expectedContent: String = views.html.taxYearOverview(
            testYear,
            Some(calcOverview),
            testEmptyChargesList,
            testObligtionsModel,
            mockImplicitDateFormatter,
            taxYearsBackLink).toString

          val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe expectedContent
          contentType(result) shouldBe Some("text/html")
          result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
        }
      }

      "getFinancialDetails returns an error" should {
        "show the internal server error page" in {
          enable(TaxYearOverviewUpdate)
          mockSingleBusinessIncomeSource()
          mockCalculationCrystalisationSuccess()
          mockFinancialDetailsFailed()

          val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "getReportDeadlines returns an error" should {
        "show the internal server error page" in {
          enable(TaxYearOverviewUpdate)
          mockSingleBusinessIncomeSource()
          mockCalculationCrystalisationSuccess()
          mockFinancialDetailsNotFound()
          mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6),
            toDate = LocalDate.of(testYear, 4, 5))(
            response = ReportDeadlinesErrorModel(500, "INTERNAL_SERVER_ERROR")
          )

          val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "Called with an Unauthenticated User" should {
        "return redirect SEE_OTHER (303)" in {
          enable(TaxYearOverviewUpdate)
          setupMockAuthorisationException()

          val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
        }
      }

      "Called with an Authenticated HMRC-MTD-IT User" when {
        "provided with a negative tax year" should {
          "return Status Bad Request Error (400)" in {
            enable(TaxYearOverviewUpdate)
            mockPropertyIncomeSource()

            val result = TestCalculationController.renderTaxYearOverviewPage(-testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.BAD_REQUEST
          }
        }

        "the calculation returned from the calculation service was not found" should {
          "show tax year overview page with expected content" in {
            enable(TaxYearOverviewUpdate)
            mockSingleBusinessIncomeSource()
            mockCalculationNotFound()
            mockFinancialDetailsSuccess()
            mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6),
              toDate = LocalDate.of(testYear, 4, 5))(
              response = testObligtionsModel
            )

            val expectedContent: String = views.html.taxYearOverview(
              testYear,
              None,
              testChargesList,
              testObligtionsModel,
              mockImplicitDateFormatter,
              taxYearsBackLink).toString

            val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe expectedContent
            contentType(result) shouldBe Some("text/html")
            result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
          }
        }

        "the calculation returned from the calculation service was an error" should {
          "return the internal server error page" in {
            enable(TaxYearOverviewUpdate)
            mockSingleBusinessIncomeSource()
            mockCalculationError()

            val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentType(result) shouldBe Some("text/html")
          }
        }
      }

    }

    "TaxYearOverviewUpdate FS is disabled" should {
      "show the old Tax Year Overview Page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccess()

        val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, Some(transactionModel()))
        val expectedContent: String = views.html.taxYearOverviewOld(
          testYear, calcOverview, None, None, testIncomeBreakdown, testDeductionBreakdown, testTaxDue, mockImplicitDateFormatter, taxYearsBackLink).toString

        val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
      }


      "NewFinancialDetailsApi FS is disabled" when {
        "Called with an Unauthenticated User" should {
          "return redirect SEE_OTHER (303)" in {
            setupMockAuthorisationException()
            val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)
            status(result) shouldBe Status.SEE_OTHER
          }
        }

        "Called with an Authenticated HMRC-MTD-IT User" when {
          "provided with a negative tax year" should {
            "return Status Bad Request Error (400)" in {
              mockPropertyIncomeSource()

              val result = TestCalculationController.renderTaxYearOverviewPage(-testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.BAD_REQUEST
            }
          }

          "the calculation returned from the calculation service was not found" should {
            "return the internal server error page" in {
              mockSingleBusinessIncomeSource()
              mockCalculationNotFound()

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }

          "the calculation returned from the calculation service was an error" should {
            "return the internal server error page" in {
              mockSingleBusinessIncomeSource()
              mockCalculationError()

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }

          "the calculation returned from the calculation service is not crystallised" should {
            "return OK (200) with the correct view" in {
              mockSingleBusinessIncomeSource()
              mockCalculationSuccess()

              val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, None)
              val expectedContent: String = views.html.taxYearOverviewOld(
                testYear, calcOverview, None, None, testIncomeBreakdown, testDeductionBreakdown, testTaxDue, mockImplicitDateFormatter, taxYearsBackLink).toString

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.OK
              contentAsString(result) shouldBe expectedContent
              contentType(result) shouldBe Some("text/html")

              lazy val expectedTestMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
                singleBusinessIncome, None, Some("credId"), Some("Individual"))(FakeRequest())

              verifyExtendedAudit(BillsAuditModel(expectedTestMtdItUser, BigDecimal(2010.00)))
            }
          }

          "the calculation returned from the calculation service is crystallised" when {
            "the financial transaction returned from the service was an error" should {
              "return the internal server error page" in {
                mockSingleBusinessIncomeSource()
                mockCalculationCrystalisationSuccess()
                mockFinancialTransactionFailed()

                val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
                contentType(result) shouldBe Some("text/html")
              }
            }

            "the financial transaction returned from the service is successful" should {
              "return OK (200) with the correct view" in {
                mockSingleBusinessIncomeSource()
                mockCalculationCrystalisationSuccess()
                mockFinancialTransactionSuccess()

                val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, Some(transactionModel()))
                val expectedContent: String = views.html.taxYearOverviewOld(
                  testYear, calcOverview, Some(transactionModel()), None, testIncomeBreakdown, testDeductionBreakdown, testTaxDue, mockImplicitDateFormatter, taxYearsBackLink).toString

                val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
              }
            }
          }
        }
      }

      "NewFinancialDetailsApi FS is enabled" when {
        "Called with an Unauthenticated User" should {
          "return redirect SEE_OTHER (303)" in {
            enable(NewFinancialDetailsApi)
            setupMockAuthorisationException()
            val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)
            status(result) shouldBe Status.SEE_OTHER
          }
        }

        "Called with an Authenticated HMRC-MTD-IT User" when {
          "provided with a negative tax year" should {
            "return Status Bad Request Error (400)" in {
              enable(NewFinancialDetailsApi)
              mockPropertyIncomeSource()

              val result = TestCalculationController.renderTaxYearOverviewPage(-testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.BAD_REQUEST
            }
          }

          "the calculation returned from the calculation service was not found" should {
            "return the internal server error page" in {
              enable(NewFinancialDetailsApi)
              mockSingleBusinessIncomeSource()
              mockCalculationNotFound()

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }

          "the calculation returned from the calculation service was an error" should {
            "return the internal server error page" in {
              enable(NewFinancialDetailsApi)
              mockSingleBusinessIncomeSource()
              mockCalculationError()

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }

          "the calculation returned from the calculation service is not crystallised" should {
            "return OK (200) with the correct view" in {
              enable(NewFinancialDetailsApi)
              mockSingleBusinessIncomeSource()
              mockCalculationSuccess()

              val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, None)
              val expectedContent: String = views.html.taxYearOverviewOld(
                testYear, calcOverview, None, None, testIncomeBreakdown, testDeductionBreakdown, testTaxDue, mockImplicitDateFormatter, taxYearsBackLink).toString

              val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

              status(result) shouldBe Status.OK
              contentAsString(result) shouldBe expectedContent
              contentType(result) shouldBe Some("text/html")

              lazy val expectedTestMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
                singleBusinessIncome, None, Some("credId"), Some("Individual"))(FakeRequest())

              verifyExtendedAudit(BillsAuditModel(expectedTestMtdItUser, BigDecimal(2010.00)))
            }
          }

          "the calculation returned from the calculation service is crystallised" when {
            "the financial details returned from the service was an error" should {
              "return the internal server error page" in {
                enable(NewFinancialDetailsApi)
                mockSingleBusinessIncomeSource()
                mockCalculationCrystalisationSuccess()
                mockFinancialDetailsFailed()

                val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
                contentType(result) shouldBe Some("text/html")
              }
            }

            "the financial details returned from the service is successful" should {
              "return OK (200) with the correct view" in {
                enable(NewFinancialDetailsApi)
                mockSingleBusinessIncomeSource()
                mockCalculationCrystalisationSuccess()
                mockFinancialDetailsSuccess()

                val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel, None)
                val expectedContent: String = views.html.taxYearOverviewOld(
                  testYear, calcOverview, None, Some(documentDetailWithDueDateModel()), testIncomeBreakdown, testDeductionBreakdown, testTaxDue, mockImplicitDateFormatter, taxYearsBackLink).toString

                val result = TestCalculationController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

                status(result) shouldBe Status.OK
                contentAsString(result) shouldBe expectedContent
                contentType(result) shouldBe Some("text/html")
              }
            }
          }
        }
      }
    }
  }
}
