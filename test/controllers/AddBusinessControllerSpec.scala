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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.BusinessNameForm
import forms.utils.SessionKeys
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.Results.Status
import play.api.test.Helpers._
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat
import services.IncomeSourceDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.AddBusiness

import scala.concurrent.Future


class AddBusinessControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching {

  val mockAddBusinessView: AddBusiness = mock(classOf[AddBusiness])
  val mockBusinessNameForm: BusinessNameForm = mock(classOf[BusinessNameForm])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.routes.AddBusinessController.submit()

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestAddBusinessNameController
    extends AddBusinessController(
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

  "AddBusinessController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAuthorisationException()

        val result = TestAddBusinessNameController.submit()(fakeRequestWithTimeoutSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

      //TODO - 200 != 303 result
    ".submit business" when {
      "redirect to the add business start date" when {
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
          redirectLocation(result) mustBe Some(routes.AddBusinessStartDate.show().url)
          session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
        }

        "the agent is authenticated and the business name entered is valid" in {

          disableAllSwitches()
          enable(IncomeSources)

          val validBusinessName: String = "Test Business"
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submitAgent()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> validBusinessName
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.AddBusinessStartDate.showAgent().url)
          session(result).get(SessionKeys.businessName) mustBe Some(validBusinessName)
        }

        "return to AddBusinessName when business name is empty as an Agent" in {

          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameEmpty: String = ""
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submitAgent()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameEmpty
          ))

          status(result) mustBe OK
          contentAsString(result) must include ("Enter your name or the name of your business")
        }

        "return to AddBusiness when business name is empty" in {

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

        "return to AddBusiness when business name is too long" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameLength: String = "ABCDEFGHIJKLMNOfgfgfgffgfgfgfgPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameLength
          ))

          status(result) mustBe OK
          println(result)
          contentAsString(result) must include("Business name must be 105 characters or fewer")
        }

        "return to AddBusiness and business name has invalid characters" in {
          disableAllSwitches()
          enable(IncomeSources)

          val invalidBusinessNameEmpty: String = "££"
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = TestAddBusinessNameController.submit()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
            SessionKeys.businessName -> invalidBusinessNameEmpty
          ))

          status(result) mustBe OK
          println(result)
          contentAsString(result) must include("Business name cannot include !, &quot;&quot;, * or ?")
        }
      }
    }
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
