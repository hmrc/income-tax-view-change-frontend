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

import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
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
  }

  "showDeductionsSummary" when {
    "render the Allowances and Deductions page with full calc data" in new Setup {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationSuccessFullNew(taxYear = testYear)

      val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
      val document = result.toHtmlDocument

      status(result) shouldBe Status.OK
      document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
      document.getElementById("total-value").text() shouldBe "£17,500.99"
    }

    "render the Allowances and Deductions page with no calc data" in new Setup {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationSuccessMinimalNew(taxYear = testYear)

      val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
      val document = result.toHtmlDocument

      status(result) shouldBe Status.OK
      document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
      document.getElementById("total-value").text() shouldBe "£0.00"
    }

    "render error page when NOT_FOUND is returned from calc" in new Setup {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationNotFoundNew(year = testYear)
      mockShowInternalServerError()

      val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}
