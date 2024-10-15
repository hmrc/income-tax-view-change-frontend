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

import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockCalculationService
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{charset, contentType, defaultAwaitTimeout, status}
import services.IncomeSourceDetailsService
import services.admin.FeatureSwitchService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testMtditid, testMtditidAgent, testTaxYear}
import testUtils.TestSupport
import views.html.ForecastTaxCalcSummary

class ForecastTaxCalcSummaryControllerSpec extends TestSupport with MockCalculationService with MockFrontendAuthorisedFunctions
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockAuditingService with FeatureSwitching {

  object TestForecastTaxCalcSummaryController extends ForecastTaxCalcSummaryController(
    app.injector.instanceOf[ForecastTaxCalcSummary],
    mockAuditingService,
    mockCalculationService,
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
    app.injector.instanceOf[AgentItvcErrorHandler]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "individual user" when {
    "show(taxYear)" when {
      lazy val result = {
        disableAllSwitches()
        mockCalculationSuccessfulNew(testMtditid)
        TestForecastTaxCalcSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)
      }
      lazy val document = result.toHtmlDocument

      "given a tax year which can be found in ETMP" should {
        s"return status $OK" in {
          status(result) shouldBe OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the forecast tax calc summary page" in {
          document.title() shouldBe messages("htmlTitle", messages("forecast_taxCalc.heading"))
        }
      }

      "given a tax year which can not be found in ETMP" should {
        lazy val result = TestForecastTaxCalcSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          mockCalculationNotFoundNew(testMtditid)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

      }
      s"there is a downstream error which returns $NOT_FOUND" should {
        lazy val result = TestForecastTaxCalcSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          mockCalculationNotFoundNew(testMtditid)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }

      s"there is a downstream error which returns $INTERNAL_SERVER_ERROR" should {
        lazy val result = TestForecastTaxCalcSummaryController.show(testTaxYear)(fakeRequestWithActiveSession)

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          mockCalculationErrorNew(testMtditid)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "agent user" when {

    "show(taxYear) with forecast calculation fs enabled" when {

      lazy val result = {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockCalculationSuccessfulNew(testMtditidAgent)
        TestForecastTaxCalcSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))
      }
      lazy val document = result.toHtmlDocument

      "given a tax year which can be found in ETMP" should {
        s"return $OK" in {
          status(result) shouldBe OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "return the forecast tax calc summary page" in {
          document.title() shouldBe messages("htmlTitle.agent", messages("forecast_taxCalc.heading"))
        }
      }

      "given a tax year which can not be found in ETMP" should {

        lazy val result = TestForecastTaxCalcSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationNotFoundNew(testMtditidAgent)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }

      s"there is a downstream error which returns $NOT_FOUND" should {

        lazy val result = TestForecastTaxCalcSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationNotFoundNew(testMtditidAgent)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }

      s"there is a downstream error which returns $INTERNAL_SERVER_ERROR" should {

        lazy val result = TestForecastTaxCalcSummaryController.showAgent(testTaxYear)(fakeRequestConfirmedClient("AB123456C"))

        s"return status $INTERNAL_SERVER_ERROR" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockCalculationErrorNew(testMtditidAgent)
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
