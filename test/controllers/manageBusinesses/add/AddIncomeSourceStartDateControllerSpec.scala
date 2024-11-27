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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import forms.incomeSources.add.AddIncomeSourceStartDateFormProvider
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.DateService
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.manageBusinesses.add.AddIncomeSourceStartDate

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

  val dayField = "value.day"
  val monthField = "value.month"
  val yearField = "value.year"

  val testDay = "01"
  val testMonth = "01"
  val testYear = "2022"

  val testStartDate: LocalDate = LocalDate.of(2022, 1, 1)

  val currentDate = dateService.getCurrentDate

  val maximumAllowableDatePlusOneDay = mockImplicitDateFormatter
    .longDate(currentDate.plusWeeks(1).plusDays(1))
    .toLongDate

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString)

  def sessionDataWithDate(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-01-01")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = sourceType match {
    case SelfEmployment => Some(sessionData(IncomeSourceJourneyType(Add, SelfEmployment)))
    case _ =>
      setupMockCreateSession(true)
      None
  }

  def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): String = ((isAgent, isChange, incomeSourceType) match {
    case (false, false, SelfEmployment) => routes.AddBusinessNameController.show(isChange = false)
    case (_, false, SelfEmployment) => routes.AddBusinessNameController.showAgent(isChange = false)
    case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
    case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    case (false, false, _) => controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent)
    case (_, false, _) => controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent)
    case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
    case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
    case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
    case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
  }).url

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): String = (incomeSourceType, isAgent, isChange) match {
    case (_, _, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType).url
  }

  def getRequest(isAgent: Boolean) = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean) = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  def getValidationErrorTabTitle(incomeSourceType: IncomeSourceType): String = {
    s"${messages("htmlTitle.invalidInput", messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))}"
  }

  def getHintText(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => "business started trading"
      case UkProperty => "UK property business started"
      case ForeignProperty => "foreign property business started"
    }
  }

  object TestAddIncomeSourceStartDateController extends AddIncomeSourceStartDateController(
    authorisedFunctions = mockAuthService,
    addIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate],
    customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError],
    sessionService = mockSessionService,
    form = app.injector.instanceOf[AddIncomeSourceStartDateFormProvider],
    testAuthenticator
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateFormatter = mockImplicitDateFormatter,
    dateService = app.injector.instanceOf[DateService],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec
  )

  for (incomeSourceType <- incomeSourceTypes) yield {
    for (isAgent <- Seq(true, false)) yield {
      s"AddIncomeSourceStartDateController.show (${incomeSourceType.key}, ${if (isAgent) "Agent" else "Individual"})" should {

        s"return ${Status.SEE_OTHER} and redirect to home page" when {
          s"incomeSources FS is disabled (${incomeSourceType.key})" in {
            disableAllSwitches()
            disable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))

            val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(getRequest(isAgent))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
        s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date page" when {
          "incomeSources FS is enabled" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)
            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))
            val result = TestAddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(getRequest(isAgent))

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
            val backUrl = getBackUrl(isAgent, false, incomeSourceType)
            document.getElementById("back-fallback").attr("href") shouldBe backUrl
            status(result) shouldBe OK
          }
        }
        s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date Change page" when {
          s"isChange flag set to true (${incomeSourceType.key})" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

            val result = TestAddIncomeSourceStartDateController
              .show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true)(
                getRequest(isAgent))

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
            val backUrl = getBackUrl(isAgent, true, incomeSourceType)
            document.getElementById("back-fallback").attr("href") shouldBe backUrl
            document.getElementById("value.day").attr("value") shouldBe "1"
            document.getElementById("value.month").attr("value") shouldBe "1"
            document.getElementById("value.year").attr("value") shouldBe "2022"
            status(result) shouldBe OK
          }
        }
        s"return ${Status.OK}: render the Add Business start date Change page with form filled " when {
          s"session contains key: $dateStartedField (${incomeSourceType.key})" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockCreateSession(true)
            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

            val result = TestAddIncomeSourceStartDateController
              .show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true)(
                getRequest(isAgent))

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
            document.getElementById("back-fallback").attr("href") shouldBe {
              if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
              else routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
            }
            document.getElementById("value.day").attr("value") shouldBe "1"
            document.getElementById("value.month").attr("value") shouldBe "1"
            document.getElementById("value.year").attr("value") shouldBe "2022"
            status(result) shouldBe OK
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
          s"user has already completed the journey (${incomeSourceType.key})" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(sessionDataCompletedJourney(IncomeSourceJourneyType(Add, incomeSourceType)))))

            val result: Future[Result] = TestAddIncomeSourceStartDateController.show(isAgent, isChange = false, incomeSourceType)(getRequest(isAgent))
            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
            else controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
          s"user has already added their income source (${incomeSourceType.key})" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(sessionDataISAdded(IncomeSourceJourneyType(Add, incomeSourceType)))))

            val result: Future[Result] = TestAddIncomeSourceStartDateController.show(isAgent, isChange = false, incomeSourceType)(getRequest(isAgent))
            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
            else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }

      s"AddIncomeSourceStartDateController.submit (${incomeSourceType.key}, ${if (isAgent) "Agent" else "Individual"})" should {
        s"return ${Status.SEE_OTHER} and redirect to home page" when {
          "incomeSources FS is disabled" in {
            disableAllSwitches()
            disable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(postRequest(isAgent))

            status(result) shouldBe SEE_OTHER
            val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
        s"return ${Status.BAD_REQUEST}" when {
          "an invalid form is submitted" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(postRequest(isAgent).withFormUrlEncodedBody("INVALID" -> "INVALID"))

            status(result) shouldBe BAD_REQUEST

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
          }
          "an empty form is submitted" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(postRequest(isAgent).withFormUrlEncodedBody("" -> ""))

            status(result) shouldBe BAD_REQUEST

            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
            document.getElementsByClass("govuk-error-message").text() shouldBe s"Error:: Enter the date your ${getHintText(incomeSourceType)}"
          }
          "no form is submitted" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(postRequest(isAgent))

            status(result) shouldBe BAD_REQUEST
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check page" when {
          "a valid form is submitted" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
            setupMockSetMongoData(true)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false)(postRequest(isAgent).withFormUrlEncodedBody(
              dayField -> "12",
              monthField -> "08",
              yearField -> "2023"
            ))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = false).url)
          }
        }
        s"return ${Status.SEE_OTHER}: redirect to the Add Business Start Date Check Change page" when {
          "a valid form is submitted" in {
            disableAllSwitches()
            enable(IncomeSourcesFs)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
            setupMockSetMongoData(true)

            val result = TestAddIncomeSourceStartDateController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true)(postRequest(isAgent).withFormUrlEncodedBody(
              dayField -> testDay,
              monthField -> testMonth,
              yearField -> testYear
            ))

            redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = true).url)
            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }
  }
}
