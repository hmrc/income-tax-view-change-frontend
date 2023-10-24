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
import enums.IncomeSourceJourney.ForeignProperty
import forms.incomeSources.add.AddForeignPropertyReportingMethodForm
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.incomeSourceDetails.viewmodels.ForeignPropertyReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{CalculationListService, DateService, ITSAStatusService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyReportingMethod

import scala.concurrent.Future

class ForeignPropertyReportingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockIncomeTaxViewChangeConnector {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockForeignPropertyReportingMethod: ForeignPropertyReportingMethod = app.injector.instanceOf[ForeignPropertyReportingMethod]
  val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])
  val mockDateService: DateService = mock(classOf[DateService])

  val newTaxYear1ReportingMethod = "new_tax_year_1_reporting_method"
  val newTaxYear2ReportingMethod = "new_tax_year_2_reporting_method"
  val taxYear1 = s"${newTaxYear1ReportingMethod}_tax_year"
  val taxYear2 = s"${newTaxYear2ReportingMethod}_tax_year"
  val taxYear1ReportingMethod = "tax_year_1_reporting_method"
  val taxYear2ReportingMethod = "tax_year_2_reporting_method"

  object TestForeignPropertyReportingMethodController extends ForeignPropertyReportingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockForeignPropertyReportingMethod,
    mockUpdateIncomeSourceService,
    mockITSAStatusService,
    mockDateService,
    mockCalculationListService,
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.foreignPropertyReportingMethod.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.foreignPropertyReportingMethod.heading"))}"
    val heading: String = messages("incomeSources.add.foreignPropertyReportingMethod.heading")
    val description1_TY1: String = messages("incomeSources.add.foreignPropertyReportingMethod.description1", "2023")
    val description1_TY2: String = messages("incomeSources.add.foreignPropertyReportingMethod.description1", "2022")
    val description2: String = messages("incomeSources.add.foreignPropertyReportingMethod.description2")
    val description3: String = messages("incomeSources.add.foreignPropertyReportingMethod.description3")
    val description4: String = messages("incomeSources.add.foreignPropertyReportingMethod.description4")
    val chooseReport: String = messages("incomeSources.add.foreignPropertyReportingMethod.chooseReport")
    val taxYear1_TY1: String = messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", "2021", "2022")
    val taxYear2_TY1: String = messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", "2022", "2023")
    val taxYear1_TY2: String = messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", "2020", "2021")
    val taxYear2_TY2: String = messages("incomeSources.add.foreignPropertyReportingMethod.taxYear", "2021", "2022")
    val chooseAnnualReport: String = messages("incomeSources.add.foreignPropertyReportingMethod.chooseAnnualReport")
    val chooseQuarterlyReport: String = messages("incomeSources.add.foreignPropertyReportingMethod.chooseQuarterlyReport")
    val error: String = messages("incomeSources.add.foreignPropertyReportingMethod.error")
    val incomeSourceId: String = "XA00001234"
    val updateForm: AddForeignPropertyReportingMethodForm = AddForeignPropertyReportingMethodForm(Some("Q"), Some("A"), Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val updateFormMap: Map[String, String] = updateForm.toFormMap.map(x => x._1 -> x._2.get)
    val unchangedUpdateForm: AddForeignPropertyReportingMethodForm = AddForeignPropertyReportingMethodForm(Some("A"), Some("Q"), Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val unchangedUpdateFormMap: Map[String, String] = unchangedUpdateForm.toFormMap.map(x => x._1 -> x._2.get)
    val inTaxYear1: ForeignPropertyReportingMethodViewModel = ForeignPropertyReportingMethodViewModel(Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val inTaxYear2_TaxYear1Crystallised: ForeignPropertyReportingMethodViewModel = ForeignPropertyReportingMethodViewModel(None, None, Some("2022"), None)
    val inTaxYear2_TaxYear1NotCrystallised: ForeignPropertyReportingMethodViewModel = ForeignPropertyReportingMethodViewModel(Some("2021"), None, Some("2022"), None)
    val inTaxYear3_Expired: ForeignPropertyReportingMethodViewModel = ForeignPropertyReportingMethodViewModel(None)
    val radioMustBeSelectedError_TY1: String = messages("incomeSources.add.foreignPropertyReportingMethod.error", "2021", "2022")
    val radioMustBeSelectedError_TY2: String = messages("incomeSources.add.foreignPropertyReportingMethod.error", "2022", "2023")
    val testNino: String = "AB123456C"
    val testAgentNino: String = "AA111111A"
    val isAgent: Boolean = true
  }

  object Scenario extends Enumeration {
    type Scenario = Value
    val NO_LATENCY, NON_ELIGIBLE_ITS_STATUS, FIRST_YEAR_CRYSTALLIZED,
    CURRENT_TAX_YEAR_IN_LATENCY_YEARS, LATENCY_PERIOD_EXPIRED, CURRENT_TAX_2024_YEAR_IN_LATENCY_YEARS = Value
  }

  import Scenario._

  def mockAndBasicSetup(scenario: Scenario, isAgent: Boolean = false): Unit = {
    disableAllSwitches()
    if (isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    } else {
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    }

    scenario match {
      case NO_LATENCY =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockForeignPropertyIncomeSource()

      case LATENCY_PERIOD_EXPIRED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2024)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockForeignPropertyIncomeSourceWithLatency2023()

      case FIRST_YEAR_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockForeignPropertyIncomeSourceWithLatency2023()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2022), any)(any, any, any)).thenReturn(Future(Some(true)))

      case CURRENT_TAX_YEAR_IN_LATENCY_YEARS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2022), any)(any, any, any)).thenReturn(Future(Some(false)))
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockForeignPropertyIncomeSourceWithLatency2023()

      case CURRENT_TAX_2024_YEAR_IN_LATENCY_YEARS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2024)
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockForeignPropertyIncomeSourceWithLatency2024()
        when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.eq(2023), any)(any, any, any)).thenReturn(Future(Some(false)))

      case NON_ELIGIBLE_ITS_STATUS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        mockForeignPropertyIncomeSourceWithLatency2023()
        when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(false))

      case _ =>
    }


    enable(IncomeSources)
  }

  "Individual - ForeignPropertyReportingMethodController" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no back button" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.title
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.hasClass("govuk-back-link") shouldBe false
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        disable(IncomeSources)
        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "return 303 SEE_OTHER and redirect to foreign property added page" when {
      "registering foreign property in Tax Year 3 and beyond (latency expired)" in {
        mockAndBasicSetup(LATENCY_PERIOD_EXPIRED)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.show(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)

      }

      "ITSA Status returned is not MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITS_STATUS)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
      }
    }

    "Show select reporting method with TY1 & TY2" when {
      "registering foreign property in Latency Years" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.title
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestForeignPropertyReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestForeignPropertyReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestForeignPropertyReportingMethodController.description3
        document.getElementById("bullet-1").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
        document.getElementById("bullet-2").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
        document.getElementById("bullet-3").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")
        document.select("h1").get(1).text shouldBe TestForeignPropertyReportingMethodController.chooseReport
        document.getElementsByTag("legend").get(0).text shouldBe TestForeignPropertyReportingMethodController.taxYear1_TY1
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByTag("legend").get(1).text shouldBe TestForeignPropertyReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 7
      }
    }

    "Show select reporting method with only TY2" when {
      "registering foreign property within Tax Year 2, Tax Year 1 is crystallised" in {
        mockAndBasicSetup(FIRST_YEAR_CRYSTALLIZED)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.show(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.title
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestForeignPropertyReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestForeignPropertyReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestForeignPropertyReportingMethodController.description3
        document.getElementById("bullet-1").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
        document.getElementById("bullet-2").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
        document.getElementById("bullet-3").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")
        document.select("h1").get(1).text shouldBe TestForeignPropertyReportingMethodController.chooseReport
        document.getElementsByTag("legend").get(0).text shouldBe TestForeignPropertyReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 3
      }
    }

    "display error message" when {
      "Tax Year 2 reporting method is not selected (Tax Year 1 Crystallised)" in {
        mockAndBasicSetup(FIRST_YEAR_CRYSTALLIZED)
        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "Tax Year 1 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "none",
            newTaxYear2ReportingMethod -> "A"
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_2_reporting_method-error") shouldBe null
      }

      "Tax Year 2 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "Tax Year 1 & Tax Year 2 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "none",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
      }
    }

    "Update success and redirect to foreign property added page" when {
      "all mandatory fields are selected" in {
        val tySpecific1 = TaxYearSpecific("2022", latencyIndicator = false)
        val tySpecific2 = TaxYearSpecific("2023", latencyIndicator = true)
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        mockBothIncomeSources()
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseModel("")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseModel("")))


        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.show(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }

    "Update not required and redirect to foreign property added page" when {
      "all mandatory fields are selected and values unchanged" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)

        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.show(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }

    "Update failed and error page shown" when {
      "some internal failure in the update action (both calls)" in {
        val tySpecific1 = TaxYearSpecific("2022", false)
        val tySpecific2 = TaxYearSpecific("2023", true)
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
      "some internal failure in the update action (one call)" in {
        val tySpecific1 = TaxYearSpecific("2022", false)
        val tySpecific2 = TaxYearSpecific("2023", true)
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseModel("")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        val result = TestForeignPropertyReportingMethodController.submit(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }
  }

  "Agent - ForeignPropertyReportingMethodController" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no back button" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.titleAgent
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.hasClass("govuk-back-link") shouldBe false
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        disable(IncomeSources)
        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "return 303 SEE_OTHER and redirect to foreign property added page" when {
      "registering foreign property in Tax Year 3 and beyond (latency expired)" in {
        mockAndBasicSetup(LATENCY_PERIOD_EXPIRED, isAgent = true)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)

      }

      "ITSA Status returned is not MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(NON_ELIGIBLE_ITS_STATUS, isAgent = true)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
      }
    }

    "Show select reporting method with TY1 & TY2" when {
      "registering foreign property in Latency Years" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.titleAgent
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestForeignPropertyReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestForeignPropertyReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestForeignPropertyReportingMethodController.description3
        document.getElementById("bullet-1").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
        document.getElementById("bullet-2").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
        document.getElementById("bullet-3").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")
        document.select("h1").get(1).text shouldBe TestForeignPropertyReportingMethodController.chooseReport
        document.getElementsByTag("legend").get(0).text shouldBe TestForeignPropertyReportingMethodController.taxYear1_TY1
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByTag("legend").get(1).text shouldBe TestForeignPropertyReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 7
      }
    }

    "Show select reporting method with only TY2" when {
      "registering foreign property within Tax Year 2, Tax Year 1 is crystallised" in {
        mockAndBasicSetup(FIRST_YEAR_CRYSTALLIZED, isAgent = true)

        val result: Future[Result] = TestForeignPropertyReportingMethodController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestForeignPropertyReportingMethodController.titleAgent
        document.getElementsByClass("govuk-heading-xl").text shouldBe TestForeignPropertyReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestForeignPropertyReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestForeignPropertyReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestForeignPropertyReportingMethodController.description3
        document.getElementById("bullet-1").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet1")
        document.getElementById("bullet-2").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet2")
        document.getElementById("bullet-3").text() shouldBe messages("incomeSources.add.foreignPropertyReportingMethod.description4.bullet3")
        document.select("h1").get(1).text shouldBe TestForeignPropertyReportingMethodController.chooseReport
        document.getElementsByTag("legend").get(0).text shouldBe TestForeignPropertyReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 3
      }
    }

    "display error message" when {
      "Tax Year 2 reporting method is not selected (Tax Year 1 Crystallised)" in {
        mockAndBasicSetup(FIRST_YEAR_CRYSTALLIZED, isAgent = true)
        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "Tax Year 1 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "none",
            newTaxYear2ReportingMethod -> "A"
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_2_reporting_method-error") shouldBe null
      }

      "Tax Year 2 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "Tax Year 1 & Tax Year 2 reporting method is not selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "none",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestForeignPropertyReportingMethodController.radioMustBeSelectedError_TY2
      }
    }

    "Update success and redirect to foreign property added page" when {
      "all mandatory fields are selected" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          any()
        )(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseModel("")))


        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient(TestForeignPropertyReportingMethodController.testNino).withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }

    "Update not required and redirect to foreign property added page" when {
      "all mandatory fields are selected and values unchanged" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)

        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceAddedController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }

    "Update failed and error page shown" when {
      "some internal failure in the update action (both calls)" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, isAgent = true)

        val tySpecific1 = TaxYearSpecific("2022", false)
        val tySpecific2 = TaxYearSpecific("2023", true)
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient(TestForeignPropertyReportingMethodController.testNino).withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
      "some internal failure in the update action (one call)" in {
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS, true)

        val tySpecific1 = TaxYearSpecific("2022", false)
        val tySpecific2 = TaxYearSpecific("2023", true)
        mockAndBasicSetup(CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific1))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          any(),
          any(),
          ArgumentMatchers.eq(tySpecific2))(any, any)).thenReturn(Future.successful(UpdateIncomeSourceResponseModel("")))

        val result = TestForeignPropertyReportingMethodController.submitAgent(TestForeignPropertyReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient(TestForeignPropertyReportingMethodController.testNino).withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(TestForeignPropertyReportingMethodController.incomeSourceId, ForeignProperty).url)
      }
    }
  }
}