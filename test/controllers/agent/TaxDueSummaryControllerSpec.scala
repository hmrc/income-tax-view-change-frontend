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
import testConstants.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import mocks.views.agent.MockTaxCalcBreakdown
import models.calculation.CalcDisplayError
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, _}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext

class TaxDueSummaryControllerSpec extends TestSupport with MockCalculationService with MockTaxCalcBreakdown
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockItvcErrorHandler {

  class Setup {
    val testYear: Int = 2020
		val isAgent: Boolean = true

    val controller: TaxDueSummaryController = new TaxDueSummaryController(
      taxCalcBreakdown = taxCalcBreakdown,
      appConfig = appConfig,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      auditingService = mockAuditingService
    )(
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      itvcErrorHandler = mockItvcErrorHandler
    )
  }

  "backUrl" should {
    "return to the taxyear overview" in new Setup {
      controller.backUrl(testYear) shouldBe controllers.agent.routes.TaxYearOverviewController.show(testYear).url
    }
  }

  "showTaxDueSummary" when {
		"given a tax year which can be found in ETMP" should {
			"return Status OK (200) with HTML" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
				mockTaxCalcBreakdown(testYear, calculationDisplaySuccessModel(calculationDataSuccessModel),
					controllers.agent.routes.TaxYearOverviewController.show(testYear).url, isAgent)(HtmlFormat.empty)

				lazy val result = controller.showTaxDueSummary(testYear)(fakeRequestConfirmedClient())

				status(result) shouldBe OK
				contentType(result) shouldBe Some(HTML)
			}
		}
		"there was a problem retrieving income source details for the user" should {
			"throw an internal server exception" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockErrorIncomeSource()
				setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
				mockShowInternalServerError()

				val result = controller.showTaxDueSummary(testYear)(fakeRequestConfirmedClient()).failed.futureValue
				result shouldBe an[InternalServerException]
				result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
			}
		}

		"there is a downstream error" should {
			"return Status Internal Server Error (500)" in new Setup {
				lazy val result = controller.showTaxDueSummary(testYear)(fakeRequestConfirmedClient())
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
				mockShowInternalServerError()
				setupMockGetIncomeSourceDetails()(businessIncome2018and2019)

				status(result) shouldBe Status.INTERNAL_SERVER_ERROR
			}
		}
  }
}
