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

import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import testConstants.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import config.FrontendAppConfig
import config.featureswitch._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockCalculationService, MockIncomeSourceDetailsService}
import mocks.views.agent.MockTaxYears
import models.calculation.{Calculation, CalculationErrorModel, CalculationResponseModelWithYear}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

class TaxYearsControllerSpec extends TestSupport
  with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with MockTaxYears
  with FeatureSwitching {

  trait Setup {
    val controller = new TaxYearsController(taxYears, mockAuthService, mockIncomeSourceDetailsService)(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      mockItvcErrorHandler
    )
  }

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.show()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
		"there was a problem retrieving income source details for the user" should {
			"throw an internal server exception" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockErrorIncomeSource()
				mockShowInternalServerError()

				val result = controller.show()(fakeRequestConfirmedClient()).failed.futureValue
				result shouldBe an[InternalServerException]
				result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
			}
		}
		"there is no firstAccountingPeriodEndDate from income source details" should {
			"show the tax years page" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockNoIncomeSources()

				mockTaxYears(years = List(2022, 2021, 2020, 2019, 2018), controllers.agent.routes.HomeController.show().url)(HtmlFormat.empty)

				val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

				status(result) shouldBe OK
				contentType(result) shouldBe Some(HTML)
			}
		}
		"all data is returned successfully" should {
			"show the tax years page" in new Setup {
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockBothIncomeSources()

				mockTaxYears(years = List(2022, 2021, 2020, 2019, 2018), controllers.agent.routes.HomeController.show().url)(HtmlFormat.empty)

				val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

				status(result) shouldBe OK
				contentType(result) shouldBe Some(HTML)
			}
		}
  }

}
