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

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.YouCannotGoBackError
import scala.concurrent.Future

class ReportingMethodSetBackErrorControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  object TestReportingMethodSetBackController$ extends ReportingMethodSetBackErrorController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    MockIncomeSourceDetailsPredicate,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[YouCannotGoBackError]
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = messages("cannotGoBack.heading")
    val messageSE: String = messages("cannotGoBack.soleTraderAdded")
    val messageUK: String = messages("cannotGoBack.ukPropertyAdded")
    val messageFP: String = messages("cannotGoBack.foreignPropertyAdded")

    def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
      (isAgent, incomeSourceType) match {
        case (false, SelfEmployment) => messages("htmlTitle", s"$title")
        case (false, UkProperty) => messages("htmlTitle", s"$title")
        case (false, ForeignProperty) => messages("htmlTitle", s"$title")
        case (true, SelfEmployment) => messages("htmlTitle.agent", s"$title")
        case (true, UkProperty) => messages("htmlTitle.agent", s"$title")
        case (true, ForeignProperty) => messages("htmlTitle.agent", s"$title")
      }
    }
  }

  "ReportingMethodSetBackErrorController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestReportingMethodSetBackController$.show(SelfEmployment)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }

    "feature switch is disabled" should {
      "redirect to home page" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestReportingMethodSetBackController$.show(UkProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "redirect to home page (agent)" in {
        disableAllSwitches()

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestReportingMethodSetBackController$.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }

    ".show" should {
      "Display the you cannot go back error page (Individual, SelfEmployment)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.show(SelfEmployment)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(SelfEmployment, isAgent = false)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageSE
      }
      "Display the you cannot go back error page (Individual, UkProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.show(UkProperty)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(UkProperty, isAgent = false)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageUK
      }
      "Display the you cannot go back error page (Individual, ForeignProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.show(ForeignProperty)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(ForeignProperty, isAgent = false)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageFP
      }

      "Display the you cannot go back error page (Agent, SelfEmployment)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(SelfEmployment, isAgent = true)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageSE
      }
      "Display the you cannot go back error page (Agent, UkProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.showAgent(UkProperty)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(UkProperty, isAgent = true)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageUK
      }
      "Display the you cannot go back error page (Agent, ForeignProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = TestReportingMethodSetBackController$.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestReportingMethodSetBackController$.getTitle(ForeignProperty, isAgent = true)
        document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController$.messageFP
      }
    }
  }
}
