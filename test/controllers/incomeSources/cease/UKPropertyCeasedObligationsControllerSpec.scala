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

package controllers.incomeSources.cease

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates._
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockNextUpdatesService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.DateService
import services.helpers.ActivePropertyBusinessesHelper
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants
import testUtils.TestSupport
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import java.time.LocalDate
import scala.concurrent.Future

class UKPropertyCeasedObligationsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNextUpdatesService with MockIncomeSourceDetailsService with FeatureSwitching {

  val view: IncomeSourceCeasedObligations = app.injector.instanceOf[IncomeSourceCeasedObligations]
  val mockDateService: DateService = mock(classOf[DateService])

  object TestUKPropertyCeasedObligationsController$ extends UKPropertyCeasedObligationsController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NavBarPredicate],
    mockIncomeSourceDetailsService,
    view,
    mockDateService,
    mockNextUpdatesService
  )(appConfig,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec)


  "UKPropertyCeasedObligationsController.show" should {
    "return 200 OK" when {
      "FS enabled with newly added UK Property and obligations view model" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()


        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))

        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
          Future(IncomeSourcesObligationsTestConstants.viewModel))

        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result = TestUKPropertyCeasedObligationsController$.show()(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
    }
    "return 303 SEE_OTHER" when {
      "Income Sources FS is disabled" in {
        disable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()

        val result = TestUKPropertyCeasedObligationsController$.show()(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
    "return 500 ISE" when {
      "UK Property start date was not retrieved" in {
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBusinessIncomeSource()

        val result = TestUKPropertyCeasedObligationsController$.show()(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  " UKPropertyCeasedObligationsController.showAgent" should {
    "return 200 OK" when {
      "FS enabled with newly added UK Property and obligations view model" in {
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSourceWithCeasedUkProperty()

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))

        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
          Future(IncomeSourcesObligationsTestConstants.viewModel))

        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result = TestUKPropertyCeasedObligationsController$.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }
    "return 303 SEE_OTHER" when {
      "Income Sources FS is disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result = TestUKPropertyCeasedObligationsController$.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return 500 ISE" when {
      "income source not found" in {
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()

        val result = TestUKPropertyCeasedObligationsController$.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
