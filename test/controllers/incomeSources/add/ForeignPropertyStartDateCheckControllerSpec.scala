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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.add.{ForeignPropertyStartDateCheckForm, ForeignPropertyStartDateForm}
import forms.utils.SessionKeys.foreignPropertyStartDate
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
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.{ForeignPropertyStartDate, ForeignPropertyStartDateCheck}

import scala.concurrent.Future

class ForeignPropertyStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {


  val currentDate = dateService.getCurrentDate()

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestForeignPropertyStartDateCheckController
    extends ForeignPropertyStartDateCheckController(
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockIncomeSourceDetailsPredicate,
      MockNavBarPredicate,
      app.injector.instanceOf[NinoPredicate],
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[ForeignPropertyStartDateCheck],
      app.injector.instanceOf[CustomNotFoundError],
      app.injector.instanceOf[LanguageUtils]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler]
    ) {
    val messageKey = "incomeSources.add.foreignProperty.startDate.check.heading"
    val title: String = s"${messages("htmlTitle", messages(messageKey))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages(messageKey))}"
    val heading: String = messages(messageKey)
  }

  "ForeignPropertyStartDateCheckController for individual" should {
    "show customNotFoundErrorView page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestForeignPropertyStartDateCheckController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestForeignPropertyStartDateCheckController.customNotFoundErrorView().toString()
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

        val result = TestForeignPropertyStartDateCheckController.show()(fakeRequestWithActiveSession.withSession(foreignPropertyStartDate -> "2022-01-01"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyStartDateCheckController.title
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestForeignPropertyStartDateCheckController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "input is empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        mockBothIncomeSources()

        val result = TestForeignPropertyStartDateCheckController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody().withSession(foreignPropertyStartDate -> "2022-01-01"))

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("incomeSources.add.foreignProperty.startDate.check.error"))
      }
    }
    "redirect to the different pages as to response" when {
      "an individual gives a valid input - Yes" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show()

        val result = TestForeignPropertyStartDateCheckController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            "foreign-property-start-date-check" -> "Yes"
          ).withSession(foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postAction.url)
      }

      "an individual gives a valid input - No" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val postAction: Call = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.show()

        val result = TestForeignPropertyStartDateCheckController.submit()(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            "foreign-property-start-date-check" -> "No"
          ).withSession(foreignPropertyStartDate -> "2022-01-01")
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

        val result = TestForeignPropertyStartDateCheckController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestForeignPropertyStartDateCheckController.customNotFoundErrorView().toString()
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

        val result = TestForeignPropertyStartDateCheckController.showAgent()(fakeRequestConfirmedClient().withSession(foreignPropertyStartDate -> "2022-01-01"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyStartDateCheckController.titleAgent
      }
    }
    "should redirect" when {
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestForeignPropertyStartDateCheckController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "display error message" when {
      "input is empty" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestForeignPropertyStartDateCheckController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody().withSession(foreignPropertyStartDate -> "2022-01-01"))
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("incomeSources.add.foreignProperty.startDate.check.error"))
      }
    }
    "redirect to the different pages as to response" when {
      "an Agent gives a valid input - Yes" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent()

        val result = TestForeignPropertyStartDateCheckController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            "foreign-property-start-date-check" -> "Yes"
          ).withSession(foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postActionAgent.url)
      }

      "an Agent gives a valid input - No" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val postActionAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.showAgent()

        val result = TestForeignPropertyStartDateCheckController.submitAgent()(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            "foreign-property-start-date-check" -> "No"
          ).withSession(foreignPropertyStartDate -> "2022-01-01")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(postActionAgent.url)
      }
    }
  }
}

