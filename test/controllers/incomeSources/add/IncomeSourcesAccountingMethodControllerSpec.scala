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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.utils.SessionKeys.addIncomeSourcesAccountingMethod
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.incomeSourceDetails.AddIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, contentType, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import scala.concurrent.Future

class IncomeSourcesAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockIncomeSourcesAccountingMethod: IncomeSourcesAccountingMethod = app.injector.instanceOf[IncomeSourcesAccountingMethod]

  val selfEmploymentAccountingMethod: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val UKPropertyAccountingMethod: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val foreignPropertyAccountingMethod: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  def verifySetMongoKey(key: String, value: String, journeyType: JourneyType): Unit = {
    val argumentKey: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentValue: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentJourneyType: ArgumentCaptor[JourneyType] = ArgumentCaptor.forClass(classOf[JourneyType])
    verify(mockSessionService).setMongoKey(argumentKey.capture(), argumentValue.capture(), argumentJourneyType.capture())(any(), any())
    argumentKey.getValue shouldBe key
    argumentValue.getValue shouldBe value
    argumentJourneyType.getValue.toString shouldBe journeyType.toString
  }

  def getAccountingMethod(incomeSourceType: String): String = {
    "incomeSources.add." + incomeSourceType + ".AccountingMethod"
  }

  def getHeading(incomeSourceType: IncomeSourceType): String = {
    messages("incomeSources.add." + incomeSourceType.key + ".AccountingMethod.heading")
  }

  def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): String = {
    if (isAgent)
      s"${messages("htmlTitle.agent", getHeading(incomeSourceType))}"
    else
      s"${messages("htmlTitle", getHeading(incomeSourceType))}"
  }

  def showResult(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): Future[Result] = {
    if (isAgent)
      TestIncomeSourcesAccountingMethodController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
    else
      TestIncomeSourcesAccountingMethodController.show(incomeSourceType)(fakeRequestWithActiveSession)
  }

  def changeResult(incomeSourceType: IncomeSourceType, isAgent: Boolean = false, cashOrAccrualsFlag: Option[String] = None): Future[Result] = {
    if (isAgent)
      TestIncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethodAgent(incomeSourceType)(fakeRequestConfirmedClient())
    else
      TestIncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod(incomeSourceType)(fakeRequestWithActiveSession)
  }

  def setupMockAuth(isAgent: Boolean = false): Unit = {
    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  def submitResult(incomeSourceType: IncomeSourceType, accountingMethod: String, isAgent: Boolean = false): Future[Result] = {
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
    sessionService = mockSessionService)(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  def showIncomeSourcesAccountingMethodTest(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): Unit = {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no active " + incomeSourceType + " businesses" in {
        setupMockAuth(isAgent)
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
        val accountingMethod: String = "accruals"
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithAccruals()
        reset(mockSessionService)
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
        verifySetMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField, accountingMethod, JourneyType(Add, incomeSourceType))
      }
      "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string cash" in {
        val accountingMethod: String = "cash"
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
        verifySetMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField, accountingMethod, JourneyType(Add, incomeSourceType))
      }
      "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list) for " + incomeSourceType in {
        val accountingMethod: String = "cash"
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceWithCashAndAccruals()
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
        verifySetMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField, accountingMethod, JourneyType(Add, incomeSourceType))
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "navigating to the page with FS Enabled and a user with a " + incomeSourceType + " business missing its cashOrAccruals field" in {
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockBusinessIncomeSourceMissingCashOrAccrualsField()

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentType(result) shouldBe Some("text/html")
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page for " + incomeSourceType when {
      "navigating to the page with FS Disabled" in {
        setupMockAuth(isAgent)
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

  def submitIncomeSourcesAccountingMethodTest(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): Unit = {
    s"return 303 SEE_OTHER and redirect to ${getRedirectUrl(isAgent)}" when {
      "form is completed successfully with cash radio button selected for " + incomeSourceType in {
        val accountingMethod: String = "cash"
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = submitResult(incomeSourceType, accountingMethod, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
        verifySetMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField, accountingMethod, JourneyType(Add, incomeSourceType))
      }
      "form is completed successfully with traditional radio button selected for " + incomeSourceType in {
        val accountingMethod: String = "accruals"
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = submitResult(incomeSourceType, "traditional", isAgent)


        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(isAgent))
        verifySetMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField, accountingMethod, JourneyType(Add, incomeSourceType))
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully for " + incomeSourceType in {
        setupMockAuth(isAgent)
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

  def changeIncomeSourcesAccountingMethodTest(incomeSourceType: IncomeSourceType, isAgent: Boolean = false, cashOrAccrualsFlag: Option[String] = None): Unit = {
    "return 200 OK for change accounting method for isAgent = " + isAgent + "" when {
      "navigating to the page by change link with FS Enabled and no active " + incomeSourceType + " businesses" in {

        setupMockAuth(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()

        setupMockGetSessionKeyMongoTyped[String](Right(cashOrAccrualsFlag))

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
    showIncomeSourcesAccountingMethodTest(SelfEmployment)
  }
  "Individual - IncomeSourcesAccountingMethodController.submit()" should {
    submitIncomeSourcesAccountingMethodTest(SelfEmployment)
  }
  "Individual - IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod()" should {
    changeIncomeSourcesAccountingMethodTest(SelfEmployment, isAgent = false, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(UkProperty, isAgent = false, Some("accruals"))
    changeIncomeSourcesAccountingMethodTest(ForeignProperty, isAgent = false, Some("cash"))
  }

  "Agent - IncomeSourcesAccountingMethodController.showAgent()" should {
    showIncomeSourcesAccountingMethodTest(SelfEmployment, isAgent = true)
  }
  "Agent - IncomeSourcesAccountingMethodController.submit()" should {
    submitIncomeSourcesAccountingMethodTest(SelfEmployment, isAgent = true)
  }
  "Agent - IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod()" should {
    changeIncomeSourcesAccountingMethodTest(SelfEmployment, isAgent = true, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(UkProperty, isAgent = true, Some("cash"))
    changeIncomeSourcesAccountingMethodTest(ForeignProperty, isAgent = true, Some("accruals"))
  }

}
