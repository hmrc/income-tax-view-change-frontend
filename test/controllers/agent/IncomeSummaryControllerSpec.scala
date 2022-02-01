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

import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import mocks.views.agent.MockIncomeSummary
import models.calculation.CalcDisplayError
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, _}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testMtditid}
import testConstants.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessFull
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.ExecutionContext

class IncomeSummaryControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with FeatureSwitching
  with MockIncomeSummary with MockCalculationService with MockIncomeSourceDetailsService with MockItvcErrorHandler {

  class Setup {
    val testYear: Int = 2020
    val isAgent: Boolean = true

    val controller: IncomeSummaryController = new IncomeSummaryController(
      incomeBreakdown = incomeBreakdown,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      incomeBreakdownOld = incomeBreakdownOld,
      incomeSourceDetailsService = mockIncomeSourceDetailsService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      mockItvcErrorHandler
    )
  }
  "backUrl" should {
    "return to the taxyear overview" in new Setup {
      controller.backUrl(testYear) shouldBe controllers.agent.routes.TaxYearOverviewController.show(testYear).url
    }
  }
  "showIncomeSummary" when {
    "NewTaxCalcProxy feature switch is disabled" when {
      "given a tax year which can be found in ETMP" should {
        "return Status OK (200) with html content and right title" in new Setup {
          disable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
          mockIncomeBreakdownOld(testYear, calculationDisplaySuccessModel(calculationDataSuccessModel),
            controllers.agent.routes.TaxYearOverviewController.show(testYear).url, isAgent)(HtmlFormat.empty)

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
      "there was a problem retrieving income source details for the user" should {
        "throw an internal server exception" in new Setup {
          disable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockErrorIncomeSource()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
          mockShowInternalServerError()
          val exception = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient()).failed.futureValue
          exception shouldBe an[InternalServerException]
          exception.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
        }

      }

      "there is a downstream error" should {
        "return Status Internal Server Error (500)" in new Setup {
          disable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
          mockShowInternalServerError()

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
    "NewTaxCalcProxy feature switch is enabled" when {
      "given a tax year which can be found in ETMP" should {
        "return Status OK (200) with html content and right title" in new Setup {
          enable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculationNew("XAIT00000000015","AA111111A", testYear)(liabilityCalculationModelSuccessFull)
          mockIncomeBreakdown(testYear, IncomeBreakdownViewModel(liabilityCalculationModelSuccessFull.calculation).get,
            controllers.agent.routes.TaxYearOverviewController.show(testYear).url, isAgent)(HtmlFormat.empty)

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
      "there was a problem retrieving income source details for the user" should {
        "throw an internal server exception" in new Setup {
          enable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockErrorIncomeSource()
          setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(liabilityCalculationModelSuccessFull)
          mockShowInternalServerError()
          val exception = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient()).failed.futureValue
          exception shouldBe an[InternalServerException]
          exception.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
        }

      }

      "there is a downstream error" should {
        "return Status Internal Server Error (500)" in new Setup {
          enable(NewTaxCalcProxy)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(LiabilityCalculationError(500, "error"))
          mockShowInternalServerError()

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          disable(NewTaxCalcProxy)
        }
      }
    }
  }
}

