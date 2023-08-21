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
import forms.BusinessNameForm
import forms.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.IncomeSourceDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.add.AddBusinessName

import scala.concurrent.Future


class AddBusinessNameControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching {

  val mockAddBusinessNameView: AddBusinessName = mock(classOf[AddBusinessName])
  val mockBusinessNameForm: BusinessNameForm = mock(classOf[BusinessNameForm])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submit()

  object TestAddBusinessNameController
    extends AddBusinessNameController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      addBusinessView = app.injector.instanceOf[AddBusinessName],
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

  "Individual - ADD - AddBusinessNameController.show" should {
    "return 200 OK" when {
      "the individual is authenticated" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)

        status(result) mustBe OK
      }
    }

    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAuthorisationException()
        val result = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "redirect to the session timeout page" when {
      "the user has timed out" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessNameController.show()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.show().url)
      }
    }
  }

  "Individual - ADD - AddBusinessNameController.submit" should {
    "return 303 and redirect to add business start date" when {
      "the individual is authenticated and the business name entered is valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        val validBusinessName: String = "Test Business"
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          SessionKeys.businessName -> validBusinessName
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url)
        session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
      }

      "show AddBusinessName with error" when {
        "Business name is empty" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameEmpty: String = ""
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Enter your name or the name of your business")
        }

        "Business name is too long" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameLength: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameLength
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business name must be 105 characters or fewer")
        }

        "Business name has invalid characters" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameEmpty: String = "££"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include("Business name cannot include !, &quot;&quot;, * or ?")
        }
      }
    }
  }
  "Agent - Add - AddBusinessNameController.show" should {
    "return 200 OK" when {
      "the agent is authenticated" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
        status(result) mustBe OK
      }
    }
    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAgentAuthorisationException()
        val result = TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
      }
    }
    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.showAgent.url)
      }
    }
  }

  "Agent - Add - AddBusinessNameController.submit" should {
    "return 303 and redirect to add business start date" when {
      "the agent is authenticated and the business name entered is valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        val validBusinessName: String = "Test Business"
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.submitAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          SessionKeys.businessName -> validBusinessName
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url)
        session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
      }
    }
    "return to AddBusinessName when business name is empty as an Agent" in {
      disableAllSwitches()
      enable(IncomeSources)

      val invalidBusinessNameEmpty: String = ""
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.submitAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
        SessionKeys.businessName -> invalidBusinessNameEmpty
      ))

      status(result) mustBe OK
      contentAsString(result) must include("Enter your name or the name of your business")
    }
  }

  "Individual - Change - AddBusinessNameController.changeBusinessName" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessNameController.changeBusinessName()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
    "when feature switch is disabled" in {
      disableAllSwitches()

      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.changeBusinessName()(fakeRequestWithActiveSession)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.HomeController.show().url)
    }
  }

  "Individual - Change - AddBusinessNameController.submitChange" should {
    "change business name" when {
      "redirect to the check business details" when {
        "the individual is authenticated" in {
          disableAllSwitches()
          enable(IncomeSources)

          val validBusinessName: String = "Test Business"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submitChange()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
            SessionKeys.businessName -> validBusinessName,
            SessionKeys.businessStartDate -> "21-04-2020",
            SessionKeys.businessTrade -> "Plumber",
            SessionKeys.addBusinessAddressLine1 -> "10 Test Road",
            SessionKeys.addBusinessPostalCode -> "TE5 T69",
            SessionKeys.addIncomeSourcesAccountingMethod -> "Quarterly"
          )
          )

          redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url)
          session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
        }
      }
    }
    "show empty error when business name is empty" in {
      disableAllSwitches()
      enable(IncomeSources)

      val invalidBusinessNameEmpty: String = ""
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.submitChange()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
        SessionKeys.businessName -> invalidBusinessNameEmpty
      ))

      status(result) mustBe OK
      contentAsString(result) must include("Enter your name or the name of your business")
    }

    "show length error when business name is too long" in {
      disableAllSwitches()
      enable(IncomeSources)

      val invalidBusinessNameLength: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.submitChange()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
        SessionKeys.businessName -> invalidBusinessNameLength
      ))

      status(result) mustBe OK
      contentAsString(result) must include("Business name must be 105 characters or fewer")
    }

    "show invalid error when business name has invalid characters" in {
      disableAllSwitches()
      enable(IncomeSources)

      val invalidBusinessNameEmpty: String = "££"
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.submitChange()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
        SessionKeys.businessName -> invalidBusinessNameEmpty
      ))

      status(result) mustBe OK
      contentAsString(result) must include("Business name cannot include !, &quot;&quot;, * or ?")
    }
  }

  "Agent - Change - AddBusinessNameController.changeBusinessNameAgent" should {
    "return 200 OK" when {
      "the agent is authenticated" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
        status(result) mustBe OK
      }
    }
    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAgentAuthorisationException()
        val result = TestAddBusinessNameController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
      }
    }
    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.changeBusinessNameAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.showAgent.url)
      }
    }
  }

  "Agent - Add - AddBusinessNameController.submitChangeAgent" should {
    "return 303 and redirect to add business check details" when {
      "the agent is authenticated and the business name entered is valid" in {
        disableAllSwitches()
        enable(IncomeSources)

        val validBusinessName: String = "Test Business"
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestAddBusinessNameController.submitChangeAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          SessionKeys.businessName -> validBusinessName
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url)
        session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
      }
    }
    "return to AddBusinessName when business name is empty as an Agent" in {
      disableAllSwitches()
      enable(IncomeSources)

      val invalidBusinessNameEmpty: String = ""
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

      val result: Future[Result] = TestAddBusinessNameController.submitChangeAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
        SessionKeys.businessName -> invalidBusinessNameEmpty
      ))

      status(result) mustBe OK
      contentAsString(result) must include("Enter your name or the name of your business")
    }
  }
}


