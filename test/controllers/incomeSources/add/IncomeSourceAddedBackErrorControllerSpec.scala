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
import enums.JourneyType.{Add, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceAddedBackError

import scala.concurrent.Future

class IncomeSourceAddedBackErrorControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  object TestIncomeSourceAddedBackErrorController extends IncomeSourceAddedBackErrorController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    MockIncomeSourceDetailsPredicate,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[IncomeSourceAddedBackError],
    mockSessionService
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = messages("cannotGoBack.heading")
    val warningMessage: String = s"! Warning ${messages("cannotGoBack.warningMessage")}"

    def getTitle(isAgent: Boolean): String = {
      if (isAgent )messages("htmlTitle.agent", s"$title") else messages("htmlTitle", s"$title")
    }
  }
  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"))))

  def mockMongo(journeyType: JourneyType): Unit = {
    setupMockGetMongo(Right(Some(sessionData(journeyType))))
  }

  "ReportingMethodSetBackErrorController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestIncomeSourceAddedBackErrorController.show(SelfEmployment)(fakeRequestWithActiveSession)
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

        val result: Future[Result] = TestIncomeSourceAddedBackErrorController.show(UkProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "redirect to home page (agent)" in {
        disableAllSwitches()

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedBackErrorController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
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
        mockMongo(JourneyType(Add, SelfEmployment))

        val result = TestIncomeSourceAddedBackErrorController.show(SelfEmployment)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = false)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }
      "Display the you cannot go back error page (Individual, UkProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMongo(JourneyType(Add, UkProperty))

        val result = TestIncomeSourceAddedBackErrorController.show(UkProperty)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = false)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }
      "Display the you cannot go back error page (Individual, ForeignProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMongo(JourneyType(Add, ForeignProperty))

        val result = TestIncomeSourceAddedBackErrorController.show(ForeignProperty)(fakeRequestWithActiveSession)
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = false)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }

      "Display the you cannot go back error page (Agent, SelfEmployment)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMongo(JourneyType(Add, SelfEmployment))

        val result = TestIncomeSourceAddedBackErrorController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = true)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }
      "Display the you cannot go back error page (Agent, UkProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMongo(JourneyType(Add, UkProperty))

        val result = TestIncomeSourceAddedBackErrorController.showAgent(UkProperty)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = true)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }
      "Display the you cannot go back error page (Agent, ForeignProperty)" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMongo(JourneyType(Add, ForeignProperty))

        val result = TestIncomeSourceAddedBackErrorController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent = true)
        document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
      }
    }
  }
}
