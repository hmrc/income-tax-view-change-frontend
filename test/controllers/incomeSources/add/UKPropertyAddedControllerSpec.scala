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

package controllers.incomeSources.add

import config.featureswitch.{FeatureSwitching, IncomeSources, NavBarFs}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys.{addUkPropertyAccountingMethod, addUkPropertyStartDate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, PropertyDetailsModel}
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, IncomeSourceDetailsService, NextUpdatesService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants
import testUtils.TestSupport
import views.html.incomeSources.add.UKPropertyAdded

import java.time.LocalDate
import scala.concurrent.Future

class UKPropertyAddedControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNextUpdatesService with MockIncomeSourceDetailsService with FeatureSwitching {

  val view: UKPropertyAdded = app.injector.instanceOf[UKPropertyAdded]
  val mockDateService: DateService = mock(classOf[DateService])

  object TestUKPropertyAddedController extends UKPropertyAddedController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockDateService,
    mockIncomeSourceDetailsService,
    mockNextUpdatesService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    view
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  object UKPropertyAddedTestConstants {
  }

  "UKPropertyAddedController.getBackUrl()" should {
    "return the correct back URL for individual user" in {
      val expectedBackUrl = controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testPropertyIncomeId).url
      val backUrl = TestUKPropertyAddedController.getBackUrl(testPropertyIncomeId, isAgent = false)

      backUrl shouldBe expectedBackUrl
    }
    "return the correct back URL for agent user" in {
      val expectedBackUrl = controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testPropertyIncomeId).url
      val backUrl = TestUKPropertyAddedController.getBackUrl(testPropertyIncomeId, isAgent = true)

      backUrl shouldBe expectedBackUrl
    }
  }

  "UKPropertyAddedController.show" should {
    "return 200 OK" when {
      "FS enabled with newly added UK Property and obligations view model" in {
        disable(NavBarFs)
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()


        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2018, 4, 6))

        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
          Future(IncomeSourcesObligationsTestConstants.testObligationsViewModel))

        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result = TestUKPropertyAddedController.show(testSelfEmploymentId)(fakeRequestWithActiveSession.withSession(addUkPropertyStartDate -> "2022-01-01", addUkPropertyAccountingMethod -> "cash"))
        status(result) shouldBe OK
      }
    }
    "return 303 SEE_OTHER" when {
      "Income Sources FS is disabled" in {
        //        disable(NavBarFs)
        disable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockUKPropertyIncomeSource()

        val result = TestUKPropertyAddedController.show(testPropertyIncomeId)(fakeRequestWithNinoAndOrigin("PTA").withSession(addUkPropertyStartDate -> "2022-01-01", addUkPropertyAccountingMethod -> "cash"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
  }

  "UKPropertyAddedController.showAgent" should {
    "return 200 OK" when {
      "FS enabled with newly added UK Property and obligations view model" in {
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockUKPropertyIncomeSource()

        when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))

        when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
          Future(IncomeSourcesObligationsTestConstants.testObligationsViewModel))

        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

        val result = TestUKPropertyAddedController.showAgent(testPropertyIncomeId)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }
  }

  //  "UKPropertyAddedController.showAgent" should {
  //  }

}
