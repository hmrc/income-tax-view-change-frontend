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
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import mocks.views.agent.MockIncomeSummary
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testMtditid, testNino, testTaxYear}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessful
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException
import views.html.IncomeBreakdown

import scala.concurrent.Future

class IncomeSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockItvcErrorHandler with MockIncomeSummary with MockFrontendAuthorisedFunctions with FeatureSwitching {

  val testYear: Int = 2020
  val isAgent: Boolean = true

  object TestIncomeSummaryController extends IncomeSummaryController(
    app.injector.instanceOf[IncomeBreakdown],
    mockAuthService,
    mockIncomeSourceDetailsService,
    mockCalculationService,
    app.injector.instanceOf[AuditingService],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    testAuthenticator
  )(
    ec,
    languageUtils,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents])

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  "showIncomeSummary" when {

    lazy val resultIndividual = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)
    lazy val document = resultIndividual.toHtmlDocument

    "given a tax year which can be found in ETMP" should {

      "return Status OK (200)" in {
        mockCalculationSuccessfulNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(resultIndividual) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(resultIndividual) shouldBe Some("text/html")
        charset(resultIndividual) shouldBe Some("utf-8")
      }

      "render the IncomeBreakdown page" in {
        document.title() shouldBe messages("htmlTitle", messages("income_breakdown.heading"))
      }
    }
    "given a tax year which can not be found in ETMP" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationNotFoundNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "there is a downstream error which return NOT_FOUND" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationNotFoundNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error which return INTERNAL_SERVER_ERROR" should {

      lazy val result = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationErrorNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }

  "showIncomeSummaryAgent" when {
    "given a tax year which can be found in ETMP" should {
      "return Status OK (200) with html content and right title" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetCalculationNew("XAIT00000000015", testNino, testYear)(liabilityCalculationModelSuccessful)
        mockIncomeBreakdown(testYear, IncomeBreakdownViewModel(liabilityCalculationModelSuccessful.calculation).get,
          controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testYear).url, isAgent)(HtmlFormat.empty)

        lazy val result: Future[Result] = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        setupMockGetCalculationNew("XAIT00000000015", testNino, testYear)(liabilityCalculationModelSuccessful)
        mockShowInternalServerError()
        val exception = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient(testNino)).failed.futureValue
        exception shouldBe an[InternalServerException]
        exception.getMessage shouldBe "IncomeSourceDetailsModel not created"
      }

    }

    "there is a downstream error which returns NOT_FOUND" should {
      "return Status Internal Server Error (500)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationNotFoundNew(nino = testNino, year = testYear)
        mockShowInternalServerError()

        lazy val result = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error which returns INTERNAL_SERVER_ERROR" should {
      "return Status Internal Server Error (500)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationErrorNew(nino = testNino, year = testYear)
        mockShowInternalServerError()

        lazy val result: Future[Result] = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient(testNino))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

