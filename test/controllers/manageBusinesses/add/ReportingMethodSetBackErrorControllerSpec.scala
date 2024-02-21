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

package controllers.manageBusinesses.add

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.incomeSources.YouCannotGoBackError

import scala.concurrent.Future

class ReportingMethodSetBackErrorControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {


  object TestReportingMethodSetBackController extends ReportingMethodSetBackErrorController(
    mockAuthService,
    app.injector.instanceOf[YouCannotGoBackError],
    testAuthenticator
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockSessionService) {

    val title: String = messages("cannotGoBack.heading")
    val messageSE: String = messages("cannotGoBack.soleTraderAdded")
    val messageUK: String = messages("cannotGoBack.ukPropertyAdded")
    val messageFP: String = messages("cannotGoBack.foreignPropertyAdded")

    def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
      (isAgent, incomeSourceType) match {
        case (false, _) => messages("htmlTitle", s"$title")
        case (true, _) => messages("htmlTitle.agent", s"$title")
      }
    }

    def getSubHeading(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => messageSE
        case UkProperty => messageUK
        case ForeignProperty => messageFP
      }
    }
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"))))

  def mockMongo(journeyType: JourneyType): Unit = {
    setupMockGetMongo(Right(Some(sessionData(journeyType))))
    setupMockGetSessionKeyMongoTyped[String](Right(Some("1234")))
  }

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  for (incomeSourceType <- incomeSourceTypes) yield {
    for (isAgent <- Seq(true, false)) yield {
      s"ReportingMethodSetBackErrorController ($incomeSourceType, ${if (isAgent) "Agent" else "Individual"})" should {
        "redirect a user back to the custom error page" when {
          "the user is not authenticated" should {
            "redirect them to sign in" in {
              if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
              val result = if (isAgent) TestReportingMethodSetBackController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
              else TestReportingMethodSetBackController.show(incomeSourceType)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
            }
          }
        }

        "feature switch is disabled" should {
          "redirect to home page" in {
            disableAllSwitches()

            authenticate(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            val result: Future[Result] = if (isAgent) TestReportingMethodSetBackController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestReportingMethodSetBackController.show(incomeSourceType)(fakeRequestWithActiveSession)
            status(result) shouldBe SEE_OTHER
            val homeUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }

        ".show" should {
          "Display the you cannot go back error page" in {
            disableAllSwitches()
            enable(IncomeSources)

            authenticate(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            mockMongo(JourneyType(Add, incomeSourceType))

            val result = if (isAgent) TestReportingMethodSetBackController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestReportingMethodSetBackController.show(incomeSourceType)(fakeRequestWithActiveSession)
            val document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe TestReportingMethodSetBackController.getTitle(incomeSourceType, isAgent)
            document.getElementById("subheading").text() shouldBe TestReportingMethodSetBackController.getSubHeading(incomeSourceType)
          }
        }
      }
    }
  }
}
