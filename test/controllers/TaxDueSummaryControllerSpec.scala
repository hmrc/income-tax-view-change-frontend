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

import testConstants.EstimatesTestConstants.testYear
import testConstants.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import config.ItvcErrorHandler
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy}
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.TaxCalcBreakdown
import views.html.TaxCalcBreakdownNew

class TaxDueSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  object TestTaxDueSummaryController extends TaxDueSummaryController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[TaxCalcBreakdown],
    app.injector.instanceOf[BtaNavBarPredicate],
    app.injector.instanceOf[TaxCalcBreakdownNew],
    mockAuditingService
  )(appConfig, languageUtils, app.injector.instanceOf[MessagesControllerComponents], ec)

  "showTaxDueSummary" when {

    "given a tax year which can be found in ETMP and NewTaxCalcProxy is enabled" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        enable(NewTaxCalcProxy)
        mockCalculationSuccessFullNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the Tax Due page" in {
        document.title() shouldBe "Tax calculation - Business Tax account - GOV.UK"
      }
    }

    "given a tax year which can be found in ETMP" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        disable(NewTaxCalcProxy)
        mockCalculationSuccess()
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the Tax Due page" in {
        document.title() shouldBe "Tax calculation - Business Tax account - GOV.UK"
      }
    }

    "given a tax year which can not be found in ETMP" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        disable(NewTaxCalcProxy)
        mockCalculationNotFound()
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "given a tax year which can not be found in ETMP and NewTaxCalcProxy is enabled" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)

      "return Status ISE (500)" in {
        enable(NewTaxCalcProxy)
        mockCalculationNotFoundNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "there is a downstream error" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        disable(NewTaxCalcProxy)
        mockCalculationError()
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error and NewTaxCalcProxy is enabled" should {
      lazy val result = TestTaxDueSummaryController.showTaxDueSummary(testYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        enable(NewTaxCalcProxy)
        mockCalculationErrorNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

