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
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.CreateBusinessDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate

class IncomeSourceCheckDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with MockSessionService with FeatureSwitching{

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
            else{
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
    }
  }

  //show:
  //200
  //303 - incomeSources disabled/unauthorised
  //500 - session data missing

  //submit:
  //303 - correct
  //303 to custom error page

}
