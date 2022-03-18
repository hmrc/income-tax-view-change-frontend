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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import models.liabilitycalculation.LiabilityCalculationError
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, _}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testTaxYear}
import testConstants.IncomeSourceDetailsTestConstants.businessIncome2018and2019
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException
import views.html.TaxCalcBreakdown

class TaxDueSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with
  MockIncomeSourceDetailsService with MockItvcErrorHandler with MockFrontendAuthorisedFunctions with FeatureSwitching {

  val testYear: Int = 2020
  val isAgent: Boolean = true


  object TestTaxDueSummaryController extends TaxDueSummaryController(
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[TaxCalcBreakdown],
    mockAuditingService,
    app.injector.instanceOf[BtaNavBarPredicate]
  )(appConfig,
    languageUtils,
    app.injector.instanceOf[MessagesControllerComponents],
    ec)

  "showTaxDueSummary" when {

    "given a tax year which can be found in ETMP" should {
      lazy val resultIndividual = TestTaxDueSummaryController.showTaxDueSummary(testTaxYear)(fakeRequestWithActiveSession)
      lazy val document = resultIndividual.toHtmlDocument

      "return Status OK (200)" in {
        mockCalculationSuccessFullNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(resultIndividual) shouldBe Status.OK
      }

      "return HTML" in {
        contentType(resultIndividual) shouldBe Some("text/html")
        charset(resultIndividual) shouldBe Some("utf-8")
      }

      "render the Tax Due page" in {
        document.title() shouldBe "Tax calculation - Business Tax account - GOV.UK"
      }
    }

    "given a tax year which can not be found in ETMP" should {
      lazy val resultIndividual = TestTaxDueSummaryController.showTaxDueSummary(testTaxYear)(fakeRequestWithActiveSession)
      "return Status ISE (500)" in {
        mockCalculationNotFoundNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(resultIndividual) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is a downstream error" should {
      lazy val resultIndividual = TestTaxDueSummaryController.showTaxDueSummary(testTaxYear)(fakeRequestWithActiveSession)

      "return Status Internal Server Error (500)" in {
        mockCalculationErrorNew("XAIT0000123456")
        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)
        status(resultIndividual) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "showAgentTaxDueSummary" when {
    "given a tax year which can be found in ETMP" should {
      "return Status OK (200) with HTML" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        mockCalculationSuccessFullNew("XAIT00000000015", "AA111111A", testYear)

        lazy val result = TestTaxDueSummaryController.showTaxDueSummaryAgent(testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(LiabilityCalculationError(404, "Not Found"))
        mockShowInternalServerError()

        val result = TestTaxDueSummaryController.showTaxDueSummaryAgent(testYear)(fakeRequestConfirmedClient()).failed.futureValue
        result shouldBe an[InternalServerException]
        result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }

    "there is a downstream error" should {
      "return Status Internal Server Error (500)" in {
        lazy val result = TestTaxDueSummaryController.showTaxDueSummaryAgent(testYear)(fakeRequestConfirmedClient())
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()
        setupMockGetCalculationNew("XAIT00000000015", "AA111111A", testYear)(LiabilityCalculationError(500, "Some Error"))
        mockShowInternalServerError()

        setupMockGetIncomeSourceDetails()(businessIncome2018and2019)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

