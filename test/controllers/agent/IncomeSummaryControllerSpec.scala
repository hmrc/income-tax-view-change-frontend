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

import assets.BaseTestConstants.testAgentAuthRetrievalSuccess
import assets.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import config.featureswitch.{AgentViewer, FeatureSwitching, IncomeBreakdown}
import implicits.ImplicitDateFormatterImpl
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockIncomeSourceDetailsService}
import mocks.views.MockIncomeBreakdown
import models.calculation.CalcDisplayError
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, _}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.ExecutionContext

class IncomeSummaryControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with FeatureSwitching
  with MockIncomeBreakdown with MockCalculationService with MockIncomeSourceDetailsService with MockItvcErrorHandler {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
    disable(IncomeBreakdown)
  }

  class Setup {
    val testYear: Int = 2020

    val controller: IncomeSummaryController = new IncomeSummaryController(
      incomeBreakdown = incomeBreakdown,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      incomeSourceDetailsService = mockIncomeSourceDetailsService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ImplicitDateFormatterImpl],
      app.injector.instanceOf[ExecutionContext],
      itvcErrorHandler = mockItvcErrorHandler
    )
  }
  "backUrl" should {
    "return to the taxyear overview" in new Setup {
      controller.backUrl(testYear) shouldBe controllers.agent.routes.TaxYearOverviewController.show(testYear).url
    }
  }
  "showIncomeSummary" when {
    "feature switch IncomeBreakdown and AgentViewer are enabled" when {
      "given a tax year which can be found in ETMP" should {
        "return Status OK (200) with html content and right title" in new Setup {
          enable(IncomeBreakdown)
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
          mockIncomeBreakdown(testYear, calculationDisplaySuccessModel(calculationDataSuccessModel),
            controllers.agent.routes.TaxYearOverviewController.show(testYear).url)(HtmlFormat.empty)

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
      "there was a problem retrieving income source details for the user" should {
        "throw an internal server exception" in new Setup {
          enable(IncomeBreakdown)
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockErrorIncomeSource()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
          mockShowInternalServerError()

          intercept[InternalServerException](await(controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())))
            .message shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
        }

      }

      "there is a downstream error" should {
        "return Status Internal Server Error (500)" in new Setup {
          enable(IncomeBreakdown)
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
          mockShowInternalServerError()

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "feature switch AgentViewer is disabled" when {
      "given a tax year which can be found in ETMP" should {
        "return Status NotFound (404)" in new Setup {
          disable(AgentViewer)
          enable(IncomeBreakdown)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockNotFound()

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe Status.NOT_FOUND
        }
      }
    }

    "feature switch IncomeBreakdown is disabled" when {
      "given a tax year which can be found in ETMP" should {
        "return Status NotFound (404)" in new Setup {
          disable(IncomeBreakdown)
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockNotFound()

          lazy val result = controller.showIncomeSummary(testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe Status.NOT_FOUND
        }
      }
    }
  }
}

