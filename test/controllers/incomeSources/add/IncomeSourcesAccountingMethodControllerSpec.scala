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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import enums.{AccrualsAsAccountingMethod, CashAsAccountingMethod}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import scala.concurrent.Future

class IncomeSourcesAccountingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockIncomeSourcesAccountingMethod: IncomeSourcesAccountingMethod = app.injector.instanceOf[IncomeSourcesAccountingMethod]

  def businessResponseRoute(incomeSourceType: IncomeSourceType): String = {
    "incomeSources.add." + incomeSourceType.key + ".AccountingMethod"
  }

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData()))

  def sessionDataWithAccMethodCash(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = Some("cash"))))

  def sessionDataWithAccMethodAccruals(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = Some("accruals"))))

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

  def getRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  def showResult(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): Future[Result] = {
    TestIncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)(getRequest(isAgent))
  }

  def changeResult(incomeSourceType: IncomeSourceType, isAgent: Boolean = false, cashOrAccrualsFlag: Option[String] = None): Future[Result] = {
    TestIncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod(incomeSourceType, isAgent)(getRequest(isAgent))
  }

  def setupMockAuth(isAgent: Boolean = false): Unit = {
    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  def submitResult(incomeSourceType: IncomeSourceType, accountingMethod: String, isAgent: Boolean = false): Future[Result] = {
    TestIncomeSourcesAccountingMethodController.submit(incomeSourceType, isAgent)(postRequest(isAgent)
      .withFormUrlEncodedBody(businessResponseRoute(incomeSourceType) -> accountingMethod))
  }

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): String = {
    if (isAgent)
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
    else
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
  }

  object TestIncomeSourcesAccountingMethodController extends IncomeSourcesAccountingMethodController(
    mockAuthService,
    app.injector.instanceOf[IncomeSourcesAccountingMethod],
    app.injector.instanceOf[CustomNotFoundError],
    sessionService = mockSessionService,
    testAuthenticator)(appConfig,
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
        val journeyType = JourneyType(Add, incomeSourceType)
        setupMockGetMongo(Right(Some(sessionData(journeyType))))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        document.select("legend").text shouldBe getHeading(incomeSourceType)
      }
    }
    if (incomeSourceType == SelfEmployment) {
      "return 303 SEE_OTHER" when {
        "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string accruals" in {
          setupMockAuth(isAgent)
          enable(IncomeSources)
          mockBusinessIncomeSourceWithAccruals()
          reset(mockSessionService)
          setupMockSetMongoData(true)
          val journeyType = JourneyType(Add, incomeSourceType)
          setupMockGetMongo(Right(Some(sessionDataWithAccMethodAccruals(journeyType))))

          val result: Future[Result] = showResult(incomeSourceType, isAgent)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
        }
        "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string cash" in {
          setupMockAuth(isAgent)
          enable(IncomeSources)
          mockBusinessIncomeSource()
          setupMockSetMongoData(true)
          val journeyType = JourneyType(Add, incomeSourceType)
          setupMockGetMongo(Right(Some(sessionDataWithAccMethodCash(journeyType))))

          val result: Future[Result] = showResult(incomeSourceType, isAgent)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
        }
        "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list) for " + incomeSourceType in {
          setupMockAuth(isAgent)
          enable(IncomeSources)
          mockBusinessIncomeSourceWithCashAndAccruals()
          setupMockSetMongoData(true)
          val journeyType = JourneyType(Add, incomeSourceType)
          setupMockGetMongo(Right(Some(sessionData(journeyType))))

          val result: Future[Result] = showResult(incomeSourceType, isAgent)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
        }
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page for " + incomeSourceType when {
      "navigating to the page with FS Disabled" in {
        setupMockAuth(isAgent)
        disable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockSetMongoData(true)
        val journeyType = JourneyType(Add, incomeSourceType)
        setupMockGetMongo(Right(Some(sessionData(journeyType))))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)

        status(result) shouldBe Status.SEE_OTHER
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
    s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" + incomeSourceType when {
      s"user has already completed the journey" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockBusinessIncomeSource()
        setupMockAuth(isAgent)
        setupMockSetMongoData(true)
        setupMockGetMongo(Right(Some(sessionDataCompletedJourney(JourneyType(Add, incomeSourceType)))))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        status(result) shouldBe SEE_OTHER
        val expectedUrl = if (isAgent) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType)
        else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType)
        redirectLocation(result) shouldBe Some(expectedUrl.url)
      }
      s"user has already added their income source" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockBusinessIncomeSource()
        setupMockAuth(isAgent)
        setupMockSetMongoData(true)
        setupMockGetMongo(Right(Some(sessionDataISAdded(JourneyType(Add, incomeSourceType)))))

        val result: Future[Result] = showResult(incomeSourceType, isAgent)
        status(result) shouldBe SEE_OTHER
        val expectedUrl = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType)
        else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType)
        redirectLocation(result) shouldBe Some(expectedUrl.url)
      }
    }
  }

  def submitIncomeSourcesAccountingMethodTest(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): Unit = {
    s"return 303 SEE_OTHER and redirect to ${getRedirectUrl(incomeSourceType, isAgent)}" when {
      "form is completed successfully with cash radio button selected for " + incomeSourceType in {
        val accountingMethod: String = CashAsAccountingMethod
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        setupMockSetMongoData(true)
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))

        lazy val result: Future[Result] = submitResult(incomeSourceType, accountingMethod, isAgent)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
      }
      "form is completed successfully with traditional radio button selected for " + incomeSourceType in {
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))
        setupMockSetMongoData(true)

        lazy val result: Future[Result] = submitResult(incomeSourceType, "traditional", isAgent)


        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully for " + incomeSourceType in {
        setupMockAuth(isAgent)
        enable(IncomeSources)
        mockNoIncomeSources()

        when(mockHttpClient.POSTForm[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "valid")))

        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))
        setupMockSetMongoData(true)

        lazy val result: Future[Result] = submitResult(incomeSourceType, "", isAgent)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.BAD_REQUEST
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        result.futureValue.session.get(AddIncomeSourceData.incomeSourcesAccountingMethodField) shouldBe None
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

        val journeyType = JourneyType(Add, incomeSourceType)
        setupMockGetMongo(Right(Some(UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = cashOrAccrualsFlag))))))
        setupMockSetMongoData(true)

        val result: Future[Result] = changeResult(incomeSourceType, isAgent, cashOrAccrualsFlag)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe getTitle(incomeSourceType, isAgent)
        document.select("input").select("[checked]").`val`() shouldBe (if (cashOrAccrualsFlag.getOrElse("") == "cash") "cash" else "traditional")
        document.select("legend").text shouldBe getHeading(incomeSourceType)
      }
    }
  }

  def cashOrAccrualsAsAccountingMethod(incomeSourceType: IncomeSourceType): Option[String] = {
    incomeSourceType match {
      case UkProperty => Some(AccrualsAsAccountingMethod)
      case _ => Some(CashAsAccountingMethod)
    }
  }

  def getTestTitleIncomeSourceType(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => "Self Employment"
      case UkProperty => "UK Property"
      case ForeignProperty => "Foreign Property"
    }
  }

  def isAgentTestName(isAgent: Boolean): String = {
    if (isAgent) "Agent -" else "Individual -"
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  for (isAgent <- Seq(true, false)) yield {
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"${isAgentTestName(isAgent)} ${getTestTitleIncomeSourceType(incomeSourceType)} - IncomeSourcesAccountingMethodController.show()" should {
        showIncomeSourcesAccountingMethodTest(incomeSourceType, isAgent)
      }
      s"${isAgentTestName(isAgent)} ${getTestTitleIncomeSourceType(incomeSourceType)} - IncomeSourcesAccountingMethodController.submit()" should {
        submitIncomeSourcesAccountingMethodTest(incomeSourceType, isAgent)
      }
      s"${isAgentTestName(isAgent)} - " +
        s"${getTestTitleIncomeSourceType(incomeSourceType)} - IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod()" should {
        changeIncomeSourcesAccountingMethodTest(incomeSourceType, isAgent = isAgent, cashOrAccrualsAsAccountingMethod(incomeSourceType))
      }
    }
  }

}
