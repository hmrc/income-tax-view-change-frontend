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

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.nextUpdates.ObligationsModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.add.BusinessAddedObligations

import scala.concurrent.Future


class BusinessAddedObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }


  object TestObligationsController extends BusinessAddedObligationsController(
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    nextUpdatesService = mockNextUpdatesService,
    obligationsView = app.injector.instanceOf[BusinessAddedObligations]
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    dateService
  )


  "BusinessAddedObligationsController" should {
    "redirect a user back to the custom error page" when {
      "the indivdual is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestObligationsController.show(None)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestObligationsController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    ".show" should {
      "show correct page when individual valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        //mockSingleBusinessIncomeSourceWithDeadlines()
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          Some("Test name"),
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(ObligationsModel(Seq.empty)))

        val result: Future[Result] = TestObligationsController.show(Some("123"))(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockSingleBusinessIncomeSourceWithDeadlines()
        val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel("", Some("2022"), List(BusinessDetailsModel(
          Some("123"),
          None,
          Some("Test name"),
          None, None, None
        )), List.empty)
        setupMockGetIncomeSourceDetails()(sources)
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(ObligationsModel(Seq.empty)))

        val result: Future[Result] = TestObligationsController.showAgent(Some("123"))(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }
  }
}
