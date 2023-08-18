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
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import forms.utils.SessionKeys
import forms.utils.SessionKeys.addUkPropertyStartDate
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import scala.concurrent.Future


class AddIncomeSourceStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val testStartDate: String = "2022-11-11"
  val testBusinessAccountingPeriodStartDate: String = "2022-11-11"
  val testBusinessAccountingPeriodEndDate: String = "2023-04-05"

  val responseNo: String = AddIncomeSourceStartDateCheckForm.responseNo

  val responseYes: String = AddIncomeSourceStartDateCheckForm.responseYes

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])


  object TestAddIncomeSourceStartDateCheckController
    extends AddIncomeSourceStartDateCheckController(authenticate = MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      addIncomeSourceStartDateCheckView = app.injector.instanceOf[AddIncomeSourceStartDateCheck],
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError],
      languageUtils = languageUtils
    )(
      app.injector.instanceOf[FrontendAppConfig],
      dateService,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler]
    ) {

    val heading: String = messages("dateForm.check.heading")
    val titleAgent: String = s"${messages("htmlTitle.agent", heading)}"
    val title: String = s"${messages("htmlTitle", heading)}"
  }

  "Individual: AddIncomeSourceStartDateCheckController.handleRequest" should {
    "render the custom not found error view" when {
      "Income Sources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "render the custom not found error view" when {
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKProperty(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      s"calling Business Start Date Check Page but session does not contain key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      s"calling Business Start Date Check Page and session contains key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate))

        status(result) shouldBe OK
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      s"calling UK Property Start Date Check Page but session does not contain key: ${SessionKeys.addUkPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showUKProperty(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      s"calling Foreign Property Start Date Check Page and session contains key: ${SessionKeys.foreignPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false)(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate))

        status(result) shouldBe OK
      }
    }

  }


  "Individual - Sole Trader Business - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    "redirect back to add business start date page with businessStartDate removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = false, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
        fakePostRequestWithActiveSession
          .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the session value of addBusinessStartDate cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the session value of an income source start date cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to add business trade page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.show().url)
        result.futureValue.session.get(SessionKeys.addBusinessStartDate) shouldBe Some(testStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodStartDate) shouldBe Some(testBusinessAccountingPeriodStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodEndDate) shouldBe Some(testBusinessAccountingPeriodEndDate)
      }
    }
  }

  "Individual - UK Property - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    s"redirect back to add UK Property start date page with ${SessionKeys.addUkPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.addUkPropertyStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty(isAgent = false, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = false, isUpdate = false)(
        fakePostRequestWithActiveSession
          .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      s"the session value of ${SessionKeys.addUkPropertyStartDate} cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to UK Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.show().url)
      }
    }
  }

  "Individual - Foreign Property - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    s"redirect back to add Foreign Property start date page with ${SessionKeys.foreignPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.foreignPropertyStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty(isAgent = false, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = false, isUpdate = false)(
        fakePostRequestWithActiveSession
          .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      s"the session value of ${SessionKeys.foreignPropertyStartDate} cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.foreignPropertyStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to Foreign Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url)
      }
    }
  }



  "Agent - Sole Trader Business - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    "redirect back to add business start date page with businessStartDate removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = true, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
        fakePostRequestConfirmedClient()
          .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the session value of addBusinessStartDate cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the session value of an income source start date cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to add business trade page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent().url)
        result.futureValue.session.get(SessionKeys.addBusinessStartDate) shouldBe Some(testStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodStartDate) shouldBe Some(testBusinessAccountingPeriodStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodEndDate) shouldBe Some(testBusinessAccountingPeriodEndDate)
      }
    }
  }

  "Agent - UK Property - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    s"redirect back to add UK Property start date page with ${SessionKeys.addUkPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.addUkPropertyStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty(isAgent = true, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = true, isUpdate = false)(
        fakePostRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      s"the session value of ${SessionKeys.addUkPropertyStartDate} cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to UK Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url)
      }
    }
  }

  "Agent - Foreign Property - AddIncomeSourceStartDateCheckController.handleSubmitRequest" should {
    s"redirect back to add Foreign Property start date page with ${SessionKeys.foreignPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.foreignPropertyStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty(isAgent = true, isUpdate = false).url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
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

      val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = true, isUpdate = false)(
        fakePostRequestConfirmedClient()
          .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR" when {
      s"the session value of ${SessionKeys.foreignPropertyStartDate} cannot be parsed as a LocalDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.foreignPropertyStartDate -> "INVALID_DATE")
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to Foreign Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient()
            .withSession(SessionKeys.foreignPropertyStartDate -> testStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url)
      }
    }
  }
}

