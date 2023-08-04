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
import forms.BusinessStartDateCheckForm
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.scalatest.matchers.must.Matchers._
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddBusinessStartDateCheck

import scala.concurrent.Future


class AddBusinessStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val testBusinessStartDate: String = "2022-11-11"
  val testBusinessAccountingPeriodStartDate: String = "2022-11-11"
  val testBusinessAccountingPeriodEndDate: String = "2023-04-05"

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
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[CustomNotFoundError]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      mockImplicitDateFormatter,
      dateService,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      app.injector.instanceOf[MessagesControllerComponents],
      ec
    )

  "AddBusinessStartDateCheckController.show()" should {
    "return the not found view" when {
      "Income Sources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestAddBusinessStartDateCheckController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddBusinessStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is no businessStartDate in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      "the session contains businessStartDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateCheckController.show()(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate))


        status(result) shouldBe OK
      }
    }

    "AddBusinessStartDateCheckController.submit()" should {
      "redirect back to add business start date page with businessStartDate removed from session" when {
        "No is submitted with the form" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit(
            fakeRequestWithActiveSession
              .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
              .withFormUrlEncodedBody(
                BusinessStartDateCheckForm.response -> responseNo,
                BusinessStartDateCheckForm.csrfToken -> csrfToken
              ))

          result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessStartDateController.show().url)
        }
      }
      "return BAD_REQUEST with an error summary" when {
        "form is submitted with neither radio option selected" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit(
            fakeRequestWithActiveSession
              .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
              .withFormUrlEncodedBody())

          status(result) shouldBe BAD_REQUEST
        }
      }

      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val invalidResponse: String = "£££"

        val result = TestAddBusinessStartDateCheckController.submit(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody(
              BusinessStartDateCheckForm.response -> invalidResponse,
              BusinessStartDateCheckForm.csrfToken -> csrfToken
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "redirect to add business trade page" when {
        "Yes is submitted with the form with a valid session" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddBusinessStartDateCheckController.submit(
            fakeRequestWithActiveSession
              .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
              .withFormUrlEncodedBody(
                BusinessStartDateCheckForm.response -> responseYes,
                BusinessStartDateCheckForm.csrfToken -> csrfToken
              ))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.show().url)
          result.futureValue.session.get(SessionKeys.addBusinessStartDate) shouldBe Some(testBusinessStartDate)
          result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodStartDate) shouldBe Some(testBusinessAccountingPeriodStartDate)
          result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodEndDate) shouldBe Some(testBusinessAccountingPeriodEndDate)
        }
      }
    }
  }


  "AddBusinessStartDateCheckController.showAgent()" should {

    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is no businessStartDate in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      "the session contains businessStartDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.showAgent()(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate))

        status(result) shouldBe OK
      }
    }
  }

  "AddBusinessStartDateCheckController.submitAgent()" should {
    "redirect back to add business start date page with businessStartDate removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.submitAgent(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody(
              BusinessStartDateCheckForm.response -> responseNo,
              BusinessStartDateCheckForm.csrfToken -> csrfToken
            ))

        result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessStartDateController.showAgent().url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.submitAgent(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
    }

    "an invalid response is submitted" in {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

      val invalidResponse: String = "£££"

      val result = TestAddBusinessStartDateCheckController.submitAgent(
        fakeRequestConfirmedClient()
          .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
          .withFormUrlEncodedBody(
            BusinessStartDateCheckForm.response -> invalidResponse,
            BusinessStartDateCheckForm.csrfToken -> csrfToken
          ))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to add business trade page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateCheckController.submitAgent(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody(
              BusinessStartDateCheckForm.response -> responseYes,
              BusinessStartDateCheckForm.csrfToken -> csrfToken
            ))


        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent().url)
        result.futureValue.session.get(SessionKeys.addBusinessStartDate) shouldBe Some(testBusinessStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodStartDate) shouldBe Some(testBusinessAccountingPeriodStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodEndDate) shouldBe Some(testBusinessAccountingPeriodEndDate)
      }
    }
  }
}

