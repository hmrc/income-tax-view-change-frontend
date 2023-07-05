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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import controllers.routes
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.IncomeSourceDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.{AddBusiness, AddBusinessTrade}

import scala.concurrent.Future



class AddBusinessTradeControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching {


  val mockAddBusinessTradeView: AddBusinessTrade = mock(classOf[AddBusinessTrade])
  val mockBusinessTradeForm: BusinessTradeForm = mock(classOf[BusinessTradeForm])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.incomeSources.add.routes.AddBusinessTradeController.submit()

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestAddBusinessTradeController
    extends AddBusinessTradeController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      addBusinessTradeView = app.injector.instanceOf[AddBusinessTrade],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  object TestAddBusinessNameNameController$
    extends AddBusinessNameController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      addBusinessView = app.injector.instanceOf[AddBusiness],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  "AddBusinessTradeController" should {
    "redirect a user back to the custom error page" when {
      "the indivdual is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessTradeController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessTradeController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    ".show" should {
      "show correct page when user valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessTradeController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessTradeController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }


    ".submit trade" when {
      "redirect to the add business address page" when {
        "the individual is authenticated and the business trade entered is valid" in {

          disableAllSwitches()
          enable(IncomeSources)

          val validBusinessTrade: String = "Test Trade"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)


          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSessionWithBusinessName.withFormUrlEncodedBody(
              SessionKeys.businessTrade -> validBusinessTrade
            ))
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.AddBusinessAddressController.show().url)
          session(result).get(SessionKeys.businessTrade) mustBe Some(validBusinessTrade)
        }

        "the agent is authenticated and the business trade entered is valid" in {

          disableAllSwitches()
          enable(IncomeSources)

          val validBusinessTrade: String = "Test Trade"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClientwithBusinessName().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> validBusinessTrade
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.AddBusinessAddressController.showAgent().url)
          session(result).get(SessionKeys.businessTrade) mustBe Some(validBusinessTrade)
        }
      }

      "return to add business trade page" when {
        "trade name is same as business name" in {
          disableAllSwitches()
          enable(IncomeSources)

          val sameNameAsTrade = "Test Name"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSessionWithBusinessName.withFormUrlEncodedBody(
            SessionKeys.businessTrade -> sameNameAsTrade
          ))

          status(result) mustBe OK
          contentAsString(result) must include("You cannot enter the same trade and same business name")
        }
        "trade name is same as business name for agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          val sameNameAsTrade = "Test Name"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClientwithBusinessName().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> sameNameAsTrade
          ))

          status(result) mustBe OK
          contentAsString(result) must include("You cannot enter the same trade and same business name")
        }

        "trade name contains invalid characters" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "££"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade cannot include !, &quot;&quot;, * or ?")
        }
        "trade name contains invalid characters as agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "££"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade cannot include !, &quot;&quot;, * or ?")
        }

        "trade name is empty" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = ""
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Enter the trade of your business")
        }
        "trade name is empty as agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = ""
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Enter the trade of your business")
        }

        "trade name is too short" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "A"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade must have at least two letters")
        }
        "trade name is too short as agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "A"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade must have at least two letters")
        }

        "trade name is too long" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "This trade name is far too long to be accepted"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade must be 35 characters or fewer")
        }
        "trade name is too long as agent" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessTradeEmpty: String = "This trade name is far too long to be accepted"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessTradeController.agentSubmit()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
            SessionKeys.businessTrade -> invalidBusinessTradeEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business trade must be 35 characters or fewer")
        }
      }
    }

    "when feature switch is disabled" in {
      disableAllSwitches()

      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessTradeController.show()(fakeRequestWithActiveSession)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.HomeController.show().url)

    }
  }
}
