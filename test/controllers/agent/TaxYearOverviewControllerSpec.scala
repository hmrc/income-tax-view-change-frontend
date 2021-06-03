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

import java.time.LocalDate

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import assets.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import assets.FinancialDetailsTestConstants.{financialDetailsModel, testFinancialDetailsErrorModel}
import audit.mocks.MockAuditingService
import config.featureswitch.{AgentViewer, FeatureSwitching}
import implicits.ImplicitDateFormatterImpl
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockIncomeSourceDetailsService, MockReportDeadlinesService}
import mocks.views.MockTaxYearOverview
import models.calculation.{CalcDisplayError, CalcDisplayNoDataFound, CalcOverview}
import models.reportDeadlines.{ObligationsModel, ReportDeadlinesErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

class TaxYearOverviewControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with MockFinancialDetailsService
  with FeatureSwitching with MockTaxYearOverview with MockCalculationService with MockIncomeSourceDetailsService
  with MockReportDeadlinesService with MockItvcErrorHandler with MockAuditingService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  class Setup {

    val testYear: Int = 2020

    val controller: TaxYearOverviewController = new TaxYearOverviewController(
      taxYearOverview = taxYearOverview,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      financialDetailsService = mockFinancialDetailsService,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      reportDeadlinesService = mockReportDeadlinesService,
      auditingService = mockAuditingService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ImplicitDateFormatterImpl],
      app.injector.instanceOf[ExecutionContext],
      itvcErrorHandler = mockItvcErrorHandler
    )
  }

  "backUrl" should {
    "return to the home page" in new Setup {
      controller.backUrl() shouldBe controllers.agent.routes.TaxYearsController.show().url
    }
  }

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the agent viewer feature switch is disabled" should {
      "return Not Found" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

        status(result) shouldBe NOT_FOUND
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the agent viewer feature switch is enabled" when {
      "there was a problem retrieving income source details for the user" should {
        "throw an internal server exception" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockErrorIncomeSource()
          mockShowInternalServerError()

          intercept[InternalServerException](await(controller.show(taxYear = testYear)(fakeRequestConfirmedClient())))
            .message shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
        }
      }
      "there was a problem retrieving the calculation for the user" should {
        "return technical difficulties" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayError)
          mockShowInternalServerError()

          val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some(HTML)
        }
      }
      "there was a problem retrieving the charges for the user" should {
        "return technical difficulties" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
          setupMockGetFinancialDetails(testYear, "AA111111A")(testFinancialDetailsErrorModel)
          mockShowInternalServerError()

          val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some(HTML)
        }
      }
      "there was a problem retrieving the updaes for the user" should {
        "return technical difficulties" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
          setupMockGetFinancialDetails(testYear, "AA111111A")(financialDetailsModel(testYear))
          mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
            ReportDeadlinesErrorModel(INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR")
          )
          mockShowInternalServerError()

          val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentType(result) shouldBe Some(HTML)
        }
      }
      "no calculation data was returned" should {
        "show the tax year overview page" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(CalcDisplayNoDataFound)
          setupMockGetFinancialDetails(testYear, "AA111111A")(financialDetailsModel(testYear))
          mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
            ObligationsModel(Nil)
          )
          mockTaxYearOverview(
            taxYear = testYear,
            calcOverview = None,
            documentDetailsWithDueDates = financialDetailsModel(testYear).getAllDocumentDetailsWithDueDates,
            obligations = ObligationsModel(Nil),
            backUrl = controllers.agent.routes.TaxYearsController.show().url
          )(HtmlFormat.empty)

          val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }

      "all calls to retrieve data were successful" should {
        "show the tax year overview page" in new Setup {
          enable(AgentViewer)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockBothIncomeSources()
          setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
          setupMockGetFinancialDetails(testYear, "AA111111A")(financialDetailsModel(testYear))
          mockGetReportDeadlines(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
            ObligationsModel(Nil)
          )
          mockTaxYearOverview(
            taxYear = testYear,
            calcOverview = Some(CalcOverview(calculationDataSuccessModel, None)),
            documentDetailsWithDueDates = financialDetailsModel(testYear).getAllDocumentDetailsWithDueDates,
            obligations = ObligationsModel(Nil),
            backUrl = controllers.agent.routes.TaxYearsController.show().url
          )(HtmlFormat.empty)

          val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }
      }
    }
  }
}
