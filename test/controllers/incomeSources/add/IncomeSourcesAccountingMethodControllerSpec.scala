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
import forms.utils.SessionKeys.addIncomeSourcesAccountingMethod
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import scala.concurrent.Future

class IncomeSourcesAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockIncomeSourcesAccountingMethod: IncomeSourcesAccountingMethod = app.injector.instanceOf[IncomeSourcesAccountingMethod]

  val selfEmploymentAccountingMethod: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val UKPropertyAccountingMethod: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val foreignPropertyAccountingMethod: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  def getAccountingMethod(incomeSourceType: String): String = {
    "incomeSources.add." + incomeSourceType + ".AccountingMethod"
  }

  def getHeading(incomeSourceType: String): String = {
    messages("incomeSources.add." + incomeSourceType + ".AccountingMethod.heading")
  }

  def getTitle(incomeSourceType: String, isAgent: Boolean = false): String = {
    if (isAgent)
      s"${messages("htmlTitle.agent", getHeading(incomeSourceType))}"
    else
      s"${messages("htmlTitle", getHeading(incomeSourceType))}"
  }

  def showResult(incomeSourceType: String, isAgent: Boolean = false): Future[Result] = {
    if (isAgent)
      TestIncomeSourcesAccountingMethodController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
    else
      TestIncomeSourcesAccountingMethodController.show(incomeSourceType)(fakeRequestWithActiveSession)
  }

  def changeResult(incomeSourceType: String, isAgent: Boolean = false, cashOrAccrualsFlag: Option[String] = None): Future[Result] = {
    if (isAgent)
      TestIncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethodAgent(incomeSourceType)(fakeRequestConfirmedClient().withSession(addIncomeSourcesAccountingMethod -> cashOrAccrualsFlag.getOrElse("")))
    else
      TestIncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod(incomeSourceType)(fakeRequestWithActiveSession.withSession(addIncomeSourcesAccountingMethod -> cashOrAccrualsFlag.getOrElse("")))
  }

  def getSetupMockAuth(isAgent: Boolean = false): Unit = {
    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  def submitResult(incomeSourceType: String, accountingMethod: String, isAgent: Boolean = false): Future[Result] = {
    if (isAgent)
      TestIncomeSourcesAccountingMethodController.submitAgent(incomeSourceType)(fakeRequestConfirmedClient()
        .withFormUrlEncodedBody(selfEmploymentAccountingMethod -> accountingMethod))
    else
      TestIncomeSourcesAccountingMethodController.submit(incomeSourceType)(fakeRequestNoSession
        .withFormUrlEncodedBody(selfEmploymentAccountingMethod -> accountingMethod))
  }

  def getRedirectUrl(isAgent: Boolean = false): String = {
    if (isAgent)
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
    else
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  }


  object TestIncomeSourcesAccountingMethodController extends IncomeSourcesAccountingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[IncomeSourcesAccountingMethod],
    app.injector.instanceOf[CustomNotFoundError],
    sessionService = app.injector.instanceOf[SessionService])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  def showIncomeSourcesAccountingMethodTest(incomeSourceType: String, isAgent: Boolean = false): Unit = {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active " + incomeSourceType + " businesses" in {
        getSetupMockAuth(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        document.select("legend").text shouldBe getHeading(incomeSourceType)
      }
    }
    "return 303 SEE_OTHER" when {
      "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string accruals" in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithAccruals()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
      }
      "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string cash" in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
      }
      "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list) for " + incomeSourceType in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "navigating to the page with FS Enabled and a user with a " + incomeSourceType + " business missing its cashOrAccruals field" in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceMissingCashOrAccrualsField()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page for " + incomeSourceType when {
      "navigating to the page with FS Disabled" in {
        getSetupMockAuth(isAgent)
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        val expectedContent: String = TestIncomeSourcesAccountingMethodController.customNotFoundErrorView().toString()

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user for " + incomeSourceType in {
        if (isAgent)
          setupMockAgentAuthorisationException()
        else
          setupMockAuthorisationException()
        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  def submitIncomeSourcesAccountingMethodTest(incomeSourceType: String, isAgent: Boolean = false): Unit = {
    s"return 303 SEE_OTHER and redirect to ${getRedirectUrl(isAgent)}" when {
      "form is completed successfully with cash radio button selected for " + incomeSourceType in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = submitResult(incomeSourceType, "cash", isAgent)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe Some("cash")
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
      }
      "form is completed successfully with traditional radio button selected for " + incomeSourceType in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = submitResult(incomeSourceType, "traditional", isAgent)

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe Some("accruals")
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully for " + incomeSourceType in {
        getSetupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        lazy val result: Future[Result] = submitResult(incomeSourceType, "", isAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.BAD_REQUEST
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        result.futureValue.session.get(addIncomeSourcesAccountingMethod) shouldBe None
      }
    }
  }

  def changeIncomeSourcesAccountingMethodTest(incomeSourceType: String, isAgent: Boolean = false, cashOrAccrualsFlag: Option[String] = None): Unit = {
    "return 200 OK for change accounting method for isAgent = " + isAgent + "" when {
      "navigating to the page by change link with FS Enabled and no active " + incomeSourceType + " businesses" in {
        getSetupMockAuth(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        val result: Future[Result] = changeResult(incomeSourceType, isAgent, cashOrAccrualsFlag)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        document.select("input").select("[checked]").`val`() shouldBe (if (cashOrAccrualsFlag.getOrElse("") == "cash") "cash" else "traditional")
        document.select("legend").text shouldBe getHeading(incomeSourceType)
      }
    }
  }

  "Individual - IncomeSourcesAccountingMethodController.show()" should {
    showIncomeSourcesAccountingMethodTest(SelfEmployment.key)
  }
  "Individual - IncomeSourcesAccountingMethodController.submit()" should {
    submitIncomeSourcesAccountingMethodTest(SelfEmployment.key)
  }
  "Individual - IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod()" should {
    changeIncomeSourcesAccountingMethodTest(SelfEmployment.key, false, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(UkProperty.key, false, Some("accruals"))
    changeIncomeSourcesAccountingMethodTest(ForeignProperty.key, false, Some("cash"))
  }

  "Agent - IncomeSourcesAccountingMethodController.showAgent()" should {
    showIncomeSourcesAccountingMethodTest(SelfEmployment.key, true)
  }
  "Agent - IncomeSourcesAccountingMethodController.submit()" should {
    submitIncomeSourcesAccountingMethodTest(SelfEmployment.key, true)
  }
  "Agent - IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod()" should {
    changeIncomeSourcesAccountingMethodTest(SelfEmployment.key, true, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(UkProperty.key, true, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(ForeignProperty.key, true, Some("accruals"))
  }

}
