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

import audit.AuditingService
import config.featureswitch.FeatureSwitching
import config.{ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{BtaNavFromNinoPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{charset, contentType, _}
import testConstants.BaseTestConstants.testMtditid
import testConstants.EstimatesTestConstants.testYear
import testConstants.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import testUtils.TestSupport
import views.html.IncomeBreakdown

class IncomeSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  object TestIncomeSummaryController extends IncomeSummaryController(
    app.injector.instanceOf[IncomeBreakdown],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[AuditingService],
    app.injector.instanceOf[BtaNavFromNinoPredicate],
    app.injector.instanceOf[ItvcErrorHandler])(
    ec,
    languageUtils,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents])

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "showIncomeSummary" when {

    lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)
    lazy val document = result.toHtmlDocument

    "given a tax year which can be found in ETMP" should {

      "return Status OK (200)" in {
        mockCalculationSuccessFullNew(testMtditid)
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

      "return Status Internal Server Error (500)" in {
        mockCalculationNotFoundNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "there is a downstream error which return NOT_FOUND" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationNotFoundNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error which return INTERNAL_SERVER_ERROR" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationErrorNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

