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

package controllers.incomeSources.manage

import config.featureswitch.FeatureSwitch.switches
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.incomeSources.manage.ManageIncomeSourceController
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
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAuthAgentSuccessWithSaUtrResponse, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import play.api.http.Status
import models.incomeSourceDetails.viewmodels.ViewIncomeSourcesViewModel
import testConstants.BusinessDetailsTestConstants.{ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2, viewBusinessDetailsViewModel}
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel, viewUkPropertyDetailsViewModel}
import views.html.incomeSources.manage.ManageIncomeSources

import scala.concurrent.Future

class ManageIncomeSourceControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

  object TestManageIncomeSourceController
    extends ManageIncomeSourceController(
      manageIncomeSources = app.injector.instanceOf[ManageIncomeSources],
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "The ManageIncomeSourcesController" should {
    "redirect an individual back to the home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        isDisabled(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestManageIncomeSourceController.show()(fakeRequestWithActiveSession)
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

        val result: Future[Result] = TestManageIncomeSourceController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER

      }
    }

    "redirect an individual to the view and manage income sources page" when {
      "user has a sole trader business and a UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
          .thenReturn(Right(ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
            viewUkProperty = Some(viewUkPropertyDetailsViewModel),
            viewForeignProperty = None,
            viewCeasedBusinesses = Nil)))

        val result = TestManageIncomeSourceController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
      }
    }

    "redirect an agent to the view and manage income sources page" when {
      "agent's client has a sole trader business and a UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
          .thenReturn(Right(ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
            viewUkProperty = Some(viewUkPropertyDetailsViewModel),
            viewForeignProperty = None,
            viewCeasedBusinesses = Nil)))

        val result = TestManageIncomeSourceController.showAgent()(fakeRequestConfirmedClient("AB123456C"))
        status(result) shouldBe Status.OK
      }
    }

    "show error page" when {
      "error response from service for individual" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
          .thenReturn(Left(MissingFieldException("Trading Name")))

        val result: Future[Result] = TestManageIncomeSourceController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "error response from service for agent" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
          .thenReturn(Left(MissingFieldException("Trading Name")))

        val result: Future[Result] = TestManageIncomeSourceController.showAgent()(fakeRequestConfirmedClient("AB123456C"))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
