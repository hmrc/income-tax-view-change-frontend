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
import forms.BusinessStartDateCheckForm
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.incomeSources.add.AddBusinessStartDateCheck


class AddBusinessStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val validTestDate: String = "2022-11-11"

  val responseNo: String = BusinessStartDateCheckForm.responseNo

  val responseYes: String = BusinessStartDateCheckForm.responseYes

  val csrfToken: String = BusinessStartDateCheckForm.csrfToken

  object TestAddBusinessStartDateCheckController
    extends AddBusinessStartDateCheckController(
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[NinoPredicate],
      app.injector.instanceOf[AddBusinessStartDateCheck],
      MockIncomeSourceDetailsPredicate,
      MockNavBarPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      mockIncomeSourceDetailsService
    )(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[AgentItvcErrorHandler],
      app.injector.instanceOf[MessagesControllerComponents],
      ec
    )

  "AddBusinessStartDateCheckController" should {
    "redirect an individual to the home page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.HomeController.show().url)
      }
    }
    "show an individual the internal server error page" when {
      "there is no businessStartDate in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "show an agent the internal server error page" when {
      "there is no businessStartDate in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "show the add business start date check page with an OK response" when {
      "the session contains businessStartDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.show()(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.businessStartDate -> validTestDate))

        status(result) shouldBe OK
      }
    }
    "show the internal server error page" when {
      "an individual submits form with businessStartDate not in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            BusinessStartDateCheckForm.response -> responseYes
          ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "show the internal server error page" when {
      "an agent submits form with businessStartDate not in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.submitAgent()(
          fakeRequestConfirmedClient()
            .withFormUrlEncodedBody(
              BusinessStartDateCheckForm.response -> responseYes,
              BusinessStartDateCheckForm.csrfToken -> csrfToken
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "redirect back to add business start date page with businessStartDate removed from session" when {
        "No is submitted with the form" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit()(
            fakeRequestWithActiveSession
              .withHeaders(SessionKeys.businessStartDate -> validTestDate)
              .withFormUrlEncodedBody(
                BusinessStartDateCheckForm.response -> responseNo,
                BusinessStartDateCheckForm.csrfToken -> csrfToken
              ))


          result.futureValue.session.get(SessionKeys.businessStartDate).isDefined shouldBe false
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.AddBusinessStartDateController.show().url)
        }
      }
      "return BAD_REQUEST with an error summary" when {
        "form is submitted with neither radio option selected" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit()(
            fakeRequestWithActiveSession
              .withHeaders(SessionKeys.businessStartDate -> validTestDate)
              .withFormUrlEncodedBody())

          status(result) shouldBe BAD_REQUEST
          contentAsString(result) must include("Select yes if your business start date is correct")
        }
      }
      "return NOT_ACCEPTABLE response" when {
        "an invalid response is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val invalidResponse: String = "£££"

          val result = TestAddBusinessStartDateCheckController.submit()(
            fakeRequestWithActiveSession
              .withHeaders(SessionKeys.businessStartDate -> validTestDate)
              .withFormUrlEncodedBody(
                BusinessStartDateCheckForm.response -> invalidResponse,
                BusinessStartDateCheckForm.csrfToken -> csrfToken
              ))

          status(result) shouldBe NOT_ACCEPTABLE
        }
      }
      "redirect to add business trade page" when {
        "Yes is submitted with the form with a valid header" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit()(
            fakeRequestWithActiveSession
              .withHeaders(SessionKeys.businessStartDate -> validTestDate)
              .withFormUrlEncodedBody(
                BusinessStartDateCheckForm.response -> responseYes,
                BusinessStartDateCheckForm.csrfToken -> csrfToken
              ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.AddBusinessTradeController.show().url)
        }
      }
    }
  }
}