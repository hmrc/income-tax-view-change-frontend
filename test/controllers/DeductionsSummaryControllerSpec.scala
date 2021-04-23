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

import assets.BaseTestConstants.{testCredId, testMtditid, testNino}
import assets.EstimatesTestConstants.testYear
import assets.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import audit.mocks.MockAuditingService
import audit.models.{AllowanceAndDeductionsRequestAuditModel, AllowanceAndDeductionsResponseAuditModel}
import config.featureswitch.{DeductionBreakdown, FeatureSwitching}
import config.{ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialTransactionsService}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{charset, contentType, _}
import testUtils.TestSupport

class DeductionsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with MockFinancialTransactionsService with FeatureSwitching with MockAuditingService {

  object TestDeductionsSummaryController extends DeductionsSummaryController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    mockAuditingService,
    mockFinancialTransactionsService,
    app.injector.instanceOf[ItvcErrorHandler]
  )(
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    languageUtils
  )

  "showDeductionsSummary" when {
    "feature switch DeductionsBreakdown is enabled" when {
      enable(DeductionBreakdown)

      "given a tax year which can be found in ETMP" should {

        lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200) with audit events" in {
          mockCalculationSuccess()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.OK

          verifyExtendedAudit(AllowanceAndDeductionsRequestAuditModel(testMtditid, testNino, None, Some(testCredId), Some("Individual")))
          verifyExtendedAudit(AllowanceAndDeductionsResponseAuditModel(testMtditid, testNino, None, Some(testCredId),
            Some("Individual"), Some(BigDecimal("11500")), Some(BigDecimal("11501"))))
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the IncomeBreakdown page" in {
          document.title() shouldBe "Allowances and deductions - Business Tax account - GOV.UK"
        }
      }
      "given a tax year which can not be found in ETMP" should {

        lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status Internal Server Error (500)" in {
          mockCalculationNotFound()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "there is a downstream error" should {

        lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status Internal Server Error (500)" in {
          mockCalculationError()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "feature switch DeductionsBreakdown is disabled" when {


      "given a tax year which can be found in ETMP" should {

        lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status NotFound (404)" in {
          disable(DeductionBreakdown)
          mockCalculationNotFound()
          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
          status(result) shouldBe Status.NOT_FOUND
        }
      }
    }
  }
}

