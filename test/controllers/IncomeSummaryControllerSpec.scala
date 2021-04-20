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

import assets.BaseTestConstants.testMtdUserNino
import assets.EstimatesTestConstants.testYear
import assets.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import audit.AuditingService
import config.featureswitch.{FeatureSwitching, IncomeBreakdown}
import config.{ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialTransactionsService}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{charset, contentType, _}
import testUtils.TestSupport

class IncomeSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockFinancialTransactionsService with FeatureSwitching {

  object TestIncomeSummaryController extends IncomeSummaryController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[AuditingService],
    mockFinancialTransactionsService,
    app.injector.instanceOf[ItvcErrorHandler])(
    ec,
    languageUtils,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents])

  "showIncomeSummary" when {
    "feature switch IncomeBreakdown is enabled" when {
      enable(IncomeBreakdown)

    "given a tax year which can be found in ETMP" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "return Status OK (200)" in {
        mockCalculationSuccess()
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "render the IncomeBreakdown page" in {
        document.title() shouldBe "Income - Business Tax account - GOV.UK"
      }
    }
      "given a tax year which can not be found in ETMP" should {

        lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status Internal Server Error (500)" in {
          mockCalculationNotFound()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "there is a downstream error" should {

        lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status Internal Server Error (500)" in {
          mockCalculationError()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "feature switch IncomeBreakdown is disabled" when {


      "given a tax year which can be found in ETMP" should {

        lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status NotFound (404)" in {
          disable(IncomeBreakdown)
          mockCalculationNotFound()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.NOT_FOUND
        }
      }
    }
  }
}

