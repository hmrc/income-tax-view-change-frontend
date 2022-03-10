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

package controllers

import audit.AuditingService
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import mocks.views.agent.MockIncomeSummary
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{charset, contentType, _}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessFull
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.InternalServerException
import views.html.IncomeBreakdown

import scala.concurrent.Future

class IncomeSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockItvcErrorHandler with MockIncomeSummary with FeatureSwitching {

  val testYear: Int = 2020
  val isAgent: Boolean = true

  /*class Setup {
    val testYear: Int = 2020
    val isAgent: Boolean = true

    val controller: IncomeSummaryController = new IncomeSummaryController(
      incomeBreakdown = incomeBreakdown,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      incomeSourceDetailsService = mockIncomeSourceDetailsService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      mockItvcErrorHandler
    )
  }*/

  object TestIncomeSummaryController extends IncomeSummaryController(
    app.injector.instanceOf[IncomeBreakdown],
    app.injector.instanceOf[AuthorisedFunctions],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockIncomeSourceDetailsService,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[AuditingService],
    app.injector.instanceOf[BtaNavBarPredicate],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
  )(
    ec,
    languageUtils,
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents])

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "showIncomeSummary for Individual" when {

    //    lazy val resultAgent = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)
    lazy val resultIndividual = TestIncomeSummaryController.showIncomeSummary(testTaxYear)(fakeRequestWithActiveSession)
    lazy val document = resultIndividual.toHtmlDocument

    "given a tax year which can be found in ETMP" should {

      "return Status OK (200)" in {
        mockCalculationSuccessFullNew(testMtditid)
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(resultIndividual) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(resultIndividual) shouldBe Some("text/html")
        charset(resultIndividual) shouldBe Some("utf-8")
      }

      "render the IncomeBreakdown page" in {
        document.title() shouldBe "Income - Business Tax account - GOV.UK"
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

  "showIncomeSummary for Agent" when {
    "given a tax year which can be found in ETMP" should {
      "return Status OK (200) with html content and right title" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(liabilityCalculationModelSuccessFull)
        mockIncomeBreakdown(testYear, IncomeBreakdownViewModel(liabilityCalculationModelSuccessFull.calculation).get,
          controllers.agent.routes.TaxYearOverviewController.show(testYear).url, isAgent)(HtmlFormat.empty)

        lazy val result: Future[Result] = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(liabilityCalculationModelSuccessFull)
        mockShowInternalServerError()
        val exception = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient()).failed.futureValue
        exception shouldBe an[InternalServerException]
        exception.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }

    }

    "there is a downstream error which returns NOT_FOUND" should {
      "return Status Internal Server Error (500)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationNotFoundNew(nino = "AA111111A", year = testYear)
        mockShowInternalServerError()

        lazy val result = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error which returns INTERNAL_SERVER_ERROR" should {
      "return Status Internal Server Error (500)" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationErrorNew(nino = "AA111111A", year = testYear)
        mockShowInternalServerError()

        lazy val result: Future[Result] = TestIncomeSummaryController.showIncomeSummaryAgent(testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

