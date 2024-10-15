/*
 * Copyright 2023 HM Revenue & Customs
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
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import models.admin.NavBarFs
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{charset, contentType, _}
import services.IncomeSourceDetailsService
import services.admin.FeatureSwitchService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testMtditid, testMtditidAgent, testTaxYear}
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessful
import testUtils.TestSupport
import views.html.ForecastIncomeSummary

class ForecastIncomeSummaryControllerSpec extends TestSupport with MockCalculationService with MockFrontendAuthorisedFunctions
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val forecastIncomeView: ForecastIncomeSummary = app.injector.instanceOf[ForecastIncomeSummary]

  object TestIncomeSummaryController extends ForecastIncomeSummaryController(
    app.injector.instanceOf[ForecastIncomeSummary],
    mockCalculationService,
    app.injector.instanceOf[AuditingService],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[IncomeSourceDetailsService],
    mockAuthService,
    app.injector.instanceOf[FeatureSwitchService],
    testAuthenticator
  )(
    ec,
    languageUtils,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[AgentItvcErrorHandler])

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(NavBarFs)
  }

  "individual user" when {
    "show method" when {

      lazy val result = TestIncomeSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "given a tax year which can be found in ETMP" should {
        val backlink = "/report-quarterly/income-and-expenses/view/tax-year-summary/2018"
        val endOfYearEstimateModel = liabilityCalculationModelSuccessful.calculation.get.endOfYearEstimate.get
        val expectedContent: String = forecastIncomeView(
          endOfYearEstimateModel = endOfYearEstimateModel,
          taxYear = testTaxYear,
          backUrl = backlink,
          isAgent = false,
          btaNavPartial = None
        ).toString

        "return Status OK (200)" in {
          mockCalculationSuccessfulNew(testMtditid)
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the IncomeBreakdown page" in {
          document.title() shouldBe messages("htmlTitle", messages("forecast_income.heading"))
          contentAsString(result) shouldBe expectedContent
        }
      }
      "given a tax year which can not be found in ETMP" should {

        lazy val result = TestIncomeSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        "return Status Internal Server Error (500)" in {
          mockCalculationNotFoundNew(testMtditid)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "there is a downstream error which return NOT_FOUND" should {

        lazy val result = TestIncomeSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        "return Status Internal Server Error (500)" in {
          mockCalculationNotFoundNew(testMtditid)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "there is a downstream error which return INTERNAL_SERVER_ERROR" should {

        lazy val result = TestIncomeSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        "return Status Internal Server Error (500)" in {
          mockCalculationErrorNew(testMtditid)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "agent user" when {
    "show method" when {

      lazy val result = TestIncomeSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))
      lazy val document = result.toHtmlDocument

      "given a tax year which can be found in ETMP" should {
        val backlink = "/report-quarterly/income-and-expenses/view/agents/tax-year-summary/2018"
        val endOfYearEstimateModel = liabilityCalculationModelSuccessful.calculation.get.endOfYearEstimate.get
        val expectedContent: String = forecastIncomeView(
          endOfYearEstimateModel = endOfYearEstimateModel,
          taxYear = testTaxYear,
          backUrl = backlink,
          isAgent = true,
          btaNavPartial = None
        ).toString

        "return Status OK (200)" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationSuccessfulNew(testMtditidAgent)
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the IncomeBreakdown page" in {
          document.title() shouldBe messages("htmlTitle.agent", messages("forecast_income.heading"))
          contentAsString(result) shouldBe expectedContent
        }
      }

      "given a tax year which can not be found in ETMP" should {

        lazy val result = TestIncomeSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        "return Status Internal Server Error (500)" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationNotFoundNew(testMtditidAgent)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "there is a downstream error which return NOT_FOUND" should {

        lazy val result = TestIncomeSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        "return Status Internal Server Error (500)" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationNotFoundNew(testMtditidAgent)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "there is a downstream error which return INTERNAL_SERVER_ERROR" should {

        lazy val result = TestIncomeSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        "return Status Internal Server Error (500)" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationErrorNew(testMtditidAgent)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}

