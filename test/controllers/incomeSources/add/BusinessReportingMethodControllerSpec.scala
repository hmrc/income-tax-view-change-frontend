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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.add.AddBusinessReportingMethodForm
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.BusinessReportingMethodService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessReportingMethod

import scala.concurrent.Future

class BusinessReportingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockIncomeTaxViewChangeConnector{

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockBusinessReportingMethod: BusinessReportingMethod = app.injector.instanceOf[BusinessReportingMethod]
  val mockBusinessReportingMethodService: BusinessReportingMethodService = mock(classOf[BusinessReportingMethodService])

  val newTaxYear1ReportingMethod = "new_tax_year_1_reporting_method"
  val newTaxYear2ReportingMethod = "new_tax_year_2_reporting_method"
  val taxYear1 = s"${newTaxYear1ReportingMethod}_tax_year"
  val taxYear2 = s"${newTaxYear2ReportingMethod}_tax_year"
  val taxYear1ReportingMethod = "tax_year_1_reporting_method"
  val taxYear2ReportingMethod = "tax_year_2_reporting_method"

  object TestBusinessReportingMethodController extends BusinessReportingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockBusinessReportingMethod,
    mockBusinessReportingMethodService,
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.businessReportingMethod.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.businessReportingMethod.heading"))}"
    val heading: String = messages("incomeSources.add.businessReportingMethod.heading")
    val description1_TY1: String = messages("incomeSources.add.businessReportingMethod.description1", "2023")
    val description1_TY2: String = messages("incomeSources.add.businessReportingMethod.description1", "2022")
    val description2: String = messages("incomeSources.add.businessReportingMethod.description2")
    val description3: String = messages("incomeSources.add.businessReportingMethod.description3")
    val description4: String = messages("incomeSources.add.businessReportingMethod.description4")
    val chooseReport: String = messages("incomeSources.add.businessReportingMethod.chooseReport")
    val taxYear1_TY1: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2021", "2022")
    val taxYear2_TY1: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2022", "2023")
    val taxYear1_TY2: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2020", "2021")
    val taxYear2_TY2: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2021", "2022")
    val chooseAnnualReport: String = messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
    val chooseQuarterlyReport: String = messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
    val error: String = messages("incomeSources.add.businessReportingMethod.error")
    val incomeSourceId: String = "XA00001234"
    val updateForm: AddBusinessReportingMethodForm = AddBusinessReportingMethodForm(Some("Q"), Some("A"), Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val unchangedUpdateForm: AddBusinessReportingMethodForm = AddBusinessReportingMethodForm(Some("A"), Some("Q"), Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val inTaxYear1: BusinessReportingMethodViewModel  =  BusinessReportingMethodViewModel(Some("2022"), Some("A"), Some("2023"), Some("Q"))
    val inTaxYear2_TaxYear1Crystallised: BusinessReportingMethodViewModel  =  BusinessReportingMethodViewModel(None, None, Some("2022"), None)
    val inTaxYear2_TaxYear1NotCrystallised: BusinessReportingMethodViewModel  =  BusinessReportingMethodViewModel(Some("2021"), None, Some("2022"), None)
    val inTaxYear3_Expired: BusinessReportingMethodViewModel  =  BusinessReportingMethodViewModel(None)
    val radioMustBeSelectedError_TY1 = messages("incomeSources.add.businessReportingMethod.error", "2021", "2022")
    val radioMustBeSelectedError_TY2 = messages("incomeSources.add.businessReportingMethod.error", "2022", "2023")
    val testNino: String = "AB123456C"
    val testAgentNino: String = "AA111111A"
    val isAgent: Boolean = true
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  def mockAndBasicSetup(checkITSAStatusCurrentYear: Boolean, businessReportingMethodViewModel: BusinessReportingMethodViewModel, isAgent: Boolean = false): Unit = {
    disableAllSwitches()
    if(isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    } else {
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    }

    mockBusinessIncomeSource()
    setupBusinessDetails(TestBusinessReportingMethodController.testNino)(Future.successful(singleBusinessIncome))
    when(mockBusinessReportingMethodService.checkITSAStatusCurrentYear(any, any, any)).thenReturn(Future.successful(checkITSAStatusCurrentYear))
    if(businessReportingMethodViewModel.taxYear1.isDefined || businessReportingMethodViewModel.taxYear2.isDefined) {
      when(mockBusinessReportingMethodService
        .getBusinessReportingMethodDetails(ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId))(any, any, any))
        .thenReturn(Future.successful(Some(businessReportingMethodViewModel)))
    } else {
      when(mockBusinessReportingMethodService
        .getBusinessReportingMethodDetails(ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId))(any, any, any))
        .thenReturn(Future.successful(None))
    }
    enable(IncomeSources)
  }

  "Individual - BusinessReportingMethodController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no back button" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)

        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.hasClass("govuk-back-link") shouldBe false
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)
        disable(IncomeSources)
        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val expectedContent: String = TestBusinessReportingMethodController.customNotFoundErrorView().toString()

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Show select reporting method with TY1 & TY2" when {
      "registering business within Tax Year 1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)
        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n","") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1_TY1
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 6
      }

      "registering business within Tax Year 2, Tax Year 1 NOT crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised)
        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY2
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1_TY2
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2021"
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2_TY2
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-form-group").size() shouldBe 6
      }

      "Customer statuses returned other than MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)

        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
      }
    }

    "Show select reporting method with only TY2" when {
      "registering business within Tax Year 2, Tax Year 1 is crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1Crystallised)

        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY2
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear2_TY2
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-form-group").size() shouldBe 3
      }
    }

    "return 303 SEE_OTHER and redirect to business added page" when {
      "registering business in Tax Year 3 and beyond (latency expired)" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear3_Expired)

        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.show().url)

      }

      "Customer statuses returned is either MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(false, TestBusinessReportingMethodController.inTaxYear1)

        val result: Future[Result] = TestBusinessReportingMethodController.show(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.show().url)
      }
    }

    "display error message" when {
      "Both TY1 & TY2 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: "+TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: "+TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
      }

      "TY1 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> "A"
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_2_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY2 and TY1 Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1Crystallised)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY2 and TY1 not Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY1 & TY2 reporting method not selected for within TY2 and TY1 not Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised)
        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
      }
    }

    "Update success and redirect to business added page" when {
      "all mandatory fields are selected" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.updateForm))(any, any))
          .thenReturn(Future.successful(Some(UpdateIncomeSourceResponseModel(""))))

        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.show().url)
      }
    }

    "Update not required and redirect to business added page" when {
      "all mandatory fields are selected and values unchanged" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.unchangedUpdateForm))(any, any))
          .thenReturn(Future.successful(None))

        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.show().url)
      }
    }

    "Update failed and error page shown" when {
      "some internal failure in the update action" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.unchangedUpdateForm))(any, any))
          .thenReturn(Future.successful(Some(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, ""))))

        val result = TestBusinessReportingMethodController.submit(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestWithActiveSession.withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None
      }
    }
  }

  "Agent - BusinessReportingMethodController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled and no back button" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.hasClass("govuk-back-link") shouldBe false
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)
        disable(IncomeSources)
        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val expectedContent: String = TestBusinessReportingMethodController.customNotFoundErrorView().toString()

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "Show select reporting method with TY1 & TY2" when {
      "registering business within Tax Year 1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)
        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1_TY1
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2_TY1
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2023"
        document.getElementsByClass("govuk-form-group").size() shouldBe 6
      }

      "registering business within Tax Year 2, Tax Year 1 NOT crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised, TestBusinessReportingMethodController.isAgent)
        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY2
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1_TY2
        document.getElementById("new_tax_year_1_reporting_method_tax_year").`val`() shouldBe "2021"
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2_TY2
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-form-group").size() shouldBe 6
      }

      "Customer statuses returned other than MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
      }
    }

    "Show select reporting method with only TY2" when {
      "registering business within Tax Year 2, Tax Year 1 is crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1Crystallised, TestBusinessReportingMethodController.isAgent)

        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.titleAgent
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1_TY2
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear2_TY2
        document.getElementById("new_tax_year_2_reporting_method_tax_year").`val`() shouldBe "2022"
        document.getElementsByClass("govuk-form-group").size() shouldBe 3
      }
    }

    "return 303 SEE_OTHER and redirect to business added page" when {
      "registering business in Tax Year 3 and beyond (latency expired)" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear3_Expired, TestBusinessReportingMethodController.isAgent)

        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.showAgent().url)

      }

      "Customer statuses returned is either MTD Mandated or MTD Voluntary" in {
        mockAndBasicSetup(false, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        val result: Future[Result] = TestBusinessReportingMethodController.showAgent(TestBusinessReportingMethodController.incomeSourceId)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.showAgent().url)
      }
    }

    "display error message" when {
      "Both TY1 & TY2 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
      }

      "TY1 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> "A"
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_2_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY1" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY2 and TY1 Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1Crystallised, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY2 reporting method not selected for within TY2 and TY1 not Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.select("a[href='#new_tax_year_1_reporting_method']").size shouldBe 0
        document.getElementById("new_tax_year_1_reporting_method-error") shouldBe null
      }

      "TY1 & TY2 reporting method not selected for within TY2 and TY1 not Crystallised" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear2_TaxYear1NotCrystallised, TestBusinessReportingMethodController.isAgent)
        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            taxYear1 -> "2022",
            taxYear2 -> "2023",
            newTaxYear1ReportingMethod -> "",
            newTaxYear2ReportingMethod -> ""
          ))

        status(result) shouldBe BAD_REQUEST
        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("a[href='#new_tax_year_1_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.getElementById("new_tax_year_1_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY1
        document.select("a[href='#new_tax_year_2_reporting_method']").text shouldBe TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
        document.getElementById("new_tax_year_2_reporting_method-error").text shouldBe "Error: " + TestBusinessReportingMethodController.radioMustBeSelectedError_TY2
      }
    }

    "Update success and redirect to business added page" when {
      "all mandatory fields are selected" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testAgentNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.updateForm))(any, any))
          .thenReturn(Future.successful(Some(UpdateIncomeSourceResponseModel(""))))

        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "Q",
            newTaxYear2ReportingMethod -> "A",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.showAgent().url)
      }
    }

    "Update not required and redirect to business added page" when {
      "all mandatory fields are selected and values unchanged" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testAgentNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.unchangedUpdateForm))(any, any))
          .thenReturn(Future.successful(None))

        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BusinessAddedController.showAgent().url)
      }
    }

    "Update failed and error page shown" when {
      "some internal failure in the update action" in {
        mockAndBasicSetup(true, TestBusinessReportingMethodController.inTaxYear1, TestBusinessReportingMethodController.isAgent)

        when(mockBusinessReportingMethodService
          .updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(TestBusinessReportingMethodController.testAgentNino), ArgumentMatchers.eq(TestBusinessReportingMethodController.incomeSourceId), ArgumentMatchers.eq(TestBusinessReportingMethodController.unchangedUpdateForm))(any, any))
          .thenReturn(Future.successful(Some(UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, ""))))

        val result = TestBusinessReportingMethodController.submitAgent(TestBusinessReportingMethodController.incomeSourceId)(
          fakeRequestConfirmedClient().withFormUrlEncodedBody(
            newTaxYear1ReportingMethod -> "A",
            newTaxYear2ReportingMethod -> "Q",
            taxYear1 -> "2022",
            taxYear1ReportingMethod -> "A",
            taxYear2 -> "2023",
            taxYear2ReportingMethod -> "Q"
          ))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None
      }
    }
  }
}
