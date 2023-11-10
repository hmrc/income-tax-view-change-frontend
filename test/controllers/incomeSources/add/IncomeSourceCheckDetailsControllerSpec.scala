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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockSessionService
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.CreateBusinessDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceCheckDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with MockSessionService with FeatureSwitching {

  val testBusinessId: String = "some-income-source-id"
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 2)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "123 Main Street"
  val testBusinessPostCode: String = "AB123CD"
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))
  val testBusinessAccountingMethod = "Quarterly"
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckBusinessDetails: IncomeSourceCheckDetails = app.injector.instanceOf[IncomeSourceCheckDetails]
  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  val testPropertyStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testPropertyAccountingMethod: String = "CASH"
  val accruals: String = messages("incomeSources.add.accountingMethod.accruals")

  val testUIJourneySessionDataBusiness: UIJourneySessionData = UIJourneySessionData(
    sessionId = "some-session-id",
    journeyType = JourneyType(Add, SelfEmployment).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(
      businessName = Some(testBusinessName),
      businessTrade = Some(testBusinessTrade),
      dateStarted = Some(testBusinessStartDate),
      createdIncomeSourceId = Some(testBusinessId),
      address = Some(testBusinessAddress),
      countryCode = Some(testCountryCode),
      accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
      incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
    )))

  def testUIJourneySessionDataProperty(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = "some-session-id",
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(
      dateStarted = Some(testBusinessStartDate),
      incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
    )))

  object TestCheckDetailsController extends IncomeSourceCheckDetailsController(
    checkDetailsView = app.injector.instanceOf[IncomeSourceCheckDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate,
    businessDetailsService = mockBusinessDetailsService
  )(ec, mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    sessionService = mockSessionService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )

  def getHeading(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => messages("check-business-details.title")
      case UkProperty => messages("incomeSources.add.checkUKPropertyDetails.title")
      case ForeignProperty => messages("incomeSources.add.foreign-property-check-details.title")
    }
  }

  def getTitle(sourceType: IncomeSourceType, isAgent: Boolean): String = {
    val prefix: String = if (isAgent) "htmlTitle.agent" else "htmlTitle"
    sourceType match {
      case SelfEmployment => s"${messages(prefix, messages("check-business-details.title"))}"
      case UkProperty => messages(prefix, messages("incomeSources.add.checkUKPropertyDetails.title"))
      case ForeignProperty => messages(prefix, messages("incomeSources.add.foreign-property-check-details.title"))
    }
  }

  def getLink(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => s"${messages("check-business-details.change")}"
      case UkProperty => s"${messages("check-business-details.change")}"
      case ForeignProperty => s"${messages("incomeSources.add.foreign-property-check-details.change")}"
    }
  }

  "IncomeSourceCheckDetailsController" should {
    ".show" should {
      "return 200 OK" when {
        "the session contains full business details and FS enabled" when {
          def runSuccessTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
            if (incomeSourceType == SelfEmployment) {
              val sessionData: UIJourneySessionData = if (incomeSourceType == SelfEmployment) testUIJourneySessionDataBusiness else testUIJourneySessionDataProperty(incomeSourceType)
              setupMockGetMongo(Right(Some(sessionData)))
            }
            else {
              setupMockCreateSession(true)
              setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, JourneyType(Add, incomeSourceType), Right(Some(testPropertyStartDate)))
              setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType), Right(Some(accruals)))
            }

            val result = if (isAgent) TestCheckDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.show(incomeSourceType)(fakeRequestWithActiveSession)

            val document: Document = Jsoup.parse(contentAsString(result))
            val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")

            status(result) shouldBe OK
            document.title shouldBe getTitle(incomeSourceType, isAgent)
            document.select("h1:nth-child(1)").text shouldBe getHeading(incomeSourceType)
            changeDetailsLinks.first().text shouldBe getLink(incomeSourceType)
          }

          "individual" when {
            "Self Employment" in {
              runSuccessTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              runSuccessTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              runSuccessTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              runSuccessTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              runSuccessTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              runSuccessTest(isAgent = true, ForeignProperty)
            }
          }
        }
      }

      "return 303 and redirect an individual back to the home page" when {
        "the IncomeSources FS is disabled" when {
          def runFSDisabledTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            disable(IncomeSources)
            mockSingleBusinessIncomeSource()
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)

            val result = if (isAgent) TestCheckDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.show(incomeSourceType)(fakeRequestWithActiveSession)

            val redirectUrl = if(isAgent) controllers.routes.HomeController.showAgent.url
            else controllers.routes.HomeController.show().url

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment" in {
              runFSDisabledTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              runFSDisabledTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              runFSDisabledTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              runFSDisabledTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              runFSDisabledTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              runFSDisabledTest(isAgent = true, ForeignProperty)
            }
          }
        }

        "called with an unauthenticated user" when {
          def runUnauthorisedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
            val result = if (isAgent) TestCheckDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.show(incomeSourceType)(fakeRequestWithActiveSession)
            status(result) shouldBe SEE_OTHER
          }

          "individual" when {
            "Self Employment" in {
              runUnauthorisedTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              runUnauthorisedTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              runUnauthorisedTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              runUnauthorisedTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              runUnauthorisedTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              runUnauthorisedTest(isAgent = true, ForeignProperty)
            }
          }
        }
      }

      "return 500 INTERNAL_SERVER_ERROR" when {
        "there is session data missing" when {
          def missingSessionDataTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
            if (incomeSourceType == SelfEmployment) {
              setupMockGetMongo(Right(None))
            }
            else {
              setupMockCreateSession(true)
              setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, JourneyType(Add, incomeSourceType), Right(None))
              setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType), Right(Some(accruals)))
            }

            val result = if (isAgent) TestCheckDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.show(incomeSourceType)(fakeRequestWithActiveSession)

            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "individual" when {
            "Self Employment" in {
              missingSessionDataTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              missingSessionDataTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              missingSessionDataTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              missingSessionDataTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              missingSessionDataTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              missingSessionDataTest(isAgent = true, ForeignProperty)
            }
          }
        }
      }
    }

    ".submit" should{
      "return 303" when {
        "data is correct and redirect next page" when {
          def successFullRedirectTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
            when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
              .thenReturn(Future {
                Right(CreateIncomeSourceResponse(testBusinessId))
              })
            if (incomeSourceType == SelfEmployment) {
              val sessionData: UIJourneySessionData = if (incomeSourceType == SelfEmployment) testUIJourneySessionDataBusiness else testUIJourneySessionDataProperty(incomeSourceType)
              setupMockGetMongo(Right(Some(sessionData)))
            }
            else {
              setupMockCreateSession(true)
              setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, JourneyType(Add, incomeSourceType), Right(Some(testPropertyStartDate)))
              setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType), Right(Some(accruals)))
            }
            when(mockSessionService.deleteMongoData(any())(any())).thenReturn(Future(true))

            val result = if (isAgent) TestCheckDetailsController.submitAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.submit(incomeSourceType)(fakeRequestWithActiveSession)

            val redirectUrl = (isAgent, incomeSourceType) match {
              case (false, SelfEmployment) => controllers.incomeSources.add.routes.BusinessReportingMethodController.show(testBusinessId).url
              case (true, SelfEmployment) => controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(testBusinessId).url
              case (false, UkProperty) => controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testBusinessId).url
              case (true, UkProperty) => controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testBusinessId).url
              case (false, ForeignProperty) => controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(testBusinessId).url
              case (true, ForeignProperty) => controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(testBusinessId).url
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment" in {
              successFullRedirectTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              successFullRedirectTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              successFullRedirectTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              successFullRedirectTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              successFullRedirectTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              successFullRedirectTest(isAgent = true, ForeignProperty)
            }
          }
        }
        "redirect to custom error page when unable to create business" when {
          def businessCreateFailTest(isAgent: Boolean, incomeSourceType: IncomeSourceType) = {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
            when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
              .thenReturn(Future {
                Left(new Error("Test Error"))
              })
            if (incomeSourceType == SelfEmployment) {
              val sessionData: UIJourneySessionData = if (incomeSourceType == SelfEmployment) testUIJourneySessionDataBusiness else testUIJourneySessionDataProperty(incomeSourceType)
              setupMockGetMongo(Right(Some(sessionData)))
            }
            else {
              setupMockCreateSession(true)
              setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, JourneyType(Add, incomeSourceType), Right(Some(testPropertyStartDate)))
              setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType), Right(Some(accruals)))
            }
            when(mockSessionService.deleteMongoData(any())(any())).thenReturn(Future(true))

            val result = if (isAgent) TestCheckDetailsController.submitAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestCheckDetailsController.submit(incomeSourceType)(fakeRequestWithActiveSession)

            val redirectUrl = if(isAgent) controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url
            else controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType).url

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment" in {
              businessCreateFailTest(isAgent = false, SelfEmployment)
            }
            "Uk Property" in {
              businessCreateFailTest(isAgent = false, UkProperty)
            }
            "Foreign Property" in {
              businessCreateFailTest(isAgent = false, ForeignProperty)
            }
          }
          "agent" when {
            "Self Employment" in {
              businessCreateFailTest(isAgent = true, SelfEmployment)
            }
            "Uk Property" in {
              businessCreateFailTest(isAgent = true, UkProperty)
            }
            "Foreign Property" in {
              businessCreateFailTest(isAgent = true, ForeignProperty)
            }
          }
        }
      }
    }
  }
}
