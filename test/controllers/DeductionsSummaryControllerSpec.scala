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

import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NavBarFromNinoPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockCalculationService
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testMtditid, testTaxYear}
import testUtils.TestSupport

import scala.concurrent.Future

class DeductionsSummaryControllerSpec extends TestSupport with MockCalculationService
  with MockAuthenticationPredicate with FeatureSwitching with MockAuditingService with MockItvcErrorHandler {

  val testYear: Int = 2020

  object TestDeductionsSummaryController extends DeductionsSummaryController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    mockAuditingService,
    app.injector.instanceOf[views.html.DeductionBreakdown],
    app.injector.instanceOf[NavBarFromNinoPredicate],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]
  )(
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    languageUtils
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "showDeductionsSummary" when {

    "all calc data available" should {

      lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testTaxYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "render the Allowances and Deductions page" in {
        mockCalculationSuccessFullNew(testMtditid)
        status(result) shouldBe Status.OK
        document.title() shouldBe "Allowances and deductions - Business Tax account - GOV.UK"
        document.getElementById("total-value").text() shouldBe "£17,500.99"
      }
    }

    "no calc data available" should {

      lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testTaxYear)(fakeRequestWithActiveSession)
      lazy val document = result.toHtmlDocument

      "render the Allowances and Deductions page" in {
        mockCalculationSuccessMinimalNew(testMtditid)
        status(result) shouldBe Status.OK
        document.title() shouldBe "Allowances and deductions - Business Tax account - GOV.UK"
        document.getElementById("total-value").text() shouldBe "£0.00"
      }
    }
    "calc returns NOT_FOUND" should {

      lazy val result = TestDeductionsSummaryController.showDeductionsSummary(testTaxYear)(fakeRequestWithActiveSession)

      "render error page" in {
        mockCalculationNotFoundNew(testMtditid)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "showDeductionsSummaryAgent" when {
    "render the Allowances and Deductions page with full calc data" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationSuccessFullNew(taxYear = testYear)

      val result: Future[Result] = TestDeductionsSummaryController.showDeductionsSummaryAgent(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
      val document = result.toHtmlDocument

      status(result) shouldBe Status.OK
      document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
      document.getElementById("total-value").text() shouldBe "£17,500.99"
    }

    "render the Allowances and Deductions page with no calc data" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationSuccessMinimalNew(taxYear = testYear)

      val result: Future[Result] = TestDeductionsSummaryController.showDeductionsSummaryAgent(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))
      val document = result.toHtmlDocument

      status(result) shouldBe Status.OK
      document.title() shouldBe "Allowances and deductions - Your client’s Income Tax details - GOV.UK"
      document.getElementById("total-value").text() shouldBe "£0.00"
    }

    "render error page when NOT_FOUND is returned from calc" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockCalculationNotFoundNew(year = testYear)
      mockShowInternalServerError()

      val result: Future[Result] = TestDeductionsSummaryController.showDeductionsSummaryAgent(taxYear = testYear)(fakeRequestConfirmedClient("AB123456C"))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}



