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

package controllers.agent

import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import models.calculation.CalcDisplayError
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

class DeductionsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockFrontendAuthorisedFunctions with MockIncomeSourceDetailsService
  with FeatureSwitching with MockItvcErrorHandler with MockAuditingService{

  class Setup {

    val testYear: Int = 2020

    val controller: DeductionsSummaryController = new DeductionsSummaryController(
      app.injector.instanceOf[views.html.agent.DeductionBreakdown],
      mockAuthService,
      mockIncomeSourceDetailsService,
      mockAuditingService,
      mockCalculationService
    )(
      appConfig,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      itvcErrorHandler = mockItvcErrorHandler
    )
  }

  "showDeductionsSummary" when {
    "feature switch AgentViewer is enabled" should {
      "return Status OK when income sources and calculations come back with success" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
      }

      "return calcDisplay error case scenario" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
        mockShowInternalServerError()

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return internal server error when Error from both Calc and Income sources" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        mockCalculationNotFound()

        val result: Future[Result] = controller.showDeductionsSummary(taxYear = testYear)(fakeRequestConfirmedClient())

        result.failed.futureValue shouldBe an[InternalServerException]

      }

      "backUrl" should {
        "return to the home page" in new Setup {
          controller.backUrl(testYear) shouldBe controllers.agent.routes.TaxYearOverviewController.show(testYear).url
        }
      }
    }
  }
}
