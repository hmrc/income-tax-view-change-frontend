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

import config.featureswitch.FeatureSwitch.switches
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import play.api.http.Status
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import testConstants.BusinessDetailsTestConstants.{ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2}
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}

import scala.concurrent.Future

class CeaseIncomeSourceControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter
  with MockIncomeSourceDetailsService with MockNavBarEnumFsPredicate with MockFrontendAuthorisedFunctions with FeatureSwitching with TestSupport {

  val controller = new CeaseIncomeSourceController(
    app.injector.instanceOf[views.html.incomeSources.cease.CeaseIncomeSources],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate]
  )(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "The CeaseIncomeSourcesController" should {
    "redirect an individual back to the home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        isDisabled(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }

    "redirect an agent back to the home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        isDisabled(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER

      }
    }

    "redirect an individual to the cease an income source page" when {
      "user has a sole trader businesses, UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
          .thenReturn(Right(CeaseIncomeSourcesViewModel(
            soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
            ukProperty = Some(ceaseUkPropertyDetailsViewModel),
            foreignProperty = None,
            ceasedBusinesses = Nil)))

        val result = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
      }
    }

    "redirect an agent to the cease an income source page" when {
      "agent's client has a sole trader businesses, UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
          .thenReturn(Right(CeaseIncomeSourcesViewModel(
            soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
            ukProperty = Some(ceaseUkPropertyDetailsViewModel),
            foreignProperty = Some(ceaseForeignPropertyDetailsViewModel),
            ceasedBusinesses = Nil)))

        val result = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
        status(result) shouldBe Status.OK
      }
    }

    "show error page" when {
      "error response from service" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
          .thenReturn(Left(MissingFieldException("Trading Name")))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
