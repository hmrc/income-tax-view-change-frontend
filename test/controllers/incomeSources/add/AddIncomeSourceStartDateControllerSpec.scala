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

  "Individual: AddBusinessStartDateController.handleRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness()(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusiness()(fakeRequestWithActiveSession)


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showUKProperty()(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.showForeignProperty()(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        status(result) shouldBe OK
      }
    }
  }
  "Individual: AddBusinessStartDateController.handleSubmitRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(fakeRequestWithActiveSession)

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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(fakeRequestWithActiveSession.withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(fakeRequestWithActiveSession.withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(fakeRequestWithActiveSession)

        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness.url)
      }
    }
    "redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty.url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submitUKProperty()(fakeRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKProperty.url)
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignProperty.url)
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKProperty()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitForeignProperty()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKProperty()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusiness()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKProperty()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a month and a year")
      }
    }
  }

  "Agent: AddBusinessStartDateController.handleRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusinessAgent()(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showSoleTraderBusinessAgent()(fakeRequestConfirmedClient())


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showUKPropertyAgent()(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        status(result) shouldBe OK
      }
    }
    "redirect to the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.showForeignPropertyAgent()(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        status(result) shouldBe OK
      }
    }
  }
  "Agent: AddBusinessStartDateController.handleSubmitRequest" should {
    "redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(fakeRequestConfirmedClient())

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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST" when {
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusinessAgent.url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitForeignPropertyAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignPropertyAgent.url)
        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submitUKPropertyAgent()(fakeRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showUKPropertyAgent.url)
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitForeignPropertyAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.showForeignPropertyAgent.url)
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKPropertyAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitForeignPropertyAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKPropertyAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitSoleTraderBusinessAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
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

        val result = TestAddIncomeSourceStartDateController.submitUKPropertyAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a month and a year")
      }
    }
  }
}
