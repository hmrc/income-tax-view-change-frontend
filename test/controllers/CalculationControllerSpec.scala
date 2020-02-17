/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import assets.Messages
import assets.Messages.EstimatedTaxLiabilityError
import audit.AuditingService
import config.featureswitch.{CalcDataApi, FeatureSwitching}
import config.{ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialTransactionsService}
import models.calculation.CalcDisplayError
import play.api.http.Status
import play.api.test.Helpers._
import testUtils.TestSupport

class CalculationControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockFinancialTransactionsService with FeatureSwitching {

  object TestCalculationController extends CalculationController()(
    appConfig,
    messagesApi,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[AuditingService],
    mockFinancialTransactionsService,
    app.injector.instanceOf[ItvcErrorHandler]
  )

  private trait CalculationDataApiEnabled {
    enable(CalcDataApi)
  }

  private trait CalculationDataApiDisabled {
    disable(CalcDataApi)
  }

  lazy val messages = new Messages.Calculation(testYear)

  "The CalculationController.renderCalculationPage(year) action" when {

    "Called with an Authenticated HMRC-MTD-IT User" when {

      "the Calculation Data API feature is enabled" when {

        "it receives Business only income from the Income Sources predicate " +
          "and successful calculation from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiEnabled {
            mockFinancialTransactionFailed()
            mockCalculationSuccess()
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiability page" in new CalculationDataApiEnabled {
            document.title() shouldBe messages.title
          }
        }

        "it receives Property only income from the Income Sources predicate " +
          "and successful calculation from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiEnabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            mockCalculationSuccess()
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiability page" in new CalculationDataApiEnabled {
            document.title() shouldBe messages.title
          }
        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and a transaction from the Financial Transactions Service" should {
          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiEnabled {
            mockFinancialTransactionSuccess()
            mockPropertyIncomeSource()
            mockCalculationCrystalisationSuccess()
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the crystalisation page" in new CalculationDataApiEnabled {
            document.title() shouldBe messages.Crystallised.tabTitle
          }

        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and an error model from the Financial Transactions Service" should {
          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          "return Status Internal Server Error (500)" in new CalculationDataApiEnabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            mockCalculationCrystalisationSuccess()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and no transaction with the correct date" should {
          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          "return Status Internal Server Error (500)" in new CalculationDataApiEnabled {
            mockFinancialTransactionSuccess("2020-04-05")
            mockPropertyIncomeSource()
            mockCalculationCrystalisationSuccess()
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

        }

        "it receives Business Income source from the Income Sources predicate " +
          "and an error from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiEnabled {
            mockFinancialTransactionSuccess()
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            setupMockGetCalculation(testMtdUserNino.nino, testYear)(CalcDisplayError)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiability page" in new CalculationDataApiEnabled {
            document.title() shouldBe messages.title
          }
        }

        "it receives Business Income from the Income Sources predicate " +
          "and an error from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiEnabled {
            mockFinancialTransactionSuccess()
            mockCalculationError()
            mockSingleBusinessIncomeSource()
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiabilityError page" in new CalculationDataApiEnabled {
            document.title() shouldBe EstimatedTaxLiabilityError.title
          }

          s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p1}" in new CalculationDataApiEnabled {
            document.getElementById("p1").text() shouldBe EstimatedTaxLiabilityError.p1
          }

          s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p2}" in new CalculationDataApiEnabled {
            document.getElementById("p2").text() shouldBe EstimatedTaxLiabilityError.p2
          }

        }

        "it receives Business Income from the Income Sources predicate " +
          "and No Data Found from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return a 404" in new CalculationDataApiEnabled {
            mockFinancialTransactionSuccess()
            mockCalculationNotFound()
            mockSingleBusinessIncomeSource()
            status(result) shouldBe Status.NOT_FOUND
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiabilityNoData page" in new CalculationDataApiEnabled {
            document.title() shouldBe messages.title
          }
        }

        "provided with a negative tax year " should {
          lazy val result = TestCalculationController.renderCalculationPage(-testYear)(fakeRequestWithActiveSession)

          "return Status Bad Request Error (400)" in new CalculationDataApiEnabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            mockCalculationCrystalisationSuccess()
            status(result) shouldBe Status.BAD_REQUEST
          }
        }
      }

      "the Calculation Data API feature is disabled" when {

        "it receives Business only income from the Income Sources predicate " +
          "and retrieves an estimated calculation from the Calculation service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiDisabled {
            mockFinancialTransactionFailed()
            mockSingleBusinessIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcSuccess)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiDisabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the Estimate page" in new CalculationDataApiDisabled {
            document.title() shouldBe messages.title
          }
        }

        "it receives Property only income from the Income Sources predicate " +
          "and retrieves an estimated calculation from the Calculation service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiDisabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcSuccess)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiDisabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the Estimate page" in new CalculationDataApiDisabled {
            document.title() shouldBe messages.title
          }
        }

        "it receives Property only income from the Income Sources predicate " +
          "and retrieves an estimated calculation from the Calculation service with invalid calculation data" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status Internal Server Error (500)" in new CalculationDataApiDisabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcSuccess.copy(timestamp = None))
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and a transaction from the Financial Transactions Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiDisabled {
            mockFinancialTransactionSuccess()
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcCrystallisedSuccess)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiDisabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the Bill page" in new CalculationDataApiDisabled {
            document.title() shouldBe messages.Crystallised.tabTitle
          }
        }

        "it receives a crystallised calculation from the Calculation Service with invalid calculation data " +
          "and a transaction from the Financial Transactions Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status Internal Server Error (500)" in new CalculationDataApiDisabled {
            mockFinancialTransactionSuccess()
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcCrystallisedSuccess.copy(totalIncomeTaxAndNicsDue = None))
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and an error model from the Financial Transactions Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          "return Status Internal Server Error (500)" in new CalculationDataApiDisabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcCrystallisedSuccess)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "it receives a crystallised calculation from the Calculation Service " +
          "and no transaction with the correct date" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)

          "return Status Internal Server Error (500)" in new CalculationDataApiDisabled {
            mockFinancialTransactionSuccess("2020-04-05")
            mockPropertyIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcCrystallisedSuccess)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "it receives Business Income from the Income Sources predicate " +
          "and an error from the Calculation Service" should {

          lazy val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument

          "return Status OK (200)" in new CalculationDataApiDisabled {
            mockFinancialTransactionSuccess()
            mockSingleBusinessIncomeSource()
            setupMockGetCalculationId(testMtdUserNino.nino, testYear)(Right("testId"))
            setupMockGetLatestCalculation(testMtdUserNino.nino, Right("testId"))(lastTaxCalcError)
            status(result) shouldBe Status.OK
          }

          "return HTML" in new CalculationDataApiEnabled {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "render the EstimatedTaxLiabilityError page" in new CalculationDataApiDisabled {
            document.title() shouldBe EstimatedTaxLiabilityError.title
          }

          s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p1}" in new CalculationDataApiDisabled {
            document.getElementById("p1").text() shouldBe EstimatedTaxLiabilityError.p1
          }

          s"Have a paragraph with the messages ${EstimatedTaxLiabilityError.p2}" in new CalculationDataApiDisabled {
            document.getElementById("p2").text() shouldBe EstimatedTaxLiabilityError.p2
          }
        }

        "provided with a negative tax year " should {
          lazy val result = TestCalculationController.renderCalculationPage(-testYear)(fakeRequestWithActiveSession)

          "return Status Bad Request Error (400)" in new CalculationDataApiDisabled {
            mockFinancialTransactionFailed()
            mockPropertyIncomeSource()
            mockCalculationCrystalisationSuccess()
            status(result) shouldBe Status.BAD_REQUEST
          }
        }
      }
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestCalculationController.renderCalculationPage(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
