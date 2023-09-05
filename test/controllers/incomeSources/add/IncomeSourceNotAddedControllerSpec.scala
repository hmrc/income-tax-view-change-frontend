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
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.mockito.Mockito.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.CreateBusinessDetailsService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.IncomeSourceNotAddedError

import scala.concurrent.Future

class IncomeSourceNotAddedControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  object TestIncomeSourceNotAddedController extends IncomeSourceNotAddedController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    businessDetailsService = mockBusinessDetailsService,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[IncomeSourceNotAddedError])(
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

  lazy val errorUrlSE: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment.key).url
  lazy val agentErrorUrlSE: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment.key).url
  lazy val errorUrlUK: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = UkProperty.key).url
  lazy val agentErrorUrlUK: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = UkProperty.key).url
  lazy val errorUrlFP: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = ForeignProperty.key).url
  lazy val agentErrorUrlFP: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = ForeignProperty.key).url
  lazy val addIncomeSource: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  lazy val addIncomeSourceAgent: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url


  "IncomeSourceNotAddedController.show - Individual" should {
    "return 200 and render Income Source Not Added Error Page" when {
      "user is trying to add SE business" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestIncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment.key)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textSelfEmployment
      }
      "user is trying to add UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestIncomeSourceNotAddedController.show(incomeSourceType = UkProperty.key)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textUkProperty
      }
      "user is trying to add Foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestIncomeSourceNotAddedController.show(incomeSourceType = ForeignProperty.key)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textForeignProperty
      }
    }

    "show Internal Server Error when income source type is invalid" in {
      disableAllSwitches()
      enable(IncomeSources)
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
      val result: Future[Result] = TestIncomeSourceNotAddedController.show("")(fakeRequestWithActiveSession)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceNotAddedController.show(ForeignProperty.key)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }

    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAuthorisationException()
        val result = TestIncomeSourceNotAddedController.show(SelfEmployment.key)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "redirect to the session timeout page" when {
      "the user has timed out" in {
        setupMockAuthorisationException()
        val result = TestIncomeSourceNotAddedController.show(SelfEmployment.key)(fakeRequestWithTimeoutSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }
  }

  "IncomeSourceNotAddedController.showAgent - Agent" should {
    "return 200 OK" when {
      "business type is self employment" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceNotAddedController.showAgent(SelfEmployment.key)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textSelfEmployment
        document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe addIncomeSourceAgent
      }

      "business type is UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceNotAddedController.showAgent(UkProperty.key)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textUkProperty
        document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe addIncomeSourceAgent
      }

      "business type is foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceNotAddedController.showAgent(ForeignProperty.key)(fakeRequestConfirmedClient())
        val document = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        document.title shouldBe TestIncomeSourceNotAddedController.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestIncomeSourceNotAddedController.textForeignProperty
        document.getElementById("error-income-source-not-saved-form").attr("action") shouldBe addIncomeSourceAgent
      }
    }
    "business type is invalid" in {
      disableAllSwitches()
      enable(IncomeSources)
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
      val result: Future[Result] = TestIncomeSourceNotAddedController.show("")(fakeRequestConfirmedClient())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceNotAddedController.showAgent(ForeignProperty.key)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAgentAuthorisationException()
        val result = TestIncomeSourceNotAddedController.showAgent(ForeignProperty.key)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
  }
}