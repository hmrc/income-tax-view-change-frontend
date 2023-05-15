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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.incomeSources.add.AddIncomeSourceController
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.incomeSourceDetails.viewmodels.AddIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.BusinessDetailsTestConstants.{businessDetailsViewModel, businessDetailsViewModel2}
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetailsViewModel, ukPropertyDetailsViewModel}
import testUtils.TestSupport

import scala.concurrent.Future

class AddIncomeSourceControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

  val controller = new AddIncomeSourceController(
    app.injector.instanceOf[views.html.incomeSources.add.AddIncomeSources],
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


  "The AddIncomeSourcesController" should {

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
      "redirect an individual to the add income source page" when {
        "user has a Sole Trader Businesses and a UK property" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockBothIncomeSources()
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

          when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
            .thenReturn( Right(AddIncomeSourcesViewModel(
              soleTraderBusinesses = List(businessDetailsViewModel, businessDetailsViewModel2),
              ukProperty = Some(ukPropertyDetailsViewModel),
              foreignProperty = None,
              ceasedBusinesses = Nil)))

          val result = controller.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
        }
      }
      "redirect an agent to the add income source page" when {
        "user has a Sole Trader Businesses, a UK property and a Foreign Property" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockBothIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
            .thenReturn(Right(AddIncomeSourcesViewModel(
              soleTraderBusinesses = List(businessDetailsViewModel, businessDetailsViewModel2),
              ukProperty = Some(ukPropertyDetailsViewModel),
              foreignProperty = Some(foreignPropertyDetailsViewModel),
              ceasedBusinesses = Nil)))

          val result = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
          status(result) shouldBe Status.OK
        }
      }
    }
  }
}
