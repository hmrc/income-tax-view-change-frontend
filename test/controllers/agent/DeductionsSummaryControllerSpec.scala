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

package controllers.agent

import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import audit.mocks.MockAuditingService
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import models.calculation.{CalcDisplayError, CalcDisplayNoDataFound}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.EstimatesTestConstants.testYear
import testUtils.TestSupport

import scala.concurrent.{ExecutionContext, Future}

class DeductionsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockFrontendAuthorisedFunctions with MockIncomeSourceDetailsService
  with FeatureSwitching with MockItvcErrorHandler with MockAuditingService{

  class Setup {

    val testYear: Int = 2020
    val isAgent: Boolean = true // currently unused - might be required if differentiating between indiv. & agent

    val controller: DeductionsSummaryController = new DeductionsSummaryController(
      app.injector.instanceOf[views.html.DeductionBreakdown],
      app.injector.instanceOf[views.html.DeductionBreakdownNew],
      mockAuthService,
      mockAuditingService,
      mockCalculationService
    )(
      appConfig,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      itvcErrorHandler = mockItvcErrorHandler
    )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(NewTaxCalcProxy)
  }

  "showDeductionsSummary" when {
    "NewTaxCalcProxy FS is enabled" should {

      "render the Allowances and Deductions page with full calc data" in new Setup {
        enable(NewTaxCalcProxy)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockCalculationSuccessFullNew(taxYear = testYear)

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
        val document = result.toHtmlDocument

        status(result) shouldBe Status.OK
        document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
        document.getElementById("total-value").text() shouldBe "£17,500.99"
      }

      "render the Allowances and Deductions page with no calc data" in new Setup {
        enable(NewTaxCalcProxy)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockCalculationSuccessMinimalNew(taxYear = testYear)

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
        val document = result.toHtmlDocument

        status(result) shouldBe Status.OK
        document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
        document.getElementById("total-value").text() shouldBe "£0.00"
      }

    }

    "feature switch AgentViewer is enabled" should {
      "return Status OK when income sources and calculations come back with success" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
      }

      "return calcDisplay error case scenario" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
        mockShowInternalServerError()

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return internal server error when Error from Calc" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetCalculation("AA111111A", testYear)(CalcDisplayNoDataFound)
        mockShowInternalServerError()

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "backUrl" should {
        "return to the home page" in new Setup {
          controller.backUrl(testYear) shouldBe controllers.agent.routes.TaxYearOverviewController.show(testYear).url
        }
      }
    }
  }
}
