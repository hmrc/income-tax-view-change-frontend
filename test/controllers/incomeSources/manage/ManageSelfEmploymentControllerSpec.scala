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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.{CalculationListService, DateService, ITSAStatusService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.testBizAddress
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageSelfEmployment

import scala.concurrent.Future

class ManageSelfEmploymentControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockIncomeTaxViewChangeConnector with MockNavBarEnumFsPredicate {

  val mockDateService: DateService = mock(classOf[DateService])
  val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationListService)
    reset(mockITSAStatusService)
    reset(mockDateService)
  }

  object TestManageSelfEmploymentController extends ManageSelfEmploymentController (
    view = app.injector.instanceOf[ManageSelfEmployment],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    mockITSAStatusService,
    mockDateService,
    retrieveBtaNavBar = MockNavBarPredicate,
    mockCalculationListService
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
    val businessWithLatencyAddress: String = "64 Zoo Lane, Happy Place, Magical Land, England, ZL1 064, UK"
    val unknown: String = messages("incomeSources.generic.unknown")
    val annually: String = messages("incomeSources.manage.business-manage-details.annually")
    val quarterly: String = messages("incomeSources.manage.business-manage-details.quarterly")

  }

  sealed trait Scenario
  case object ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION extends Scenario
  case object NON_ELIGIBLE_ITSA_STATUS extends Scenario
  case object FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED extends Scenario
  case object FIRST_AND_SECOND_YEAR_CRYSTALLIZED extends Scenario

  val testBusinessAddress = testBizAddress

  def mockAndBasicSetup(scenario: Scenario, isAgent: Boolean = false): Unit = {
    disableAllSwitches()
    if (isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    } else {
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    }

    scenario match {
      case ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2024)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockSingleBusinessIncomeSource()
        when(mockIncomeSourceDetailsService.getLongAddressFromBusinessAddressDetails(ArgumentMatchers.eq(Option(testBizAddress))))
          .thenReturn(Some("64 Zoo Lane, Happy Place, Magical Land, England, ZL1 064, UK"))

      case FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockBusinessIncomeSourceWithLatency2024()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2023))(any, any, any))
          .thenReturn(Future.successful(Some(false)))
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2024))(any, any, any))
          .thenReturn(Future.successful(Some(false)))
        when(mockIncomeSourceDetailsService.getLongAddressFromBusinessAddressDetails(ArgumentMatchers.eq(Option(testBizAddress))))
          .thenReturn(Some("64 Zoo Lane, Happy Place, Magical Land, England, ZL1 064, UK"))

      case FIRST_AND_SECOND_YEAR_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockBusinessIncomeSourceWithLatency2024()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2023))(any, any, any))
          .thenReturn(Future.successful(Some(true)))
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2024))(any, any, any))
          .thenReturn(Future.successful(Some(true)))
        when(mockIncomeSourceDetailsService.getLongAddressFromBusinessAddressDetails(any))
          .thenReturn(Some("64 Zoo Lane, Happy Place, Magical Land, England, ZL1 064, UK"))

      case NON_ELIGIBLE_ITSA_STATUS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(false))
        mockBusinessIncomeSourceWithLatency2023AndUnknownValues()
        when(mockIncomeSourceDetailsService.getLongAddressFromBusinessAddressDetails(any))
          .thenReturn(None)
    }

    enable(IncomeSources)

  }

  "Individual - ManageSelfEmploymentController" should {
    "return 200 OK" when {
      "FS is enabled and the .show(id) method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION)

        val result: Future[Result] = TestManageSelfEmploymentController.show(testSelfEmploymentId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-1")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-2")).isDefined shouldBe false
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress
      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED)

        val result: Future[Result] = TestManageSelfEmploymentController.show(testSelfEmploymentId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("reporting-method-1").text shouldBe TestManageSelfEmploymentController.annually
        document.getElementById("reporting-method-2").text shouldBe TestManageSelfEmploymentController.quarterly
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress

      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED)

        val result: Future[Result] = TestManageSelfEmploymentController.show(testSelfEmploymentId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("reporting-method-1").text shouldBe TestManageSelfEmploymentController.annually
        document.getElementById("reporting-method-2").text shouldBe TestManageSelfEmploymentController.quarterly
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress

      }
      "FS is enabled and the .show(id) method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS)

        val result: Future[Result] = TestManageSelfEmploymentController.show(testSelfEmploymentId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-1")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-2")).isDefined shouldBe false
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-name").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-date-started").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-accounting-method").text shouldBe TestManageSelfEmploymentController.unknown

      }
    }
  }

  "Agent - ManageSelfEmploymentController" should {
    "return 200 OK" when {
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(ITSA_STATUS_MANDATORY_OR_VOLUNTARY_BUT_NO_LATENCY_INFORMATION, isAgent = true)

        val result: Future[Result] = TestManageSelfEmploymentController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-1")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-2")).isDefined shouldBe false
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress

      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, valid latency information and two tax years not crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED, isAgent = true)

        val result: Future[Result] = TestManageSelfEmploymentController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe true
        Option(document.getElementById("change-link-2")).isDefined shouldBe true
        document.getElementById("reporting-method-1").text shouldBe TestManageSelfEmploymentController.annually
        document.getElementById("reporting-method-2").text shouldBe TestManageSelfEmploymentController.quarterly
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress

      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, valid latency information and two tax years crystallised" in {
        mockAndBasicSetup(FIRST_AND_SECOND_YEAR_CRYSTALLIZED, isAgent = true)

        val result: Future[Result] = TestManageSelfEmploymentController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        document.getElementById("reporting-method-1").text shouldBe TestManageSelfEmploymentController.annually
        document.getElementById("reporting-method-2").text shouldBe TestManageSelfEmploymentController.quarterly
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.businessWithLatencyAddress

      }
      "FS is enabled and the .showAgent(id) method is called with a valid id parameter, but non eligable itsa status" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITSA_STATUS, isAgent = true)

        val result: Future[Result] = TestManageSelfEmploymentController.showAgent(testSelfEmploymentId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")).isDefined shouldBe false
        Option(document.getElementById("change-link-2")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-1")).isDefined shouldBe false
        Option(document.getElementById("reporting-method-2")).isDefined shouldBe false
        document.getElementById("business-address").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-name").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-date-started").text shouldBe TestManageSelfEmploymentController.unknown
        document.getElementById("business-accounting-method").text shouldBe TestManageSelfEmploymentController.unknown

      }
    }
  }
}
