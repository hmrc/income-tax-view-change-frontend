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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import controllers.routes
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData.{businessNameField, businessTradeField}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{IncomeSourceDetailsService, SessionService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.add.AddBusinessTrade

import scala.concurrent.Future


class AddBusinessTradeControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching
  with MockSessionService {


  val mockAddBusinessTradeView: AddBusinessTrade = mock(classOf[AddBusinessTrade])
  val mockBusinessTradeForm: BusinessTradeForm = mock(classOf[BusinessTradeForm])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])


  val validBusinessTrade: String = "Test Business Trade"
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  object TestAddBusinessTradeController
    extends AddBusinessTradeController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      addBusinessTradeView = app.injector.instanceOf[AddBusinessTrade],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      sessionService = app.injector.instanceOf[SessionService],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  "AddBusinessTradeController" should {
    "redirect a user back to the custom error page" when {
      "the individual is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessTradeController.show(isAgent = false, isChange = false)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithTimeoutSession)

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
        setupMockCreateSession(true)
        setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some("testBusinessTrade")))

        val result: Future[Result] = TestAddBusinessTradeController.show(isAgent = false, isChange = false)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      "show correct page when agent valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        setupMockCreateSession(true)
        setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some("testBusinessTrade")))

        val result: Future[Result] = TestAddBusinessTradeController.show(isAgent = true, isChange = false)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }


    ".submit trade" when {
      "redirect to the add business address page" when {
        "the individual is authenticated and the business trade entered is valid" in {

          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(businessTradeField, validBusinessTrade, journeyType, Right(true))
          setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some(validBusinessTrade)))

          mockSessionService.getMongoKeyTyped(businessTradeField, journeyType).futureValue shouldBe Right(Some(validBusinessTrade))

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithActiveSessionWithBusinessName.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> validBusinessTrade
            ))
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url)
        }

        "the agent is authenticated and the business trade entered is valid" in {

          disableAllSwitches()
          enable(IncomeSources)

          val validBusinessTrade: String = "Test Trade"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(businessTradeField, validBusinessTrade, journeyType, Right(true))
          setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some(validBusinessTrade)))

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClientwithBusinessName().withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> validBusinessTrade
            ))

          mockSessionService.getMongoKeyTyped(businessTradeField, journeyType).futureValue shouldBe Right(Some(validBusinessTrade))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url)
        }
      }

      "return to add business trade page" when {
        "trade name is same as business name" in {
          disableAllSwitches()
          enable(IncomeSources)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockCreateSession(true)
          val businessTrade: String = "Test Name"
          setupMockGetSessionKeyMongoTyped[String](Right(Some(businessTrade)))
          setupMockGetSessionKeyMongoTyped[String](Right(Some(businessTrade)))


          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> businessTrade
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClientwithBusinessName()
              .withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> sameNameAsTrade
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = true)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = false, isChange = false)(fakeRequestWithActiveSession.withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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
          setupMockCreateSession(true)
          setupMockGetSessionKeyMongo(Right(Some("testBusinessTrade")))
          setupMockSetSessionKeyMongo(Right(true))

          val result: Future[Result] =
            TestAddBusinessTradeController.submit(isAgent = true, isChange = false)(fakeRequestConfirmedClient().withFormUrlEncodedBody(
              BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty
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

      val result: Future[Result] = TestAddBusinessTradeController.show(isAgent = false, isChange = false)(fakeRequestWithActiveSession)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.HomeController.show().url)

    }
  }
}
