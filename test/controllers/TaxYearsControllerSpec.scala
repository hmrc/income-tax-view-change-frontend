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

import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import mocks.views.agent.MockTaxYears
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.CalculationService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testTaxYear}
import testConstants.IncomeSourceDetailsTestConstants._
import testConstants.MessagesLookUp
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import views.html.TaxYears

import scala.concurrent.Future

class TaxYearsControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with MockItvcErrorHandler with MockTaxYears with ImplicitDateFormatter with TestSupport with FeatureSwitching {

  val calculationService: CalculationService = mock[CalculationService]

  object TestTaxYearsController extends TaxYearsController(
    app.injector.instanceOf[TaxYears],
    mockAuthService,
    mockIncomeSourceDetailsService
  )(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    mockItvcErrorHandler,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NavBarPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter]
  )

  lazy val CalcMessages = new MessagesLookUp.Calculation(testTaxYear)

  ".viewTaxYears" when {
    "called with an authenticated HMRC-MTD-IT user and successfully retrieved income source" when {
      "and firstAccountingPeriodEndDate is missing from income sources" should {
        "return an Internal Server Error (500)" in {

          setupMockGetIncomeSourceDetails()(businessIncome2018and2019)

          lazy val result = TestTaxYearsController.showTaxYears()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }


      "successfully retrieves income sources and and display tax year page" should {
        "return an OK (200)" in {
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          lazy val result = TestTaxYearsController.showTaxYears()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {

        setupMockAuthorisationException()
        val result = TestTaxYearsController.showTaxYears()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "show agent tax years" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestTaxYearsController.showAgentTaxYears()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = TestTaxYearsController.showAgentTaxYears()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = TestTaxYearsController.showAgentTaxYears()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "there was a problem retrieving income source details for the user" should {
      "throw an internal server exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        mockShowInternalServerError()

        val result = TestTaxYearsController.showAgentTaxYears()(fakeRequestConfirmedClient()).failed.futureValue
        result shouldBe an[InternalServerException]
        result.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }
    "there is no firstAccountingPeriodEndDate from income source details" should {
      "throw and exception" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNoIncomeSources()

        mockTaxYears(years = List(2022, 2021, 2020, 2019, 2018), controllers.routes.HomeController.showAgent().url)(HtmlFormat.empty)

        val result: Future[Result] = TestTaxYearsController.showAgentTaxYears()(fakeRequestConfirmedClient())

        the[Exception] thrownBy status(result) should have message "User missing first accounting period information"
      }
    }
    "all data is returned successfully" should {
      "show the tax years page" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockBothIncomeSources()

        mockTaxYears(years = List(2022, 2021, 2020, 2019, 2018), controllers.routes.HomeController.showAgent().url)(HtmlFormat.empty)

        val result: Future[Result] = TestTaxYearsController.showAgentTaxYears()(fakeRequestConfirmedClient())

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
  }
}
