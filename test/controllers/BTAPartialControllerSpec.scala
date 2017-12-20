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

import assets.Messages.{BtaPartial => messages}
import assets.TestConstants.Estimates._
import assets.TestConstants.IncomeSourceDetails._
import assets.TestConstants.ReportDeadlines._
import assets.TestConstants._
import config.FrontendAppConfig
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.Estimate
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockBTAPartialService
import models.LastTaxCalculation
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.ImplicitCurrencyFormatter._
import utils.TestSupport

class BTAPartialControllerSpec extends TestSupport with MockBTAPartialService with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestBTAPartialController extends BTAPartialController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockBTAPartialService
  )

  "The BTAPartialController.setupPartial action" when {
    "having successfully retrieved an obligation and estimates from misaligned tax years from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, bothIncomeSourceSuccessMisalignedTaxYear)(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcSuccess)
        setupMockGetEstimate(testNino, 2019)(LastTaxCalculation(
          calcID = testTaxCalculationId,
          calcTimestamp = "2018-07-06T12:34:56.789Z",
          calcAmount = 6543.21,
          calcStatus = Estimate
        ))
        mockBothIncomeSources()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimateYear(2018, BigDecimal(543.21).toCurrencyString)
        document.getElementById("current-estimate-2019").text() shouldBe messages.currentEstimateYear(2019, BigDecimal(6543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation and estimates from the same tax year from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, bothIncomeSourcesSuccessBusinessAligned)(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcSuccess)
        mockBothIncomeSourcesBusinessAligned()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation and a first estimate, but NoLastTaxCalculation from the last estimate" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, bothIncomeSourceSuccessMisalignedTaxYear)(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcSuccess)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcNotFound)
        mockBothIncomeSources()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimateYear(2018, BigDecimal(543.21).toCurrencyString)
        document.getElementById("current-estimate-2019").text() shouldBe messages.noEstimate(2019)
      }
    }

    "having successfully retrieved an obligation and a last estimate, but NoLastTaxCalculation from the first estimate" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, bothIncomeSourceSuccessMisalignedTaxYear)(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcNotFound)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcSuccess)
        mockBothIncomeSources()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.noEstimate(2018)
        document.getElementById("current-estimate-2019").text() shouldBe messages.currentEstimateYear(2019, BigDecimal(543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation, but no estimate from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, businessIncomeSourceSuccess)(openObligation)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcNotFound)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.body().toString.contains("id=\"current-estimate\"") shouldBe false
      }
    }

    "having successfully retrieved an obligation, but a LastTaxCalculationError from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, businessIncomeSourceSuccess)(openObligation)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcError)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the quarterly reporting heading" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
      }

      "render the next obligation date" in {
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
      }

      "render the estimate section as error" in {
        document.getElementById("estimate-error-p1").text() shouldBe messages.Error.estimateErrorP1
        document.getElementById("estimate-error-p2").text() shouldBe messages.Error.estimateErrorP2
      }
    }

    "having successfully retrieved an estimate, but no obligations from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetReportDeadlines(testNino, businessIncomeSourceSuccess)(obligationsDataErrorModel)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcSuccess)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }


      "render the quarterly reporting heading" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
      }

      "render the next estimated tax amount" in {
        document.getElementById("current-estimate-2019").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
      }

      "render the obligations section as error" in {
        document.getElementById("obligation-error-p1").text() shouldBe messages.Error.obligationErrorP1
        document.getElementById("obligation-error-p2").text() shouldBe messages.Error.obligationErrorP2
      }
    }

    "receiving nothing from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status INTERNAL_SERVER_ERROR (500)" in {
        setupMockGetReportDeadlines(testNino, businessIncomeSourceSuccess)(obligationsDataErrorModel)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcError)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the obligations section as error" in {
        document.getElementById("obligation-error-p1").text() shouldBe messages.Error.obligationErrorP1
        document.getElementById("obligation-error-p2").text() shouldBe messages.Error.obligationErrorP2
      }

      "render the estimate section as error" in {
        document.getElementById("estimate-error-p1").text() shouldBe messages.Error.estimateErrorP1
        document.getElementById("estimate-error-p2").text() shouldBe messages.Error.estimateErrorP2
      }
    }
  }

}
