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

import auth.MtdItUser
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.BusinessNameForm
import forms.incomeSources.add.BusinessStartDateForm
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.{mock, reset}
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{DateService, IncomeSourceDetailsService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.FinancialDetailsIntegrationTestConstants.currentDate
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.AddBusiness
import views.html.incomeSources.add.AddBusinessStartDate

import java.time.LocalDate
import scala.concurrent.Future


class AddBusinessStartDateControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val postAction: Call = controllers.routes.AddBusinessStartDateController.submit()
  val postActionAgent: Call = controllers.routes.AddBusinessStartDateController.submitAgent()

  val dayField = "add-business-start-date.day"
  val monthField = "add-business-start-date.month"
  val yearField = "add-business-start-date.year"
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

        val result = TestAddBusinessStartDateController.showAgent()(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe SEE_OTHER
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

        val currentDate = dateService.getCurrentDate()

        val testDate = DateFormElement(
          currentDate.plusDays(10)
        ).date

        val result = TestAddBusinessStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            dayField -> testDate.getDayOfMonth.toString,
            monthField -> testDate.getMonthValue.toString,
            yearField -> testDate.getYear.toString
          )
        )

        val maxDate = mockImplicitDateFormatter
          .longDate(currentDate.plusWeeks(1))
          .toLongDate

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) must include(s"The date your business started trading must be before $maxDate")
      }
    }
  }
}
