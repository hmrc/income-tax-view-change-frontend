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

package controllers.incomeSources.manage

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import mocks.connectors.MockBusinessDetailsConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockSessionService
import models.core.AddressModel
import models.core.IncomeSourceId.mkIncomeSourceId
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.{CalculationListService, DateService, ITSAStatusService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.address
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageIncomeSourceDetails

import scala.concurrent.Future

class ManageIncomeSourceDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockBusinessDetailsConnector with MockNavBarEnumFsPredicate
  with MockSessionService {

  val mockDateService: DateService = mock(classOf[DateService])
  val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])
  val incomeSourceIdHash: String = mkIncomeSourceId(testSelfEmploymentId).toHash.hash

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
    enable(IncomeSources)
    reset(mockCalculationListService)
    reset(mockITSAStatusService)
    reset(mockDateService)
  }

  object TestManageIncomeSourceDetailsController extends ManageIncomeSourceDetailsController(
    view = app.injector.instanceOf[ManageIncomeSourceDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    mockITSAStatusService,
    mockDateService,
    retrieveBtaNavBar = MockNavBarPredicate,
    mockCalculationListService,
    sessionService = mockSessionService
  )(
    ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig]
  ) {
    val heading: String = messages("incomeSources.manage.business-manage-details.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val titleAgent: String = s"${messages("htmlTitle.agent", heading)}"
    val link: String = s"${messages("incomeSources.manage.business-manage-details.change")}"
    val incomeSourceId: String = "XAIS00000000008"
    val businessWithLatencyAddress: String = "8 Test New Court New Town New City NE12 6CI United Kingdom"
    val unknown: String = messages("incomeSources.generic.unknown")
    val annually: String = messages("incomeSources.manage.business-manage-details.annually")
    val quarterly: String = messages("incomeSources.manage.business-manage-details.quarterly")
    val standard: String = messages("incomeSources.manage.quarterly-period.standard")
    val calendar: String = messages("incomeSources.manage.quarterly-period.calendar")

  }

  sealed trait Scenario

  case object ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION extends Scenario

  case object NON_ELIGIBLE_ITSA_STATUS extends Scenario

  case object FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED extends Scenario

  case object FIRST_AND_SECOND_YEAR_CRYSTALLIZED extends Scenario

  case object ERROR_TESTING extends Scenario

  val testBusinessAddress: AddressModel = address
  val testId = "XAIS00000000001"

  def mockAndBasicSetup(scenario: Scenario, isAgent: Boolean = false): Unit = {
    if (isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    } else {
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    }

    setupMockCreateSession(true)

    scenario match {
      case ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2024)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockUkPlusForeignPlusSoleTraderNoLatency()
        when(mockSessionService.createSession(any())(any(), any())).thenReturn(Future.successful(true))
        when(mockSessionService.setMongoKey(any(), any(), any())(any(), any())).thenReturn(Future(Right(true)))

      case FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockUkPlusForeignPlusSoleTraderWithLatency()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2023), any)(any, any, any))
          .thenReturn(Future.successful(Some(false)))
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2024), any)(any, any, any))
          .thenReturn(Future.successful(Some(false)))

      case FIRST_AND_SECOND_YEAR_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockUkPlusForeignPlusSoleTraderWithLatency()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2023), any)(any, any, any))
          .thenReturn(Future.successful(Some(true)))
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2024), any)(any, any, any))
          .thenReturn(Future.successful(Some(true)))

      case NON_ELIGIBLE_ITSA_STATUS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(false))
        mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns()


      case ERROR_TESTING =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(false))
    }
  }

  "Individual - ManageIncomeSourceDetailsController" should {
    "redirect an user to the home page" when {
      "incomeSources FS is disabled" in {
        disable(IncomeSources)
        setupMockAuthorisationSuccess(false)
        mockBothPropertyBothBusiness()
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)

        status(result) shouldBe Status.SEE_OTHER

      }
    }
  }
  "Agent - ManageIncomeSourceDetailsController" should {
    "redirect an agent to the home page" when {
      "incomeSources FS is disabled" in {
        disable(IncomeSources)
        setupMockAuthorisationSuccess(true)
        mockBothPropertyBothBusiness()
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result = TestManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceIdHash)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "ManageIncomeSourceDetailsController.showSoleTraderBusiness" should {
    "return 200 OK" when {
      "FS is enabled and the .show(id) method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__row").get(1).getElementsByTag("dt").text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__row").get(1).getElementsByTag("dd").text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.calendar
      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(5).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress

      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(5).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress

      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(0).text() shouldBe "Business name"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(2).text() shouldBe "Date started"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(3).text() shouldBe "Accounting method for sole trader income"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  "ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent" should {
    "return 200 OK" when {
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceIdHash)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.calendar
      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceIdHash)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(5).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress

      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceIdHash)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(5).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.businessWithLatencyAddress

      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, SelfEmployment)))))
        setupMockSetSessionKeyMongo(Right(true))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceIdHash)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(0).text() shouldBe "Business name"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe "Business address"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(2).text() shouldBe "Date started"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__key").get(3).text() shouldBe "Accounting method for sole trader income"
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  "ManageIncomeSourceDetailsController.showUkProperty" should {
    "return 200 OK" when {
      "FS is enabled and the .show method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.standard
      }
      "FS is enabled and the .show method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .show method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .show method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  "ManageIncomeSourceDetailsController.showUkPropertyAgent" should {
    "return 200 OK" when {
      "FS is enabled and the .showAgent method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.standard
      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.quarterly
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(4).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, UkProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showUkPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  "ManageIncomeSourceDetailsController.showForeignProperty" should {
    "return 200 OK" when {
      "FS is enabled and the .show method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar
      }
      "FS is enabled and the .show method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .show method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .show method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignProperty(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  "ManageIncomeSourceDetailsController.showForeignPropertyAgent" should {
    "return 200 OK" when {
      "FS is enabled and the .showAgent method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.calendar

      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(2).text() shouldBe TestManageIncomeSourceDetailsController.annually
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(3).text() shouldBe TestManageIncomeSourceDetailsController.annually

      }
      "FS is enabled and the .showAgent method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS, isAgent = true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, ForeignProperty)))))

        val result: Future[Result] = TestManageIncomeSourceDetailsController.showForeignPropertyAgent(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageIncomeSourceDetailsController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageIncomeSourceDetailsController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe TestManageIncomeSourceDetailsController.unknown
        document.getElementById("manage-details-table")
          .getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe TestManageIncomeSourceDetailsController.unknown

      }
    }
  }

  //error scenarios:
  "Any .show method" should {
    "throw an error" when {
      "User has no income source of the called type" in {
        mockAndBasicSetup(ERROR_TESTING)
        mockUKPropertyIncomeSource()
        val resultSE: Future[Result] = TestManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceIdHash)(fakeRequestWithNino)
        status(resultSE) shouldBe Status.INTERNAL_SERVER_ERROR

        mockAndBasicSetup(ERROR_TESTING)
        mockSingleBusinessIncomeSource()
        val resultUk: Future[Result] = TestManageIncomeSourceDetailsController.showUkProperty(fakeRequestWithNino)
        status(resultUk) shouldBe Status.INTERNAL_SERVER_ERROR

        mockAndBasicSetup(ERROR_TESTING)
        mockSingleBusinessIncomeSource()
        val resultFP: Future[Result] = TestManageIncomeSourceDetailsController.showForeignProperty(fakeRequestWithNino)
        status(resultFP) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }
  }
}
