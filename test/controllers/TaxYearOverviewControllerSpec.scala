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

import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import assets.EstimatesTestConstants._
import assets.FinancialDetailsTestConstants.fullDocumentDetailWithDueDateModel
import assets.MessagesLookUp
import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockNextUpdatesService}
import models.calculation.CalcOverview
import models.financialDetails.DocumentDetailWithDueDate
import models.nextUpdates.{ObligationsModel, NextUpdatesErrorModel}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.TaxYearOverview
import views.html.errorPages.StandardError

import java.time.LocalDate

class TaxYearOverviewControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with MockFinancialDetailsService with FeatureSwitching
  with MockAuditingService with MockNextUpdatesService {

  val taxYearOverviewView = app.injector.instanceOf[TaxYearOverview]

  object TestTaxYearOverviewController extends TaxYearOverviewController(
    taxYearOverviewView,
    app.injector.instanceOf[StandardError],
    MockAuthenticationPredicate,
    mockCalculationService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockFinancialDetailsService,
    app.injector.instanceOf[ItvcErrorHandler],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockNextUpdatesService,
    mockAuditingService
  )(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  lazy val messagesLookUp = new MessagesLookUp.Calculation(testYear)

  val testChargesList: List[DocumentDetailWithDueDate] = List(fullDocumentDetailWithDueDateModel.copy(
    dueDate = fullDocumentDetailWithDueDateModel.documentDetail.interestEndDate, isLatePaymentInterest = true),
    fullDocumentDetailWithDueDateModel)
  val testEmptyChargesList: List[DocumentDetailWithDueDate] = List.empty
  val testObligtionsModel: ObligationsModel = ObligationsModel(Nil)
  val taxYearsBackLink: String = "/report-quarterly/income-and-expenses/view/tax-years"


  "The TaxYearOverview.renderTaxYearOverviewPage(year) action" when {

    "TaxYearOverviewUpdate FS is enabled" should {
      "show the updated Tax Year Overview Page" in {
        mockSingleBusinessIncomeSource()
        mockCalculationSuccess()
        mockFinancialDetailsSuccess()
        mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6),
          toDate = LocalDate.of(testYear, 4, 5))(
          response = testObligtionsModel
        )

        val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel)
        val expectedContent: String = taxYearOverviewView(
          testYear,
          Some(calcOverview),
          testChargesList,
          testObligtionsModel,
          taxYearsBackLink).toString


        val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        contentType(result) shouldBe Some("text/html")
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
      }

      s"getFinancialDetails returns a $NOT_FOUND" should {
        "show the updated Tax Year Overview Page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationSuccess()
          mockFinancialDetailsNotFound()
          mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6),
            toDate = LocalDate.of(testYear, 4, 5))(
            response = testObligtionsModel
          )


          val calcOverview: CalcOverview = CalcOverview(calculationDataSuccessModel)
          val expectedContent: String = taxYearOverviewView(
            testYear,
            Some(calcOverview),
            testEmptyChargesList,
            testObligtionsModel,
            taxYearsBackLink).toString

          val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe expectedContent
          contentType(result) shouldBe Some("text/html")
          result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
        }
      }

      "getFinancialDetails returns an error" should {
        "show the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationCrystalisationSuccess()
          mockFinancialDetailsFailed()

          val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "getNextUpdates returns an error" should {
        "show the internal server error page" in {
          mockSingleBusinessIncomeSource()
          mockCalculationCrystalisationSuccess()
          mockFinancialDetailsNotFound()
          mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6),
            toDate = LocalDate.of(testYear, 4, 5))(
            response = NextUpdatesErrorModel(500, "INTERNAL_SERVER_ERROR")
          )

          val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some("text/html")
        }
      }

      "Called with an Unauthenticated User" should {
        "return redirect SEE_OTHER (303)" in {
          setupMockAuthorisationException()

          val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
        }
      }

      "Called with an Authenticated HMRC-MTD-IT User" when {
        "provided with a negative tax year" should {
          "return Status Bad Request Error (400)" in {
            mockPropertyIncomeSource()

            val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(-testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.BAD_REQUEST
          }
        }

        "the calculation returned from the calculation service was not found" should {
          "show tax year overview page with expected content" in {
            mockSingleBusinessIncomeSource()
            mockCalculationNotFound()
            mockFinancialDetailsSuccess()
            mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6),
              toDate = LocalDate.of(testYear, 4, 5))(
              response = testObligtionsModel
            )

            val expectedContent: String = taxYearOverviewView(
              testYear,
              None,
              testChargesList,
              testObligtionsModel,
              taxYearsBackLink).toString

            val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe expectedContent
            contentType(result) shouldBe Some("text/html")
            result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("taxYearOverview")
          }
        }

        "the calculation returned from the calculation service was an error" should {
          "return the internal server error page" in {
            mockSingleBusinessIncomeSource()
            mockCalculationError()

            val result = TestTaxYearOverviewController.renderTaxYearOverviewPage(testYear)(fakeRequestWithActiveSession)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentType(result) shouldBe Some("text/html")
          }
        }
      }

    }
  }
}
