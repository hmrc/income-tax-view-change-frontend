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

import auth.FrontendAuthorisedFunctions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import mocks.views.agent.MockTaxYears
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.time.Millisecond
import play.api.Logger
import play.api.http.Status
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{DefaultMessagesControllerComponents, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.{CalculationService, IncomeSourceDetailsService}
import testUtils.TestSupport
import views.html.FinalTaxCalculationView

import scala.concurrent.Future
class FinalTaxCalculationControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with MockItvcErrorHandler with MockTaxYears with ImplicitDateFormatter with TestSupport with FeatureSwitching{

  val mockCalculationService: CalculationService = mock(classOf[CalculationService])
  val mockErrorHandler: ItvcErrorHandler = mock(classOf[ItvcErrorHandler])

  val testFinalTaxCalculationController = new FinalTaxCalculationController()(
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[FinalTaxCalculationView],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    mockErrorHandler,
    app.injector.instanceOf[FrontendAuthorisedFunctions],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[IncomeSourceDetailsService],
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[FrontendAppConfig]
  )

  val testCalcError = new LiabilityCalculationError(Status.OK, "Test message")
  when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
    .thenReturn(Future.successful(testCalcError))
  when(mockErrorHandler.showInternalServerError()(any()))
    .thenReturn(InternalServerError(HtmlFormat.empty))
  val taxYear = 2018

  "handle show request" should(
    "return unknown error" when (
      "an unconventional error occurs" in {
        val result: Future[Result] = testFinalTaxCalculationController.handleShowRequest(taxYear, mockErrorHandler, false)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    )
  )

  "agent submit" should(
    "return unknown error" when(
      "an unconventional error occurs" in {
        val result: Future[Result] = testFinalTaxCalculationController.agentSubmit(taxYear)(fakeRequestConfirmedClientWithCalculationId())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    )
  )
}
