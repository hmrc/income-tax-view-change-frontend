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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, isA}
import org.mockito.Mockito.{mock, verify}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSessionId}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate
import scala.concurrent.Future


class AddIncomeSourceStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching
  with MockSessionService {

  val testStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodEndDate: LocalDate = LocalDate.of(2024, 4, 5)

  val responseNo: String = AddIncomeSourceStartDateCheckForm.responseNo
  val responseYes: String = AddIncomeSourceStartDateCheckForm.responseYes

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val journeyTypeSE = JourneyType(Add, SelfEmployment)
  val journeyTypeUK = JourneyType(Add, UkProperty)
  val journeyTypeFP = JourneyType(Add, ForeignProperty)
  def journeyType(sourceType: IncomeSourceType) = sourceType match {
    case SelfEmployment => journeyTypeSE
    case UkProperty => journeyTypeUK
    case ForeignProperty => journeyTypeFP
  }

  val addIncomeSourceDataEmpty = AddIncomeSourceData()
  val addIncomeSourceDataProperty = AddIncomeSourceData(dateStarted = Some(testStartDate))
  val addIncomeSourceDataSE = AddIncomeSourceData(dateStarted = Some(testStartDate), accountingPeriodStartDate = Some(testBusinessAccountingPeriodStartDate),
    accountingPeriodEndDate = Some(testBusinessAccountingPeriodEndDate))

  val addIncomeSourceDataPropertyWithAccSD = AddIncomeSourceData(dateStarted = Some(testStartDate), accountingPeriodStartDate = Some(testStartDate))

  val uiJourneySessionDataSE: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(addIncomeSourceDataSE))
  val uiJourneySessionDataUK: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-UK", Some(addIncomeSourceDataProperty))
  val uiJourneySessionDataFP: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-FP", Some(addIncomeSourceDataProperty))
  def uiJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = incomeSourceType match {
    case SelfEmployment => uiJourneySessionDataSE
    case UkProperty => uiJourneySessionDataUK
    case ForeignProperty => uiJourneySessionDataFP
  }

  val UIJourneySessionDataUkWithAccSD: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-UK", Some(addIncomeSourceDataPropertyWithAccSD))
  val UIJourneySessionDataFpWithAccSD: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-FP", Some(addIncomeSourceDataPropertyWithAccSD))
  def dataWithAccSD(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => uiJourneySessionDataSE
    case UkProperty => UIJourneySessionDataUkWithAccSD
    case ForeignProperty => UIJourneySessionDataFpWithAccSD
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  object TestAddIncomeSourceStartDateCheckController
    extends AddIncomeSourceStartDateCheckController(
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      addIncomeSourceStartDateCheckView = app.injector.instanceOf[AddIncomeSourceStartDateCheck],
      languageUtils = languageUtils,
      sessionService = mockSessionService,
      testAuthenticator
    )(
      app.injector.instanceOf[FrontendAppConfig],
      dateService,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler]
    ) {

    val heading: String = messages("dateForm.check.heading")
    val titleAgent: String = s"${messages("htmlTitle.agent", heading)}"
    val title: String = s"${messages("htmlTitle", heading)}"
  }

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))
  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData()))

  def sessionDataWithDate(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-11-11")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = {
    Some(sessionData(JourneyType(Add, sourceType)))
  }

  def verifyMongoDatesRemoved(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())(any(), any())

    argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataEmpty)
  }

  def verifySetMongoData(incomeSourceType: IncomeSourceType): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())(any(), any())

    if (incomeSourceType.equals(SelfEmployment)) {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataSE)
    } else {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataProperty)
    }
  }

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }

  "AddIncomeSourceStartDateCheckController.show" should {
    for (incomeSourceType <- incomeSourceTypes) yield {
      for (isAgent <- Seq(true, false)) yield {
        s"return ${Status.OK}: render the custom not found error view (isAgent = $isAgent, $incomeSourceType)" when {
          "Income Sources FS is disabled" in {showISDisabledTest(isAgent, incomeSourceType)}
        }
        s"return ${Status.SEE_OTHER}: redirect to home page (isAgent = $isAgent, $incomeSourceType)" when {
          "called with an unauthenticated user" in {showUnauthenticatedTest(isAgent, incomeSourceType)}
        }
        s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page (isAgent = $isAgent, $incomeSourceType)" when {
          "user has already completed the journey" in {showCompletedBackTest(isAgent, incomeSourceType)}
          "user has already added their income source" in {showAddedBackTest(isAgent, incomeSourceType)}
        }
        s"return ${Status.INTERNAL_SERVER_ERROR} (isAgent = $isAgent, $incomeSourceType)" when {
          s"calling Business Start Date Check Page but session does not contain key: ${AddIncomeSourceData.accountingPeriodStartDateField}" in {showSessionMissingTest(isAgent, incomeSourceType)}
        }
        s"return ${Status.OK} (isAgent = $isAgent, $incomeSourceType)" when {
          s"calling Business Start Date Check Page and session contains key: ${AddIncomeSourceData.accountingPeriodStartDateField}" in {showSuccessTest(isAgent, incomeSourceType)}
        }
        s"return ${Status.OK}: render the Add Business Start Date Check Change page (isAgent = $isAgent, $incomeSourceType)" when {
          "isUpdate flag set to true" in {showChangeTest(isAgent, incomeSourceType)}
        }
      }
    }

    def showISDisabledTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)

      val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession})

      status(result) shouldBe SEE_OTHER
      val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
      redirectLocation(result) shouldBe Some(redirectUrl)
    }
    def showUnauthenticatedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
      val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession})

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
    }
    def showCompletedBackTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetMongo(Right(Some(sessionDataCompletedJourney(journeyType(incomeSourceType)))))

      val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession})
      status(result) shouldBe SEE_OTHER
      val redirectUrl = if (isAgent) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
      else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
      redirectLocation(result) shouldBe Some(redirectUrl)
    }
    def showAddedBackTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetMongo(Right(Some(sessionDataISAdded(journeyType(incomeSourceType)))))

      val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession})
      status(result) shouldBe SEE_OTHER
      val redirectUrl = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
      else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
      redirectLocation(result) shouldBe Some(redirectUrl)
    }
    def showSessionMissingTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetMongo(Right(Some(sessionData(journeyType(incomeSourceType)))))

      val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession})

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
    def showSuccessTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(dataWithAccSD(incomeSourceType))))

      val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {
          if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession
        }
      )

      val document: Document = Jsoup.parse(contentAsString(result))

      document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false).url
      status(result) shouldBe OK
    }
    def showChangeTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType(incomeSourceType)))))

      val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true)(
        {
          if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithActiveSession
        }
      )

      val document: Document = Jsoup.parse(contentAsString(result))

      document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true).url
      status(result) shouldBe OK
    }

  }

  "AddIncomeSourceStartDateCheckController.submit" should {
    for (incomeSourceType <- incomeSourceTypes) yield {
      for (isAgent <- Seq(true, false)) yield {
        s"return ${Status.SEE_OTHER}: redirect to home page(isAgent = $isAgent, $incomeSourceType)" when {
          "IncomeSources FS is disabled" in {
            submitISDisabled(isAgent, incomeSourceType)
          }
        }
        s"return ${Status.BAD_REQUEST} with an error summary(isAgent = $isAgent, $incomeSourceType)" when {
          "form is submitted with neither radio option selected" in {
            submitNoOptionsTest(isAgent, incomeSourceType)
          }
          "an invalid response is submitted" in {
            submitInvalidResponseTest(isAgent, incomeSourceType)
          }
        }
        s"return ${Status.SEE_OTHER}: redirect back to add $incomeSourceType start date page with ${AddIncomeSourceData.accountingPeriodStartDateField} removed from session, isAgent = $isAgent" when {
          "No is submitted with the form" in {
            submitRedirectWithAccPeriodRemovedTest(isAgent, incomeSourceType)
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to $incomeSourceType accounting method page, isAgent = $isAgent" when {
          "Yes is submitted with the form with a valid session" in {
            submitRedirectAccMethodTest(isAgent, incomeSourceType)
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to check $incomeSourceType details page, isAgent = $isAgent" when {
          "Yes is submitted with isUpdate flag set to true" in {
            submitRedirectCheckDetailsTest(isAgent, incomeSourceType)
          }
        }
      }
    }

    def submitISDisabled(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()

      mockNoIncomeSources()
      authenticate(isAgent)

      val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {
          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> responseYes
          ))

      status(result) shouldBe SEE_OTHER
      val redirectUrl = if(isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
      redirectLocation(result) shouldBe Some(redirectUrl)
    }

    def submitNoOptionsTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate((isAgent))
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(sessionDataWithDate(JourneyType(Add, incomeSourceType)))))

      val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {
          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }

          .withFormUrlEncodedBody())

      status(result) shouldBe BAD_REQUEST
    }

    def submitInvalidResponseTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(sessionDataWithDate(JourneyType(Add, incomeSourceType)))))
      setupMockGetMongo(Right(Some(uiJourneySessionDataFP)))

      val invalidResponse: String = "£££"

      val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {

          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }

          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> invalidResponse
          ))

      status(result) shouldBe BAD_REQUEST
    }

    def submitRedirectWithAccPeriodRemovedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(uiJourneySessionData(incomeSourceType))))
      setupMockSetMongoData(result = true)

      val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {
          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> responseNo
          ))

      status(result) shouldBe SEE_OTHER
      verifyMongoDatesRemoved()
      redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false).url)
    }

    def submitRedirectAccMethodTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)

      mockNoIncomeSources()
      authenticate(isAgent)
      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockSetMongoData(result = true)
      setupMockGetMongo(Right(Some(sessionDataWithDate(JourneyType(Add, incomeSourceType)))))

      val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(
        {
          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }

          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> responseYes
          ))

      status(result) shouldBe SEE_OTHER
      if (incomeSourceType == SelfEmployment) verifySetMongoData(SelfEmployment)
      redirectLocation(result) shouldBe Some({
        (isAgent, incomeSourceType) match {
          case (false, SelfEmployment) => controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent, isChange = false)
          case (true, SelfEmployment) => controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent, isChange = false)
          case _ => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)
        }
      }.url)
    }

    def submitRedirectCheckDetailsTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
      disableAllSwitches()
      enable(IncomeSources)
      authenticate(isAgent)
      mockNoIncomeSources()

      setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
      setupMockGetMongo(Right(Some(sessionDataWithDate(JourneyType(Add, incomeSourceType)))))
      setupMockSetMongoData(result = true)

      val result = TestAddIncomeSourceStartDateCheckController.submit(isAgent, isChange = true, incomeSourceType)(
        {
          if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithActiveSession
        }
          .withFormUrlEncodedBody(
            AddIncomeSourceStartDateCheckForm.response -> responseYes
          ))

      status(result) shouldBe SEE_OTHER
      if (incomeSourceType == SelfEmployment) verifySetMongoData(SelfEmployment)
      redirectLocation(result) shouldBe Some({
        if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType) else routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      }.url)
    }
  }
}

