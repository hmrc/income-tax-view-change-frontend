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

  val testBusinessStartDate: String = "2022-11-11"
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

  "AddIncomeSourceStartDateCheckController.show()" should {
    "render the custom not found error view" when {
      "Income Sources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
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

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness()(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      "the session contains businessStartDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusiness(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate))


        status(result) shouldBe OK
      }
    }

    "AddIncomeSourceStartDateCheckController.submit()" should {

      "redirect back to add business start date page with businessStartDate removed from session" when {
        "No is submitted with the form" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
            fakeRequestWithActiveSession
              .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
              .withFormUrlEncodedBody(
                AddIncomeSourceStartDateCheckForm.response -> responseNo
              ))

          result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url)
        }
      }
      "return BAD_REQUEST with an error summary" when {
        "form is submitted with neither radio option selected" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
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

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
          fakeRequestWithActiveSession
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
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

          val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
            fakeRequestWithActiveSession
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

          val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
            fakeRequestWithActiveSession
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

          val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(
            fakeRequestWithActiveSession
              .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
              .withFormUrlEncodedBody(
                AddIncomeSourceStartDateCheckForm.response -> responseYes
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


  "AddIncomeSourceStartDateCheckController.showAgent()" should {
    "render the custom not found error view" when {
      "Income Sources FS is disabled" in {
        disableAllSwitches()

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusiness(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is no businessStartDate in session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusinessAgent(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 OK" when {
      "the session contains businessStartDate" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.showSoleTraderBusinessAgent()(
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

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        result.futureValue.session.get(SessionKeys.addBusinessStartDate).isDefined shouldBe false
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url)
      }
    }
    "return BAD_REQUEST with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
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

      val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
        fakeRequestConfirmedClient()
          .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
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

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
          fakeRequestConfirmedClient()
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

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
          fakeRequestConfirmedClient()
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

        val result = TestAddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent(
          fakeRequestConfirmedClient()
            .withSession(SessionKeys.addBusinessStartDate -> testBusinessStartDate)
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))


        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent().url)
        result.futureValue.session.get(SessionKeys.addBusinessStartDate) shouldBe Some(testBusinessStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodStartDate) shouldBe Some(testBusinessAccountingPeriodStartDate)
        result.futureValue.session.get(SessionKeys.addBusinessAccountingPeriodEndDate) shouldBe Some(testBusinessAccountingPeriodEndDate)
      }
    }
  }

  "Individual - AddUKPropertyBusinessController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKProperty(fakeRequestWithActiveSession
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddIncomeSourceStartDateCheckController.title
        document.select("legend:nth-child(1)").text shouldBe TestAddIncomeSourceStartDateCheckController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKProperty(fakeRequestWithActiveSession
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKProperty(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - AddUKPropertyStartDateController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.show().url}" when {
      "user confirms the date is correct" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddIncomeSourceStartDateCheckController.submitUKProperty(fakeRequestWithActiveSession
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> "Yes"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.show().url)
      }
      s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url}" when {
        "user confirms the date is incorrect" in {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          enable(IncomeSources)
          mockSingleBusinessIncomeSource()

          when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(HttpResponse(OK)))

          lazy val result: Future[Result] = {
            TestAddIncomeSourceStartDateCheckController.submitUKProperty(fakeRequestWithActiveSession
              .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
              .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> "No"))
          }

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url)
          result.futureValue.session.get(addUkPropertyStartDate) shouldBe None
        }
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        lazy val result: Future[Result] = {
          TestAddIncomeSourceStartDateCheckController.submitUKProperty(fakeRequestWithActiveSession
            .withMethod("POST")
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> ""))
        }
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

  "Agent - AddUKPropertyBusinessController.showAgent" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKPropertyAgent(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddIncomeSourceStartDateCheckController.titleAgent
        document.select("legend:nth-child(1)").text shouldBe TestAddIncomeSourceStartDateCheckController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKPropertyAgent(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showUKPropertyAgent(fakeRequestConfirmedClient()
          .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15"))
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - AddUKPropertyStartDateController.submitAgent" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url}" when {
      "user confirms the date is correct" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddIncomeSourceStartDateCheckController.submitUKPropertyAgent(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> "Yes"))
        }

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url)
      }
      "user confirms the date is incorrect" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddIncomeSourceStartDateCheckController.submitUKPropertyAgent(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> "No"))
        }

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        lazy val result: Future[Result] = {
          TestAddIncomeSourceStartDateCheckController.submitUKPropertyAgent(fakeRequestConfirmedClient()
            .withSession(SessionKeys.addUkPropertyStartDate -> "2022-04-15")
            .withFormUrlEncodedBody(AddIncomeSourceStartDateCheckForm.response -> ""))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
  "ForeignPropertyStartDateCheckController for individual" should {
    "show customNotFoundErrorView page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showForeignProperty(fakeRequestWithActiveSession)
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
    }
    "show foreign property start date check page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.showForeignProperty(fakeRequestWithActiveSession
          .withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddIncomeSourceStartDateCheckController.title
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showForeignProperty(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "input is empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBothIncomeSources()

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(
          fakeRequestWithActiveSession.withFormUrlEncodedBody().withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01"))

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("add-foreign-property-start-date-check.error"))
      }
    }
    "redirect to the different pages as to response" when {
      "an individual gives a valid input - Yes" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show()

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> "Yes"
          ).withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postAction.url)
      }

      "an individual gives a valid input - No" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignProperty(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> "No"
          ).withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postAction.url)
      }
    }
  }
  "ForeignPropertyStartDateCheckController for Agent" should {
    "show customNotFoundErrorView page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        val expectedContent: String = TestAddIncomeSourceStartDateCheckController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "show foreign property start date check page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.showForeignPropertyAgent(fakeRequestConfirmedClient()
          .withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestAddIncomeSourceStartDateCheckController.titleAgent
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "input is empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignPropertyAgent(
          fakeRequestConfirmedClient().withFormUrlEncodedBody().withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01"))
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("add-foreign-property-start-date-check.error"))
      }
    }
    "redirect to the different pages as to response" when {
      "an Agent gives a valid input - Yes" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent()

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignPropertyAgent(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> "Yes"
          ).withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postActionAgent.url)
      }

      "an Agent gives a valid input - No" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent

        val result = TestAddIncomeSourceStartDateCheckController.submitForeignPropertyAgent(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> "No"
          ).withSession(SessionKeys.foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postActionAgent.url)
      }
    }
  }
}

