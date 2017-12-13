/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.Messages
import assets.Messages.EstimatedTaxLiabilityError
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants.PropertyDetails._
import assets.TestConstants._
import audit.AuditingService
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockServiceInfoPartialService}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class CalculationControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockServiceInfoPartialService {

  object TestCalculationController extends CalculationController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    mockServiceInfoPartialService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService]
  )

  lazy val messages = new Messages.Calculation(2018)

  "The CalculationController.getFinancialData(year) action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      "that successfully retrieves Business only income from the Income Sources predicate" +
        "and financial data from the FinancialData Service" should {

        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          mockFinancialDataSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018IncomeSourceSuccess)
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives Property only income from the Income Sources predicate" +
        "and financial data from the FinancialData Service" should {

        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          mockPropertyIncomeSource()
          mockFinancialDataSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives a crystalised payment from the calculationService" should {
        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          mockPropertyIncomeSource()
          mockFinancialDataCrystalisationSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the crystalisation page" in {
          document.title() shouldBe messages.Crystallised.tabTitle
        }

      }

      "receives Business Income source from the Income Sources predicate and an error from the FinancialData Service" should {

        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018IncomeSourceSuccess)
          mockFinancialDataNoBreakdown()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives Business Income from the Income Sources predicate and an error from the FinancialData Service" should {

        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          mockFinancialDataError()
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiabilityError page" in {
          document.title() shouldBe EstimatedTaxLiabilityError.title
        }

        s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p1}" in {
          document.getElementById("p1").text() shouldBe EstimatedTaxLiabilityError.p1
        }

        s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p2}" in {
          document.getElementById("p2").text() shouldBe EstimatedTaxLiabilityError.p2
        }

      }

      "receives Business Income from the Income Sources predicate and No Data Found from the FinancialData Service" should {

        lazy val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return a 404" in {
          mockServiceInfoPartialSuccess()
          mockFinancialDataNotFound()
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.NOT_FOUND
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiabilityNoData page" in {
          document.title() shouldBe messages.title
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        mockServiceInfoPartialSuccess()
        setupMockAuthorisationException()
        mockPropertyIncomeSource()
        val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }


  "The CalculationController.redirectToEarliestEstimatedTaxLiability() action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      "successfully retrieves Business only income from the Income Sources predicate" should {

        lazy val result = TestCalculationController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status SEE_OTHER (303) (redirect)" in {
          mockServiceInfoPartialSuccess()
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.SEE_OTHER
        }

        s"redirect to ${
          controllers.routes.CalculationController
            .getFinancialData(businessIncomeModel.accountingPeriod.determineTaxYear)
        }" in {
          redirectLocation(result) shouldBe Some(controllers.routes.CalculationController
            .getFinancialData(businessIncomeModel.accountingPeriod.determineTaxYear).url)
        }
      }

      "successfully retrieves Property only income from the Income Sources predicate" should {

        lazy val result = TestCalculationController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status SEE_OTHER (303) (redirect)" in {
          mockServiceInfoPartialSuccess()
          mockPropertyIncomeSource()
          status(result) shouldBe Status.SEE_OTHER
        }

        s"redirect to ${
          controllers.routes.CalculationController
            .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
        }" in {
          redirectLocation(result) shouldBe Some(controllers.routes.CalculationController
            .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
        }
      }

      "successfully retrieves both Property and Business income from the Income Sources predicate" when {

        "the Business Accounting Period is aligned to the Tax Year and the Property Income" should {

          lazy val result = TestCalculationController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

          "return Status SEE_OTHER (303) (redirect)" in {
            mockServiceInfoPartialSuccess()
            mockBothIncomeSourcesBusinessAligned()
            status(result) shouldBe Status.SEE_OTHER
          }

          s"redirect to ${
            controllers.routes.CalculationController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
          }" in {
            redirectLocation(result) shouldBe Some(controllers.routes.CalculationController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
          }

        }

        "the Property Income Accounting Period is prior to the Business Income Accounting Period" should {

          lazy val result = TestCalculationController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

          "return Status SEE_OTHER (303) (redirect)" in {
            mockServiceInfoPartialSuccess()
            mockBothIncomeSources()
            status(result) shouldBe Status.SEE_OTHER
          }

          s"redirect to ${
            controllers.routes.CalculationController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
          }" in {
            redirectLocation(result) shouldBe Some(controllers.routes.CalculationController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
          }

        }
      }

      "retrieves no Income Sources via the Income Sources predicate" should {

        lazy val result = TestCalculationController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status ISE (500)" in {
          mockServiceInfoPartialSuccess()
          mockNoIncomeSources()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "the CalculationController.viewCrystallisedCalculations action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      "successfully receives income sources from the Income Sources predicate" should {

        lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
          mockGetAllLatestCrystallisedCalcSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Bills page" in {
          document.title() shouldBe messages.Bills.billsTitle
        }
      }

      "successfully receives income sources, but an empty list from the CalculationService" should {

        lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
          mockGetAllLatestCalcSuccessEmpty()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Bills page" in {
          document.title() shouldBe messages.Bills.billsTitle
        }
      }

      "successfully retrieves income sources, but the list returned from the service has an error model" should {

          lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)

          "return an ISE (500)" in {
            mockServiceInfoPartialSuccess()
            setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
            mockGetAllLatestCrystallisedCalcWithError()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

      }

      "successfully retrieves income sources, but the list returned from the service has a calcNotFound" should {

        lazy val result = TestCalculationController.viewCrystallisedCalculations(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockServiceInfoPartialSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018And19IncomeSourceSuccess)
          mockGetAllLatestCalcSuccessEmpty()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Bills page" in {
          document.title() shouldBe messages.Bills.billsTitle
        }

        }

      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          setupMockAuthorisationException()
          val result = TestCalculationController.getFinancialData(testYear)(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
      }

    }

}
