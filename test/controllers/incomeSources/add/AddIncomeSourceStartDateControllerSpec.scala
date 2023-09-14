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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.IncomeSourcesFormsSpec.include
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{Call, MessagesControllerComponents}
import play.api.test.Helpers._
import services.{DateService, SessionService}
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

  val testStartDate: String = s"$testYear-$testMonth-$testDay"

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
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    sessionService = app.injector.instanceOf[SessionService]
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateFormatter = mockImplicitDateFormatter,
    dateService = app.injector.instanceOf[DateService],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec
  )

  "Individual: AddIncomeSourceStartDateController.show" should {
    s"return ${Status.OK}: render the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakeRequestWithActiveSession)


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddBusinessNameController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession.withSession(SessionKeys.addBusinessStartDate -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession.withSession(SessionKeys.addUkPropertyStartDate -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckUKPropertyDetailsController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession.withSession(SessionKeys.foreignPropertyStartDate -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.ForeignPropertyCheckDetailsController.show().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date Change page with form filled" when {
      s"session contains key: ${SessionKeys.addBusinessStartDate} " in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController
          .show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(
            fakeRequestWithActiveSession.withSession(SessionKeys.addBusinessStartDate -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.show().url
        document.getElementById("income-source-start-date.day").attr("value") shouldBe "1"
        document.getElementById("income-source-start-date.month").attr("value") shouldBe "1"
        document.getElementById("income-source-start-date.year").attr("value") shouldBe "2022"
        status(result) shouldBe OK
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"session contains IncomeSourceStartDate with invalid format for the change journey" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController
          .show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(
            fakeRequestWithActiveSession.withSession(SessionKeys.addBusinessStartDate -> "INVALID_FORMAT"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Individual: AddIncomeSourceStartDateController.submit" should {
    s"return ${Status.OK}: render Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "an invalid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession)

        status(result) shouldBe BAD_REQUEST
      }
      "input date is more than 7 days in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDate = DateFormElement(
          currentDate.plusDays(8)
        ).date

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)()(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(s"The date your business started trading must be before $maximumAllowableDatePlusOneDay")
      }
      "input date is invalid" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = "99"
        val testMonth = "99"
        val testYear = "9999"

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date your business started trading must be a real date")
      }
      "only month and year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = "01"
        val testYear = "2020"

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day")
      }
      "only year is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = ""
        val testYear = "2021"

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a month")
      }
      "only month is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = ""
        val testMonth = "01"
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The date must include a day and a year")
      }
      "only day is entered" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDay = "01"
        val testMonth = ""
        val testYear = ""

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
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
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect successfully and not display date too far ahead error message" when {
      "start date for income source is 7 days in the future" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val testDate = DateFormElement(
          currentDate.plusDays(7)
        ).date

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "Agent: AddIncomeSourceStartDateController.show" should {
    s"return ${Status.OK}: render the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())


        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddBusinessNameController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)


        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(fakeRequestConfirmedClient().withSession(ForeignProperty.startDateSessionKey -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.ForeignPropertyCheckDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK property start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true)(fakeRequestConfirmedClient().withSession(UkProperty.startDateSessionKey -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckUKPropertyDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(fakeRequestConfirmedClient().withSession(SelfEmployment.startDateSessionKey -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.showAgent().url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business start date Change page with form filled" when {
      s"session contains key: ${SessionKeys.addBusinessStartDate} " in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController
          .show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(
            fakeRequestConfirmedClient().withSession(SessionKeys.addBusinessStartDate -> testStartDate))

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("add-business-start-date.heading"))
        document.getElementById("back").attr("href") shouldBe routes.CheckBusinessDetailsController.showAgent().url
        document.getElementById("income-source-start-date.day").attr("value") shouldBe "1"
        document.getElementById("income-source-start-date.month").attr("value") shouldBe "1"
        document.getElementById("income-source-start-date.year").attr("value") shouldBe "2022"
        status(result) shouldBe OK
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"session contains IncomeSourceStartDate with invalid format for the change journey" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController
          .show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(
            fakeRequestConfirmedClient().withSession(SessionKeys.addBusinessStartDate -> "INVALID_FORMAT"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Agent: AddIncomeSourceStartDateController.submit" should {
    s"return ${Status.OK}: redirect to the Custom Not Found Error page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title should include(messages("error.custom.heading"))
        status(result) shouldBe OK
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "an invalid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
  }
}
