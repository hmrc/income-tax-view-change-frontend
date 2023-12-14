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
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourceAddedField, journeyIsCompleteField}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSessionId}
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import java.time.LocalDate
import scala.concurrent.Future


class AddIncomeSourceStartDateControllerSpec extends TestSupport with MockSessionService
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching {

  val dayField = "income-source-start-date.day"
  val monthField = "income-source-start-date.month"
  val yearField = "income-source-start-date.year"

  val testDay = "01"
  val testMonth = "01"
  val testYear = "2022"

  val testStartDate: LocalDate = LocalDate.of(2022, 1, 1)

  val currentDate = dateService.getCurrentDate()

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))
  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData()))
  def sessionDataWithDate(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-01-01")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = sourceType match {
    case SelfEmployment => Some(sessionData(JourneyType(Add, SelfEmployment)))
    case _ =>
      setupMockCreateSession(true)
      None
  }

  def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): String = ((isAgent, isChange, incomeSourceType) match {
    case (false, false, SelfEmployment) => routes.AddBusinessNameController.show()
    case (_, false, SelfEmployment) => routes.AddBusinessNameController.showAgent()
    case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
    case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    case (false, false, _) => routes.AddIncomeSourceController.show()
    case (_, false, _) => routes.AddIncomeSourceController.showAgent()
    case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
    case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
    case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
    case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
  }).url

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): String = (incomeSourceType, isAgent, isChange) match {
    case (_, _, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType).url
  }

  object TestAddIncomeSourceStartDateController extends AddIncomeSourceStartDateController(
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    addIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate],
    retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    sessionService = mockSessionService
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateFormatter = mockImplicitDateFormatter,
    dateService = app.injector.instanceOf[DateService],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec
  )


  "Individual: AddIncomeSourceStartDateController.show" should {
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"return ${Status.SEE_OTHER} and redirect to home page (${incomeSourceType.key})" when {
        s"incomeSources FS is disabled (${incomeSourceType.key})" in {
          disableAllSwitches()
          disable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.routes.HomeController.show().url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
    }
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date page" when {
        "incomeSources FS is enabled" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))
          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
          val backUrl = getBackUrl(false, false, incomeSourceType)
          document.getElementById("back").attr("href") shouldBe backUrl
          status(result) shouldBe OK
        }
      }
    }
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date Change page" when {
        s"isChange flag set to true (${incomeSourceType.key})" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          val journeyType = JourneyType(Add, incomeSourceType)
          setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testStartDate)))
          setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

          val result = TestAddIncomeSourceStartDateController
            .show(incomeSourceType = incomeSourceType, isAgent = false, isChange = true)(
              fakeRequestWithActiveSession)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
          val backUrl = getBackUrl(false, true, incomeSourceType)
          document.getElementById("back").attr("href") shouldBe backUrl
          document.getElementById("income-source-start-date.day").attr("value") shouldBe "1"
          document.getElementById("income-source-start-date.month").attr("value") shouldBe "1"
          document.getElementById("income-source-start-date.year").attr("value") shouldBe "2022"
          status(result) shouldBe OK
        }
      }
    }
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
        s"user has already completed the journey (${incomeSourceType.key})" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetMongo(Right(Some(sessionDataCompletedJourney(JourneyType(Add, incomeSourceType)))))

          val result: Future[Result] = TestAddIncomeSourceStartDateController.show(false, isChange = false, incomeSourceType)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
        s"user has already added their income source (${incomeSourceType.key})" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetMongo(Right(Some(sessionDataISAdded(JourneyType(Add, incomeSourceType)))))

          val result: Future[Result] = TestAddIncomeSourceStartDateController.show(false, isChange = false, incomeSourceType)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
    }

    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"session contains IncomeSourceStartDate with invalid format for the change journey" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController
          .show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Individual: AddIncomeSourceStartDateController.submit" should {
    s"return ${Status.SEE_OTHER} and redirect to home page" when {
      "incomeSources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        val redirectUrl = controllers.routes.HomeController.show().url
        redirectLocation(result) shouldBe Some(redirectUrl)
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "an invalid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("INVALID" -> "INVALID"))

        status(result) shouldBe BAD_REQUEST
      }
      "an empty form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody("" -> ""))

        status(result) shouldBe BAD_REQUEST
      }
      "no form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession)

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> "12",
          monthField -> "08",
          yearField -> "2023"
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check Change page" when {
      "a valid form is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(fakePostRequestWithActiveSession.withFormUrlEncodedBody(
          dayField -> testDay,
          monthField -> testMonth,
          yearField -> testYear
        ))

        redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url)
        status(result) shouldBe SEE_OTHER
      }
    }


    "Agent: AddIncomeSourceStartDateController.show" should {
      s"return ${Status.SEE_OTHER}: redirect to home page" when {
        "incomeSources FS is disabled" in {
          disableAllSwitches()
          disable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.routes.HomeController.showAgent.url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
      s"return ${Status.OK}: render the Add Business start date page" when {
        "incomeSources FS is enabled" in {
          disableAllSwitches()
          enable(IncomeSources)
          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockGetMongo(Right(getInitialMongo(SelfEmployment)))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())


          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("add-business-start-date.heading"))
          document.getElementById("back").attr("href") shouldBe routes.AddBusinessNameController.showAgent().url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add UK property start date page" when {
        "incomeSources FS is enabled" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockGetMongo(Right(getInitialMongo(UkProperty)))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
          document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.showAgent().url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add Foreign property start date page" when {
        "incomeSources FS is enabled" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockGetMongo(Right(getInitialMongo(ForeignProperty)))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
          document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceController.showAgent().url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add Foreign property start date Change page" when {
        "isUpdate flag set to true" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          val journeyType = JourneyType(Add, ForeignProperty)
          setupMockGetSessionKeyMongoTyped[LocalDate](Right(Some(testStartDate)))
          setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(fakeRequestConfirmedClient())
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("incomeSources.add.foreignProperty.startDate.heading"))
          document.getElementById("back").attr("href") shouldBe routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty).url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add UK property start date Change page" when {
        "isUpdate flag set to true" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          val journeyType = JourneyType(Add, UkProperty)
          setupMockGetSessionKeyMongoTyped[LocalDate](Right(Some(testStartDate)))
          setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true)(fakeRequestConfirmedClient())

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("incomeSources.add.UKPropertyStartDate.heading"))
          document.getElementById("back").attr("href") shouldBe routes.IncomeSourceCheckDetailsController.showAgent(UkProperty).url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add Business start date Change page" when {
        "isUpdate flag set to true" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          val journeyType = JourneyType(Add, SelfEmployment)
          setupMockGetSessionKeyMongoTyped[LocalDate](Right(Some(testStartDate)))
          setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

          val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(fakeRequestConfirmedClient())

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("add-business-start-date.heading"))
          document.getElementById("back").attr("href") shouldBe routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
          status(result) shouldBe OK
        }
      }
      s"return ${Status.OK}: render the Add Business start date Change page with form filled" when {
        s"session contains key: ${dateStartedField} " in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          val journeyType = JourneyType(Add, SelfEmployment)
          setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testStartDate)))
          setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

          val result = TestAddIncomeSourceStartDateController
            .show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(
              fakeRequestConfirmedClient())

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title should include(messages("add-business-start-date.heading"))
          document.getElementById("back").attr("href") shouldBe routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
          document.getElementById("income-source-start-date.day").attr("value") shouldBe "1"
          document.getElementById("income-source-start-date.month").attr("value") shouldBe "1"
          document.getElementById("income-source-start-date.year").attr("value") shouldBe "2022"
          status(result) shouldBe OK
        }
      }
    }
    "Agent: AddIncomeSourceStartDateController.submit" should {
      s"return ${Status.SEE_OTHER}: redirect to the home page" when {
        "incomeSources FS is disabled" in {
          disableAllSwitches()
          disable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient())

          status(result) shouldBe SEE_OTHER
          val redirectUrl = controllers.routes.HomeController.showAgent.url
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
      s"return ${Status.BAD_REQUEST}" when {
        "an invalid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody("INVALID" -> "INVALID"))

          status(result) shouldBe BAD_REQUEST
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url)
          status(result) shouldBe SEE_OTHER
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url)
          status(result) shouldBe SEE_OTHER
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url)
          status(result) shouldBe SEE_OTHER
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check Change page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true).url)
          status(result) shouldBe SEE_OTHER
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add Foreign Property Start Date Check Change page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true).url)
          status(result) shouldBe SEE_OTHER
        }
      }
      s"return ${Status.SEE_OTHER}: redirect to the Add UK Property Start Date Check Change page" when {
        "a valid form is submitted" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          setupMockCreateSession(true)
          setupMockSetSessionKeyMongo(Right(true))

          val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = true)(fakePostRequestConfirmedClient().withFormUrlEncodedBody(
            dayField -> testDay,
            monthField -> testMonth,
            yearField -> testYear
          ))

          redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true).url)
          status(result) shouldBe SEE_OTHER
        }
      }
    }
  }
}
