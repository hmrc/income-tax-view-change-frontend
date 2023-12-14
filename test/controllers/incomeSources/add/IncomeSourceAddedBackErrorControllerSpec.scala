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
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSessionId}
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
      if (isAgent) messages("htmlTitle.agent", s"$title") else messages("htmlTitle", s"$title")
    }
  }

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"))))

  def mockMongo(journeyType: JourneyType): Unit = {
    setupMockGetMongo(Right(Some(sessionData(journeyType))))
    setupMockGetSessionKeyMongoTyped[String](Right(Some("1234")))
  }

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  def postRequest(isAgent: Boolean) = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  "ReportingMethodSetBackErrorController" should {

    "feature switch is disabled" should {
      def fsRedirectTest(isAgent: Boolean) = {
        s"redirect to home page (${if (isAgent) "agent" else "individual"})" in {
          disableAllSwitches()

          authenticate(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = if (isAgent) TestIncomeSourceAddedBackErrorController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
          else TestIncomeSourceAddedBackErrorController.show(UkProperty)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some({
            if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
          })
        }
      }

      fsRedirectTest(false)
      fsRedirectTest(true)
    }

    for (incomeSourceType <- incomeSourceTypes) yield {
      ".show" should {
        showSuccess(false, incomeSourceType)
        showSuccess(true, incomeSourceType)

        def showSuccess(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
          s"Display the you cannot go back error page (${if (isAgent) "agent" else "individual"}, $incomeSourceType)" in {
            disableAllSwitches()
            enable(IncomeSources)

            authenticate(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            mockMongo(JourneyType(Add, SelfEmployment))

            val result = if (isAgent) TestIncomeSourceAddedBackErrorController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceAddedBackErrorController.show(incomeSourceType)(fakeRequestWithActiveSession)
            val document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe TestIncomeSourceAddedBackErrorController.getTitle(isAgent)
            document.getElementById("warning-message").text() shouldBe TestIncomeSourceAddedBackErrorController.warningMessage
          }
        }
      }
    }

    for (incomeSourceType <- incomeSourceTypes) yield {
      for (isAgent <- Seq(true, false)) yield {
        ".submit" should {
          s"return ${Status.SEE_OTHER} and redirect to $incomeSourceType reporting method page (isAgent = $isAgent)" in {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            authenticate(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            mockMongo(JourneyType(Add, incomeSourceType))

            val result = if (isAgent) TestIncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)(postRequest(isAgent))
            else TestIncomeSourceAddedBackErrorController.submit(incomeSourceType)(postRequest(isAgent))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url)
          }
        }
      }
    }
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
  }
}
