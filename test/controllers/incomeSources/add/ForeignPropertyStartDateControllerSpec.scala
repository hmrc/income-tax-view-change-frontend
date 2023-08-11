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
import forms.incomeSources.add.AddForeignPropertyStartDateForm
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyStartDate

import scala.concurrent.Future

class ForeignPropertyStartDateControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val dayField = "foreign-property-start-date.day"
  val monthField = "foreign-property-start-date.month"
  val yearField = "foreign-property-start-date.year"

  val currentDate = dateService.getCurrentDate()

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  object TestForeignPropertyStartDateController
    extends ForeignPropertyStartDateController(
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[AddForeignPropertyStartDateForm],
      mockIncomeSourceDetailsService,
      MockNavBarPredicate,
      MockIncomeSourceDetailsPredicate,
      app.injector.instanceOf[NinoPredicate],
      app.injector.instanceOf[ForeignPropertyStartDate],
      app.injector.instanceOf[CustomNotFoundError]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler]
    ) {
    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.foreignProperty.startDate.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.foreignProperty.startDate.heading"))}"
    val heading: String = messages("incomeSources.add.foreignProperty.startDate.heading")
  }

  "ForeignPropertyStartDateController for individual" should {
    "show customNotFoundErrorView page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestForeignPropertyStartDateController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestForeignPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "redirect to the add foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestForeignPropertyStartDateController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyStartDateController.title
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestForeignPropertyStartDateController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "all input date fields are empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBothIncomeSources()

        val result = TestForeignPropertyStartDateController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("incomeSources.add.foreignProperty.startDate.error.empty"))
      }
    }
    "redirect to the foreign property start date check page" when {
      "an individual enters a valid date" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.show()

        val testDay = "01"
        val testMonth = "01"
        val testYear = "2022"

        val result = TestForeignPropertyStartDateController.submit()(
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
  }
  "ForeignPropertyStartDateController for Agent" should {
    "show customNotFoundErrorView page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestForeignPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestForeignPropertyStartDateController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

      }
    }
    "redirect to the add foreign property start date page" when {
      "incomeSources FS is enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestForeignPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyStartDateController.titleAgent
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestForeignPropertyStartDateController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "all input date fields are empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestForeignPropertyStartDateController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody())
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("incomeSources.add.foreignProperty.startDate.error.empty"))
      }
    }
    "redirect to the foreign property start date check page" when {
      "an agent enters a valid date" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent()

        val testDay = "01"
        val testMonth = "01"
        val testYear = "2022"

        val result = TestForeignPropertyStartDateController.submitAgent()(
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

