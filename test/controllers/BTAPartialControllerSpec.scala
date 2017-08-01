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
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants.Obligations._
import assets.TestConstants._
import config.FrontendAppConfig
import mocks.controllers.predicates.MockAsyncActionPredicate
import mocks.services.MockBTAPartialService
import models.LastTaxCalculation
import play.api.i18n.MessagesApi
import utils.TestSupport
import play.api.http.Status
import play.api.test.Helpers._
import utils.ImplicitCurrencyFormatter._

class BTAPartialControllerSpec extends TestSupport with MockBTAPartialService with MockAsyncActionPredicate {

  object TestBTAPartialController extends BTAPartialController()(
    fakeApplication.injector.instanceOf[FrontendAppConfig],
    fakeApplication.injector.instanceOf[MessagesApi],
    MockAsyncActionPredicate,
    mockBTAPartialService
  )

  "The BTAPartialController.setupPartial action" when {
    "having successfully retrieved an obligation and estimates from misaligned tax years from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcSuccess)
        setupMockGetEstimate(testNino, 2019)(LastTaxCalculation(
          calcID = testTaxCalculationId,
          calcTimestamp = "2018-07-06T12:34:56.789Z",
          calcAmount = 6543.21
        ))
        mockBothIncomeSources()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
        document.getElementById("current-estimate-2019").text() shouldBe messages.currentEstimate(BigDecimal(6543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation and estimates from the same tax year from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModelAlignedTaxYear))(openObligation)
        setupMockGetEstimate(testNino, 2018)(lastTaxCalcSuccess)
        mockBothIncomeSourcesBusinessAligned()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation and a first estimate, but NoLastTaxCalculation from the last estimate" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(openObligation)
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
        document.getElementById("quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation and a last estimate, but NoLastTaxCalculation from the first estimate" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(openObligation)
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
        document.getElementById("quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.getElementById("current-estimate-2019").text() shouldBe messages.currentEstimate(BigDecimal(543.21).toCurrencyString)
      }
    }

    "having successfully retrieved an obligation, but no estimate from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(openObligation)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcNotFound)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the BTA partial" in {
        document.getElementById("quarterly-reporting-heading").text() shouldBe messages.heading
        document.getElementById("report-due").text() shouldBe messages.reportDue(longDate("2017-10-31").toLongDate)
        document.body().toString.contains("id=\"current-estimate\"") shouldBe false
      }
    }

    "having successfully retrieved an obligation, but a LastTaxCalculationError from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)

      "return Status INTERNAL_SERVER_ERROR (500)" ignore {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(openObligation)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcError)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }

    "having successfully retrieved an estimate, but no obligations from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)

      "return Status INTERNAL_SERVER_ERROR (500)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(obligationsDataErrorModel)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcSuccess)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }

    "receiving nothing from BTAPartialService" should {
      lazy val result = TestBTAPartialController.setupPartial(fakeRequestWithActiveSession)

      "return Status INTERNAL_SERVER_ERROR (500)" in {
        setupMockGetObligations(testNino, Some(businessIncomeModel))(obligationsDataErrorModel)
        setupMockGetEstimate(testNino, 2019)(lastTaxCalcError)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }
  }

}
