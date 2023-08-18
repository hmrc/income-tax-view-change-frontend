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
import forms.IncomeSourcesFormsSpec.include
import forms.models.DateFormElement
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{Call, MessagesControllerComponents}
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate


class AddIncomeSourceStartDateControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val dayField = "income-source-start-date.day"
  val monthField = "income-source-start-date.month"
  val yearField = "income-source-start-date.year"

  val testDay = "01"
  val testMonth = "01"
  val testYear = "2022"

  val currentDate = dateService.getCurrentDate()

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  object TestAddIncomeSourceStartDateController extends AddIncomeSourceStartDateController(
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    addIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError],
    incomeSourceDetailsService = mockIncomeSourceDetailsService
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateFormatter = mockImplicitDateFormatter,
    dateService = app.injector.instanceOf[DateService],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec
  )

  "Individual: AddIncomeSourceStartDateController.handleRequest" should {
    "render the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showUKProperty(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showForeignProperty(isAgent = false, isUpdate = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Foreign property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showForeignProperty(isAgent = false, isUpdate = true)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.ForeignPropertyCheckDetailsController.show().url
        status(result) shouldBe OK
      }
    }
    "render the Add UK property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showUKProperty(isAgent = false, isUpdate = true)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckUKPropertyDetailsController.show().url
        status(result) shouldBe OK
      }
    }
    "render the Add Business start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = false, isUpdate = true)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.show().url
        status(result) shouldBe OK
      }
    }
  }
  "Individual: AddIncomeSourceStartDateController.handleSubmitRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "return BAD_REQUEST" when {
      "an invalid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession)

        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = false, isUpdate = false).url)
      }
    }
    "redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = false, isUpdate = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = false, isUpdate = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "display date too far ahead error message" when {
      "input date is more than 7 days in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDate = DateFormElement(
          currentDate.plusDays(8)
        ).date

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)()(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(s"The date your business started trading must be before $maximumAllowableDatePlusOneDay")
      }
    }
    "not display date too far ahead error message" when {
      "input date is 7 days or less in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDate = DateFormElement(
          currentDate.plusDays(7)
        ).date

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false).url)
      }
    }
    "display invalid date error message" when {
      "input date is invalid" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = "99"
        val testMonth = "99"
        val testYear = "9999"

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date your business started trading must be a real date")
      }
    }
    "display missing day error message" when {
      "only month and year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = "01"
        val testYear = "2020"

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day")
      }
    }

    "display missing year error message" when {
      "only day and month is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = "01"
        val testMonth = "01"
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a year")
      }
    }
    "display missing day and month error message" when {
      "only year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = ""
        val testYear = "2021"

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a month")
      }
    }
    "display missing day and year error message" when {
      "only month is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = "01"
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a year")
      }
    }
    "display missing month and year error message" when {
      "only day is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = "01"
        val testMonth = ""
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = false, isUpdate = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a month and a year")
      }
    }
    "redirect to the Add UK Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = false, isUpdate = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = false, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Foreign Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = false, isUpdate = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Business Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = false, isUpdate = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = false, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "Agent: AddIncomeSourceStartDateController.handleRequest" should {
    "render the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = true, isUpdate = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = true, isUpdate = false)(fakeRequestConfirmedClient())


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showUKProperty(isAgent = true, isUpdate = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showForeignProperty(isAgent = true, isUpdate = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        status(result) shouldBe OK
      }
    }
    "render the Add Foreign property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showForeignProperty(isAgent = true, isUpdate = true)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.ForeignPropertyCheckDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
    "render the Add UK property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showUKProperty(isAgent = true, isUpdate = true)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckUKPropertyDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
    "render the Add Business start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness(isAgent = true, isUpdate = true)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
  }
  "Agent: AddIncomeSourceStartDateController.handleSubmitRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "return BAD_REQUEST" when {
      "an invalid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient())

        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = true, isUpdate = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = true, isUpdate = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = true, isUpdate = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "display date too far ahead error message" when {
      "input date is more than 7 days in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDate = DateFormElement(
          currentDate.plusDays(8)
        ).date

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(s"The date your business started trading must be before $maximumAllowableDatePlusOneDay")
      }
    }
    "not display date too far ahead error message" when {
      "input date is 7 days or less in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDate = DateFormElement(
          currentDate.plusDays(7)
        ).date

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = false).url)
      }
    }
    "display invalid date error message" when {
      "input date is invalid" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = "99"
        val testMonth = "99"
        val testYear = "9999"

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date your business started trading must be a real date")
      }
    }
    "display missing day error message" when {
      "only month and year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = ""
        val testMonth = "01"
        val testYear = "2020"

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day")
      }
    }

    "display missing year error message" when {
      "only day and month is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = "01"
        val testMonth = "01"
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a year")
      }
    }
    "display missing day and month error message" when {
      "only year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = ""
        val testMonth = ""
        val testYear = "2021"

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a month")
      }
    }
    "display missing day and year error message" when {
      "only month is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = ""
        val testMonth = "01"
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a year")
      }
    }
    "display missing month and year error message" when {
      "only day is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val testDay = "01"
        val testMonth = ""
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = true, isUpdate = false)(
          fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a month and a year")
      }
    }
    "redirect to the Add UK Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitUKProperty(isAgent = true, isUpdate = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = true, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Foreign Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty(isAgent = true, isUpdate = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Business Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = true, isUpdate = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = true, isUpdate = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
  }
}
