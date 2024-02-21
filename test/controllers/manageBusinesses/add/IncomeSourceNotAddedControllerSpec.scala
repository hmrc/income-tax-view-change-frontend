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
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.mockito.Mockito.mock
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.CreateBusinessDetailsService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.IncomeSourceNotAddedError

import scala.concurrent.Future

class IncomeSourceNotAddedControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  object TestIncomeSourceNotAddedController extends IncomeSourceNotAddedController(
    mockAuthService,
    businessDetailsService = mockBusinessDetailsService,
    app.injector.instanceOf[IncomeSourceNotAddedError],
    testAuthenticator)(
    appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {
    val title: String = messages("incomeSources.add.error.standardError")
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.error.standardError"))}"
    val titleIndividual: String = s"${messages("htmlTitle", title)}"
    val textSelfEmployment: String = messages("incomeSources.add.error.incomeSourceNotSaved.p1", "sole trader")
    val textUkProperty: String = messages("incomeSources.add.error.incomeSourceNotSaved.p1", "UK property")
    val textForeignProperty: String = messages("incomeSources.add.error.incomeSourceNotSaved.p1", "foreign property")
  }

  lazy val errorUrlSE: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment).url
  lazy val agentErrorUrlSE: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment).url
  lazy val errorUrlUK: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = UkProperty).url
  lazy val agentErrorUrlUK: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = UkProperty).url
  lazy val errorUrlFP: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = ForeignProperty).url
  lazy val agentErrorUrlFP: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = ForeignProperty).url
  lazy val addIncomeSource: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url
  lazy val addIncomeSourceAgent: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent().url

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def mockIncomeSource(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => mockBusinessIncomeSource()
      case UkProperty => mockUKPropertyIncomeSource()
      case ForeignProperty => mockForeignPropertyIncomeSource()
    }
  }

  def paragraphText(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => TestIncomeSourceNotAddedController.textSelfEmployment
      case UkProperty => TestIncomeSourceNotAddedController.textUkProperty
      case ForeignProperty => TestIncomeSourceNotAddedController.textForeignProperty
    }
  }

  for (incomeSourceType <- incomeSourceTypes) yield {
    for (isAgent <- Seq(true, false)) yield {
      s"IncomeSourceNotAddedController.show (${incomeSourceType.key}, ${if (isAgent) "Agent" else "Individual"})" should {
        "return 200 and render Income Source Not Added Error Page" when {
          "user is trying to add SE business" in {
            disableAllSwitches()
            enable(IncomeSources)
            mockIncomeSource(incomeSourceType)
            setupMockAuthorisationSuccess(isAgent)

            val result: Future[Result] = if (isAgent) TestIncomeSourceNotAddedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceNotAddedController.show(incomeSourceType)(fakeRequestWithActiveSession)

            val document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe {if (isAgent) TestIncomeSourceNotAddedController.titleAgent else TestIncomeSourceNotAddedController.titleIndividual}
            document.getElementById("paragraph-1").text() shouldBe paragraphText(incomeSourceType)
          }
        }

        "return 303 and show home page" when {
          "when feature switch is disabled" in {
            disableAllSwitches()

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            val result: Future[Result] = if (isAgent) TestIncomeSourceNotAddedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceNotAddedController.show(incomeSourceType)(fakeRequestWithActiveSession)
            status(result) shouldBe SEE_OTHER
            val redirectUrl =if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        "return 303 and redirect to the sign in" when {
          "the user is not authenticated" in {
            if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
            val result = if (isAgent) TestIncomeSourceNotAddedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceNotAddedController.show(incomeSourceType)(fakeRequestWithActiveSession)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
          }
        }
        "redirect to the session timeout page" when {
          "the user has timed out" in {
            if (isAgent) setupMockAgentAuthorisationException(exception = BearerTokenExpired()) else setupMockAuthorisationException()
            val result =  if (isAgent) TestIncomeSourceNotAddedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceNotAddedController.show(SelfEmployment)(fakeRequestWithTimeoutSession)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }
      }
    }
  }
}