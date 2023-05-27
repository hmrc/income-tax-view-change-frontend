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
import forms.models.DateFormElement
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{Call, MessagesControllerComponents}
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.incomeSources.add.AddBusinessStartDate


class AddBusinessStartDateControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val dayField = "add-business-start-date.day"
  val monthField = "add-business-start-date.month"
  val yearField = "add-business-start-date.year"

  val currentDate = dateService.getCurrentDate()

  val maximumAllowableDate = mockImplicitDateFormatter
    .longDate(dateService.getCurrentDate().plusWeeks(1))

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestAddBusinessStartDateController
    extends AddBusinessStartDateController(
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[NinoPredicate],
      app.injector.instanceOf[AddBusinessStartDate],
      MockIncomeSourceDetailsPredicate,
      MockNavBarPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      mockIncomeSourceDetailsService
    )(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[DateService],
      app.injector.instanceOf[ImplicitDateFormatterImpl],
      app.injector.instanceOf[AgentItvcErrorHandler],
      app.injector.instanceOf[MessagesControllerComponents],
      ec = ec
    )

  "AddBusinessStartDateController" should {
    "redirect an individual to the home page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect an agent to the home page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
      }
    }
    "redirect an individual to the add business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddBusinessStartDateController.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
      }
    }
    "redirect an agent to the add business start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddBusinessStartDateController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe OK
      }
    }
    "display all fields missing error message" when {
      "all input date fields are empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBothIncomeSources()

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("Enter the date your business started trading")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include(s"The date your business started trading must be before $maximumAllowableDatePlusOneDay")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        status(result) shouldBe SEE_OTHER
        contentAsString(result) must not include(s"The date your business started trading must be before $maximumAllowableDatePlusOneDay")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date your business started trading must be a real date")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date must include a day")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date must include a year")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date must include a day and a month")
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

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date must include a day and a year")
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

        val result = TestAddBusinessStartDateController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include("The date must include a month and a year")
      }
    }
    "redirect to the business start date check page" when {
      "an individual enters a valid date" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.routes.AddBusinessStartDateCheckController.show()

        val testDay = "01"
        val testMonth = "01"
        val testYear = "2022"

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postAction.url)
      }
    }
    "redirect to the business start date check page" when {
      "an agent enters a valid date" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.routes.AddBusinessStartDateCheckController.showAgent()

        val testDay = "01"
        val testMonth = "01"
        val testYear = "2022"

        val result = TestAddBusinessStartDateController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          )
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postActionAgent.url)
      }
    }
  }
}
