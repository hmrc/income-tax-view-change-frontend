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

import audit.AuditingService
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
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAuthAgentSuccessWithSaUtrResponse, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import play.api.http.Status
import models.incomeSourceDetails.viewmodels.ViewIncomeSourcesViewModel
import play.api.test.FakeRequest
import services.SessionService
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
      auditingService = app.injector.instanceOf[AuditingService]
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      sessionService = app.injector.instanceOf[SessionService],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  "The ManageIncomeSourcesController" should {
    s"return ${Status.SEE_OTHER} and redirect to the home page" when {
      "the IncomeSources FS is disabled for an Individual" in {
        val result = runTest(isAgent = false, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }

      "the IncomeSources FS is disabled for an Agent" in {
        val result = runTest(isAgent = true, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }

    s"return ${Status.OK}" when {
      "Individual has a sole trader business and a UK property" in {
        val result = runTest(isAgent = false)
        status(result) shouldBe Status.OK
      }

      "Agent has a sole trader business and a UK property" in {
        val result = runTest(isAgent = true)
        status(result) shouldBe Status.OK
      }
    }

    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "error response from service for individual" in {
        val result = runTest(isAgent = false, errorResponse = true)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "error response from service for agent" in {
        val result = runTest(isAgent = true, errorResponse = true)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  def runTest(isAgent: Boolean,
              errorResponse: Boolean = false,
              disableIncomeSources: Boolean = false
             ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

    mockBothIncomeSources()

    when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any()))
      .thenReturn(
        if (errorResponse)
          Left(MissingFieldException("Trading Name"))
        else
          Right(
            ViewIncomeSourcesViewModel(
              viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
              viewUkProperty = Some(viewUkPropertyDetailsViewModel),
              viewForeignProperty = None,
              viewCeasedBusinesses = Nil
            )
          )
      )

    TestManageIncomeSourceController.show(isAgent)(
      if (isAgent)
        fakeRequestConfirmedClient()
      else
        fakeRequestWithActiveSession
    )
  }

  override def beforeEach(): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
  }
}
