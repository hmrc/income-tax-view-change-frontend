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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Cease, JourneyType}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockSessionService}
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.BusinessDetailsTestConstants.{ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2}
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData
import testUtils.TestSupport

import scala.concurrent.Future

class CeaseIncomeSourceControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter
  with MockIncomeSourceDetailsService with MockNavBarEnumFsPredicate with MockFrontendAuthorisedFunctions with FeatureSwitching with TestSupport
  with MockSessionService {

  val controller = new CeaseIncomeSourceController(
    app.injector.instanceOf[views.html.incomeSources.cease.CeaseIncomeSources],
    mockAuthService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    sessionService = mockSessionService,
    testAuthenticator
  )(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  "The CeaseIncomeSourcesController" should {

    "redirect user back to the home page" when {
      def testFSDisabled(isAgent: Boolean): Unit = {
        disableAllSwitches()
        mockSingleBISWithCurrentYearAsMigrationYear()

        if (isAgent) {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          val result = controller.showAgent()(fakeRequestConfirmedClient())
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        } else {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          val result = controller.show()(fakeRequestWithActiveSession)
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
      }

      "user is an individual and the IncomeSources FS is disabled " in {
        testFSDisabled(isAgent = false)
      }
      "user is an agent and the IncomeSources FS is disabled " in {
        testFSDisabled(isAgent = true)
      }
    }

    "redirect user to the cease an income source page" when {
      def testCeaseIncomeSourcePage(isAgent: Boolean): Unit = {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, SelfEmployment)))))
        setupMockDeleteSession(true)

        when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
          .thenReturn(Right(CeaseIncomeSourcesViewModel(
            soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
            ukProperty = Some(ceaseUkPropertyDetailsViewModel),
            foreignProperty = Some(ceaseForeignPropertyDetailsViewModel),
            ceasedBusinesses = Nil)))

        if (isAgent) {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          val result = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
          status(result) shouldBe Status.OK
        } else {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          val result = controller.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
        }

      }

      "user is an individual and has a sole trader business and a UK property" in {
        testCeaseIncomeSourcePage(isAgent = false)
      }

      "user is an agent and the client has a sole trader business and a UK property" in {
        testCeaseIncomeSourcePage(isAgent = true)
      }
    }

    "show error page" when {
      def testErrorResponse(isAgent: Boolean): Unit = {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothIncomeSources()

        when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
          .thenReturn(Left(MissingFieldException("Trading Name")))

        if (isAgent) {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        } else {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "error response from service for individual" in {
        testErrorResponse(isAgent = false)
      }

      "error response from service for agent" in {
        testErrorResponse(isAgent = true)
      }
    }
  }
}
