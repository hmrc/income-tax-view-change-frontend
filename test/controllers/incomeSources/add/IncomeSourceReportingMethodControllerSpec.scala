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
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.IncomeSourceReportingMethodForm._
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.incomeSourceDetails.AddIncomeSourceData
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Assertion
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{CalculationListService, DateService, ITSAStatusService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.{testNino, testSelfEmploymentId}
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceReportingMethod

import scala.concurrent.Future

class IncomeSourceReportingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])
  val mockIncomeSourceReportingMethod: IncomeSourceReportingMethod = app.injector.instanceOf[IncomeSourceReportingMethod]
  val mockDateService: DateService = mock(classOf[DateService])

  object TestIncomeSourceReportingMethodController extends IncomeSourceReportingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    mockUpdateIncomeSourceService,
    mockITSAStatusService,
    mockDateService,
    mockCalculationListService,
    mockAuditingService,
    mockIncomeSourceReportingMethod,
    mockSessionService
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  val TAX_YEAR_2024 = 2024
  val TAX_YEAR_2023 = 2023
  val TAX_YEAR_2022 = 2022

  val formData: Map[String, String] = Map(
    newTaxYear1ReportingMethod -> "Q",
    newTaxYear2ReportingMethod -> "A",
    taxYear1 -> "2022",
    taxYear1ReportingMethod -> "A",
    taxYear2 -> "2023",
    taxYear2ReportingMethod -> "Q"
  )

  val title: String = s"${messages("htmlTitle", messages("incomeSources.add.incomeSourceReportingMethod.heading"))}"
  val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.incomeSourceReportingMethod.heading"))}"
  val heading: String = messages("incomeSources.add.incomeSourceReportingMethod.heading")
  val description1_2022: String = messages("incomeSources.add.incomeSourceReportingMethod.description1", "2022")
  val description1_2023: String = messages("incomeSources.add.incomeSourceReportingMethod.description1", "2023")
  val description1_2024: String = messages("incomeSources.add.incomeSourceReportingMethod.description1", "2024")
  val description2: String = messages("incomeSources.add.incomeSourceReportingMethod.description2")
  val description3: String = messages("incomeSources.add.incomeSourceReportingMethod.description3")
  val description4: String = messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet1") + " " +
    messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet2") + " " +
    messages("incomeSources.add.incomeSourceReportingMethod.description4.bullet3")
  val chooseReport: String = messages("incomeSources.add.incomeSourceReportingMethod.chooseReport")
  val taxYear_2021: String = messages("incomeSources.add.incomeSourceReportingMethod.taxYear", "2020", "2021")
  val taxYear_2022: String = messages("incomeSources.add.incomeSourceReportingMethod.taxYear", "2021", "2022")
  val taxYear_2023: String = messages("incomeSources.add.incomeSourceReportingMethod.taxYear", "2022", "2023")
  val taxYear_2024: String = messages("incomeSources.add.incomeSourceReportingMethod.taxYear", "2023", "2024")
  val chooseAnnualReport: String = messages("incomeSources.add.incomeSourceReportingMethod.chooseAnnualReport")
  val chooseQuarterlyReport: String = messages("incomeSources.add.incomeSourceReportingMethod.chooseQuarterlyReport")

  object Scenario extends Enumeration {
    type Scenario = Value
    val NO_LATENCY, NON_ELIGIBLE_ITSA_STATUS, ITSA_STATUS_ERROR, FIRST_YEAR_CRYSTALLISED,
    CURRENT_TAX_YEAR_IN_LATENCY_YEARS, LATENCY_PERIOD_EXPIRED, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS = Value
  }

  import Scenario._

  def mockMongoSuccess: OngoingStubbing[Future[Either[Throwable, Option[Boolean]]]] = {
    when(mockSessionService.getMongoKeyTyped[Boolean](any(), any())(any(), any())).thenReturn(
      Future(Right(None)))
  }

  def mockMongoFail: OngoingStubbing[Future[Either[Throwable, Option[Boolean]]]] = {
    when(mockSessionService.getMongoKeyTyped[Boolean](any(), any())(any(), any())).thenReturn(
      Future(Right(Some(true))))
  }

  def setupMockDateServiceCall(scenario: Scenario): OngoingStubbing[Int] = scenario match {
    case LATENCY_PERIOD_EXPIRED | CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS => when(mockDateService.getCurrentTaxYearEnd(any[Boolean])).thenReturn(TAX_YEAR_2024)
    case _ => when(mockDateService.getCurrentTaxYearEnd(any[Boolean])).thenReturn(TAX_YEAR_2023)
  }

  def setupMockIncomeSourceDetailsCall(scenario: Scenario, incomeSourceType: IncomeSourceType): Unit = (scenario, incomeSourceType) match {
    case (NO_LATENCY, UkProperty) => mockUKPropertyIncomeSource()
    case (NO_LATENCY, ForeignProperty) => mockForeignPropertyIncomeSource()
    case (NO_LATENCY, SelfEmployment) => mockSingleBusinessIncomeSource()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, UkProperty) => mockUKPropertyIncomeSourceWithLatency2024()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, ForeignProperty) => mockForeignPropertyIncomeSourceWithLatency2024()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, SelfEmployment) => mockBusinessIncomeSourceWithLatency2024()
    case (_, UkProperty) => mockUKPropertyIncomeSourceWithLatency2023()
    case (_, ForeignProperty) => mockForeignPropertyIncomeSourceWithLatency2023()
    case (_, SelfEmployment) => mockBusinessIncomeSourceWithLatency2023()
  }

  def setupMockITSAStatusCall(scenario: Scenario): OngoingStubbing[Future[Boolean]] = scenario match {
    case NON_ELIGIBLE_ITSA_STATUS =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.successful(false))
    case ITSA_STATUS_ERROR =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.failed(new Exception("[ITSAStatusService][hasEligibleITSAStatusCurrentYear] - Failed to retrieve ITSAStatus")))
    case _ =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.successful(true))
  }

  def setupMockIsTaxYearCrystallisedCall(scenario: Scenario): OngoingStubbing[Future[Option[Boolean]]] = scenario match {
    case FIRST_YEAR_CRYSTALLISED =>
      when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2022), any)(any, any, any)).thenReturn(Future(Some(true)))
    case CURRENT_TAX_YEAR_IN_LATENCY_YEARS =>
      when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2022), any)(any, any, any)).thenReturn(Future(Some(false)))
    case CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS =>
      when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2023), any)(any, any, any)).thenReturn(Future(Some(false)))
    case _ =>
      when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2023), any)(any, any, any)).thenReturn(Future(Some(false)))
  }

  def setupMockUpdateIncomeSourceCall(numberOfSuccessResponses: Int): Unit = {
    val tySpecific1 = TaxYearSpecific("2022", latencyIndicator = false)
    val tySpecific2 = TaxYearSpecific("2023", latencyIndicator = true)

    numberOfSuccessResponses match {
      case 0 =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something's broken :(")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something else is broken :(")))
      case 1 =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something's broken :(")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))
      case _ =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))
    }
  }

  def setupMockCalls(isAgent: Boolean, incomeSourceType: IncomeSourceType, scenario: Scenario): Unit = {
    disableAllSwitches()
    setupMockAuthorisationSuccess(isAgent)
    enable(IncomeSources)
    setupMockIncomeSourceDetailsCall(scenario, incomeSourceType)
    setupMockDateServiceCall(scenario)
    setupMockITSAStatusCall(scenario)
    setupMockIsTaxYearCrystallisedCall(scenario)
    mockMongoSuccess
    setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.createdIncomeSourceIdField, journeyType = JourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))
  }

  "IncomeSourceReportingMethodController.show" should {
    def checkContent(isAgent: Boolean, incomeSourceType: IncomeSourceType, scenario: Scenario): Assertion = {
      val result = if (isAgent) TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
      else TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
      val document: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe OK
      if (isAgent) document.title shouldBe titleAgent else document.title shouldBe title
      document.select("h1:nth-child(1)").text shouldBe heading
      document.getElementsByClass("govuk-body").get(1).text shouldBe description2
      document.getElementsByClass("govuk-body").get(2).text shouldBe description3
      document.select("ul").get(1).text shouldBe description4
      document.select("h1").get(1).text shouldBe chooseReport
      scenario match {
        case FIRST_YEAR_CRYSTALLISED =>
          document.getElementsByTag("legend").get(0).text shouldBe taxYear_2023
          document.getElementsByClass("govuk-body").get(0).text shouldBe description1_2023
          document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
          document.getElementsByClass("govuk-form-group").size() shouldBe 3
        case CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS =>
          document.getElementsByTag("legend").get(0).text shouldBe taxYear_2023
          document.getElementsByClass("govuk-body").get(0).text shouldBe description1_2024
          document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2023"
          document.getElementsByTag("legend").get(1).text shouldBe taxYear_2024
          document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2024"
          document.getElementsByClass("govuk-form-group").size() shouldBe 7
        case _ =>
          document.getElementsByTag("legend").get(0).text shouldBe taxYear_2022
          document.getElementsByClass("govuk-body").get(0).text shouldBe description1_2023
          document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2022"
          document.getElementsByTag("legend").get(1).text shouldBe taxYear_2023
          document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
          document.getElementsByClass("govuk-form-group").size() shouldBe 7
      }
    }

    def checkRedirect(isAgent: Boolean, incomeSourceType: IncomeSourceType, expectedRedirectUrl: String): Assertion = {
      val result = if (isAgent) TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
      else TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(expectedRedirectUrl)
    }

    def checkISE(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
      val result = if (isAgent) TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
      else TestIncomeSourceReportingMethodController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 200 OK with reporting method selection for only tax year 2" when {
      "navigating to the UK Property page in 2nd year of latency with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = false, incomeSourceType = UkProperty, FIRST_YEAR_CRYSTALLISED)
      }
      "navigating to the UK Property page in 2nd year of latency with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = true, incomeSourceType = UkProperty, FIRST_YEAR_CRYSTALLISED)
      }
      "navigating to the Foreign Property page in 2nd year of latency with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = false, incomeSourceType = ForeignProperty, FIRST_YEAR_CRYSTALLISED)
      }
      "navigating to the Foreign Property page in 2nd year of latency with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = true, incomeSourceType = ForeignProperty, FIRST_YEAR_CRYSTALLISED)
      }
      "navigating to the Self Employment page in 2nd year of latency with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = false, incomeSourceType = SelfEmployment, FIRST_YEAR_CRYSTALLISED)
      }
      "navigating to the Self Employment page in 2nd year of latency with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, FIRST_YEAR_CRYSTALLISED)
        checkContent(isAgent = true, incomeSourceType = SelfEmployment, FIRST_YEAR_CRYSTALLISED)
      }
    }


    "return 200 OK with reporting method selection for tax years 1 and 2" when {
      "navigating to the UK Property page within latency period (before 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }
      "navigating to the UK Property page within latency period (before 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }
      "navigating to the Foreign Property page within latency period (before 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }
      "navigating to the Foreign Property page within latency period (before 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }
      "navigating to the Self Employment page within latency period (before 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }
      "navigating to the Self Employment page within latency period (before 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
      }


      "navigating to the UK Property page within latency period (after 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
      "navigating to the UK Property page within latency period (after 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
      "navigating to the Foreign Property page within latency period (after 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
      "navigating to the Foreign Property page within latency period (after 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
      "navigating to the Self Employment page within latency period (after 2024) with FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
      "navigating to the Self Employment page within latency period (after 2024) with FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
        checkContent(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
      }
    }


    "return 303 SEE_OTHER and redirect to Obligations page" when {
      "navigating to the UK Property page with expired latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty).url
        checkRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the UK Property page with expired latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
        checkRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with expired latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty).url
        checkRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with expired latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
        checkRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with expired latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
        checkRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with expired latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, LATENCY_PERIOD_EXPIRED)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
        checkRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }


      "navigating to the UK Property page with no latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty).url
        checkRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the UK Property page with no latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
        checkRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with no latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty).url
        checkRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with no latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
        checkRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with no latency period and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
        checkRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with no latency period and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, NO_LATENCY)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
        checkRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }


      "navigating to the UK Property page with non-mandated/voluntary ITSA status and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty).url
        checkRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the UK Property page with non-mandated/voluntary ITSA status and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
        checkRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with non-mandated/voluntary ITSA status and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty).url
        checkRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with non-mandated/voluntary ITSA status and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
        checkRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with non-mandated/voluntary ITSA status and FS enabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
        checkRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with non-mandated/voluntary ITSA status and FS enabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, NON_ELIGIBLE_ITSA_STATUS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
        checkRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
    }


    "return 303 SEE_OTHER and redirect to home page" when {
      val expectedRedirectUrl = controllers.routes.HomeController.show().url
      val expectedAgentRedirectUrl = controllers.routes.HomeController.showAgent.url

      "navigating to the UK Property page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "navigating to the UK Property page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = true, incomeSourceType = UkProperty, expectedAgentRedirectUrl)
      }
      "navigating to the Foreign Property page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "navigating to the Foreign Property page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedAgentRedirectUrl)
      }
      "navigating to the Self Employment page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "navigating to the Self Employment page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedAgentRedirectUrl)
      }
    }

    "return 303 SEE_OTHER and redirect to the You Cannot Go Back Page" when {
      "user has already visited the obligations page - SE - Individual" in {
        setupMockCalls(isAgent = false, SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(SelfEmployment).url
        checkRedirect(false, SelfEmployment, expectedBackErrorRedirectUrl)
      }
      "user has already visited the obligations page - Uk - Individual" in {
        setupMockCalls(isAgent = false, UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(UkProperty).url
        checkRedirect(false, UkProperty, expectedBackErrorRedirectUrl)
      }
      "user has already visited the obligations page - FP - Individual" in {
        setupMockCalls(isAgent = false, ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(ForeignProperty).url
        checkRedirect(false, ForeignProperty, expectedBackErrorRedirectUrl)
      }
      "user has already visited the obligations page - SE - Agent" in {
        setupMockCalls(isAgent = true, SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(SelfEmployment).url
        checkRedirect(true, SelfEmployment, expectedBackErrorRedirectUrl)
      }
      "user has already visited the obligations page - Uk - Agent" in {
        setupMockCalls(isAgent = true, UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(UkProperty).url
        checkRedirect(true, UkProperty, expectedBackErrorRedirectUrl)
      }
      "user has already visited the obligations page - FP - Agent" in {
        setupMockCalls(isAgent = true, ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockMongoFail
        val expectedBackErrorRedirectUrl = controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(ForeignProperty).url
        checkRedirect(true, ForeignProperty, expectedBackErrorRedirectUrl)
      }
    }


    "return 500 INTERNAL_SERVER_ERROR" when {
      "navigating to the UK Property page and ITSA status returns an error - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, ITSA_STATUS_ERROR)
        checkISE(isAgent = false, incomeSourceType = UkProperty)
      }
      "navigating to the UK Property page and ITSA status returns an error - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, ITSA_STATUS_ERROR)
        checkISE(isAgent = true, incomeSourceType = UkProperty)
      }
      "navigating to the Foreign Property page and ITSA status returns an error - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, ITSA_STATUS_ERROR)
        checkISE(isAgent = false, incomeSourceType = ForeignProperty)
      }
      "navigating to the Foreign Property page and ITSA status returns an error - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, ITSA_STATUS_ERROR)
        checkISE(isAgent = true, incomeSourceType = ForeignProperty)
      }
      "navigating to the Self Employment page and ITSA status returns an error - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, ITSA_STATUS_ERROR)
        checkISE(isAgent = false, incomeSourceType = SelfEmployment)
      }
      "navigating to the Self Employment page and ITSA status returns an error - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, ITSA_STATUS_ERROR)
        checkISE(isAgent = true, incomeSourceType = SelfEmployment)
      }
    }
  }

  "IncomeSourceReportingMethodController.submit" should {
    def checkSubmitRedirect(isAgent: Boolean, incomeSourceType: IncomeSourceType, expectedRedirectUrl: String): Assertion = {
      val result = {
        if (isAgent) {
          TestIncomeSourceReportingMethodController.submit(isAgent, incomeSourceType)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody(formData.toSeq: _*))
        } else {
          TestIncomeSourceReportingMethodController.submit(isAgent, incomeSourceType)(fakeRequestWithActiveSession
            .withFormUrlEncodedBody(formData.toSeq: _*))
        }
      }

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(expectedRedirectUrl)
    }

    "return 303 SEE_OTHER and redirect to Obligations page" when {
      "completing the UK Property form and updates send successfully - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the UK Property form and updates send successfully - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and updates send successfully - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and updates send successfully - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Self Employment form and updates send successfully - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "completing the Self Employment form and updates send successfully - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(2)
        checkSubmitRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
    }

    "return 303 SEE_OTHER and redirect to Reporting Method Not Saved page" when {
      "completing the UK Property form and one update fails - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        setupMockUpdateIncomeSourceCall(1)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(UkProperty).url
        checkSubmitRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the UK Property form and one update fails - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(UkProperty).url
        setupMockUpdateIncomeSourceCall(1)
        checkSubmitRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and one update fails - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(1)
        checkSubmitRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and one update fails - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(1)
        checkSubmitRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Self Employment form and one update fails - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(1)
        checkSubmitRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "completing the Self Employment form and one update fails - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(1)
        checkSubmitRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }


      "completing the UK Property form and both updates fail - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(UkProperty).url
        val tySpecific1 = TaxYearSpecific("2022", false)
        val tySpecific2 = TaxYearSpecific("2023", true)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.eq(testNino),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.eq(testNino),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "")))

        checkSubmitRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the UK Property form and both updates fail - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(UkProperty).url
        setupMockUpdateIncomeSourceCall(0)
        checkSubmitRedirect(isAgent = true, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and both updates fail - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(0)
        checkSubmitRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Foreign Property form and both updates fail - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty).url
        setupMockUpdateIncomeSourceCall(0)
        checkSubmitRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "completing the Self Employment form and both updates fail - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(0)
        checkSubmitRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "completing the Self Employment form and both updates fail - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val expectedRedirectUrl = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(SelfEmployment).url
        setupMockUpdateIncomeSourceCall(0)
        checkSubmitRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
    }


    "return 303 SEE_OTHER and redirect to home page" when {
      val expectedRedirectUrl = controllers.routes.HomeController.show().url
      val expectedAgentRedirectUrl = controllers.routes.HomeController.showAgent.url

      "POST request to the UK Property page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = false, incomeSourceType = UkProperty, expectedRedirectUrl)
      }
      "POST request to the UK Property page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = true, incomeSourceType = UkProperty, expectedAgentRedirectUrl)
      }
      "POST request to the Foreign Property page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = false, incomeSourceType = ForeignProperty, expectedRedirectUrl)
      }
      "POST request to the Foreign Property page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = true, incomeSourceType = ForeignProperty, expectedAgentRedirectUrl)
      }
      "POST to request the Self Employment page with FS disabled - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = false, incomeSourceType = SelfEmployment, expectedRedirectUrl)
      }
      "POST to request the Self Employment page with FS disabled - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        checkSubmitRedirect(isAgent = true, incomeSourceType = SelfEmployment, expectedAgentRedirectUrl)
      }
    }


    "return 400 BAD_REQUEST" when {
      def checkBadRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        val invalidFormData: Map[String, String] = Map(
          newTaxYear1ReportingMethod -> "",
          newTaxYear2ReportingMethod -> "",
          taxYear1 -> "2022",
          taxYear1ReportingMethod -> "A",
          taxYear2 -> "2023",
          taxYear2ReportingMethod -> "Q"
        )

        val result = {
          if (isAgent) {
            TestIncomeSourceReportingMethodController.submit(isAgent, incomeSourceType)(fakeRequestConfirmedClient()
              .withFormUrlEncodedBody(invalidFormData.toSeq: _*))
          } else {
            TestIncomeSourceReportingMethodController.submit(isAgent, incomeSourceType)(fakeRequestWithActiveSession
              .withFormUrlEncodedBody(invalidFormData.toSeq: _*))
          }
        }
        status(result) shouldBe BAD_REQUEST
      }

      "invalid form input on the UK Property page - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = false, incomeSourceType = UkProperty)
      }
      "invalid form input on the UK Property page - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = UkProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = true, incomeSourceType = UkProperty)
      }
      "invalid form input on the Foreign Property page - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = false, incomeSourceType = ForeignProperty)
      }
      "invalid form input on the Foreign Property page - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = ForeignProperty, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = true, incomeSourceType = ForeignProperty)
      }
      "invalid form input on the Self Employment page - Individual" in {
        setupMockCalls(isAgent = false, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = false, incomeSourceType = SelfEmployment)
      }
      "invalid form input on the Self Employment page - Agent" in {
        setupMockCalls(isAgent = true, incomeSourceType = SelfEmployment, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        checkBadRequest(isAgent = true, incomeSourceType = SelfEmployment)
      }
    }
  }
}
