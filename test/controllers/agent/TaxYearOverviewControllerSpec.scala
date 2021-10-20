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

import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import testConstants.CalcBreakdownTestConstants.{calculationDataSuccessModel, calculationDisplaySuccessModel}
import testConstants.FinancialDetailsTestConstants.{financialDetailsModel, testFinancialDetailsErrorModel}
import audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService}
import mocks.views.agent.MockTaxYearOverview
import models.calculation.{CalcDisplayError, CalcDisplayNoDataFound, CalcOverview}
import models.financialDetails.DocumentDetailWithDueDate
import models.nextUpdates.{ObligationsModel, NextUpdatesErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class TaxYearOverviewControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with MockFinancialDetailsService
  with FeatureSwitching with MockTaxYearOverview with MockCalculationService with MockIncomeSourceDetailsService
  with MockNextUpdatesService with MockItvcErrorHandler with MockAuditingService {

  class Setup {

    val testYear: Int = 2020

    val controller: TaxYearOverviewController = new TaxYearOverviewController(
      taxYearOverview = taxYearOverview,
      authorisedFunctions = mockAuthService,
      calculationService = mockCalculationService,
      financialDetailsService = mockFinancialDetailsService,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      nextUpdatesService = mockNextUpdatesService,
      auditingService = mockAuditingService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
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
		"there was a problem retrieving income source details for the user" should {
			"throw an internal server exception" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockErrorIncomeSource()
				mockShowInternalServerError()
				val result = controller.show(taxYear = testYear)(fakeRequestConfirmedClient()).failed.futureValue
				result shouldBe an[InternalServerException]
				result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
			}
		}
		"there was a problem retrieving the calculation for the user" should {
			"return technical difficulties" in new Setup {
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
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
				setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(testFinancialDetailsErrorModel)
				mockShowInternalServerError()

				val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

				status(result) shouldBe INTERNAL_SERVER_ERROR
				contentType(result) shouldBe Some(HTML)
			}
		}
		"there was a problem retrieving the updaes for the user" should {
			"return technical difficulties" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
				setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
				mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
					NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR")
				)
				mockShowInternalServerError()

				val result: Future[Result] = controller.show(taxYear = testYear)(fakeRequestConfirmedClient())

				status(result) shouldBe INTERNAL_SERVER_ERROR
				contentType(result) shouldBe Some(HTML)
			}
		}
		"no calculation data was returned" should {
			"show the tax year overview page" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(CalcDisplayNoDataFound)
				setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
				mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
					ObligationsModel(Nil)
				)
				mockTaxYearOverview(
					taxYear = testYear,
					calcOverview = None,
					documentDetailsWithDueDates = financialDetailsModel(testYear)
						.getAllDocumentDetailsWithDueDates ++ List(DocumentDetailWithDueDate(financialDetailsModel(testYear).documentDetails.head,
						financialDetailsModel(testYear).documentDetails.head.interestEndDate, true)),
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
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()
				setupMockGetCalculation("AA111111A", testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
				setupMockGetFinancialDetailsWithTaxYearAndNino(testYear, "AA111111A")(financialDetailsModel(testYear))
				mockgetNextUpdates(fromDate = LocalDate.of(testYear - 1, 4, 6), toDate = LocalDate.of(testYear, 4, 5))(
					ObligationsModel(Nil)
				)
				mockTaxYearOverview(
					taxYear = testYear,
					calcOverview = Some(CalcOverview(calculationDataSuccessModel)),
					documentDetailsWithDueDates = financialDetailsModel(testYear)
						.getAllDocumentDetailsWithDueDates ++ List(DocumentDetailWithDueDate(financialDetailsModel(testYear).documentDetails.head,
						financialDetailsModel(testYear).documentDetails.head.interestEndDate, true)),
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
