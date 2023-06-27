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
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.BusinessReportingMethodService
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessReportingMethod

import scala.concurrent.Future

class BusinessReportingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockBusinessReportingMethod: BusinessReportingMethod = app.injector.instanceOf[BusinessReportingMethod]
  val mockBusinessReportingMethodService: BusinessReportingMethodService = mock(classOf[BusinessReportingMethodService])

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
    val description1: String = messages("incomeSources.add.businessReportingMethod.description1", "2023")
    val description2: String = messages("incomeSources.add.businessReportingMethod.description2")
    val description3: String = messages("incomeSources.add.businessReportingMethod.description3")
    val description4: String = messages("incomeSources.add.businessReportingMethod.description4")
    val chooseReport: String = messages("incomeSources.add.businessReportingMethod.chooseReport")
    val taxYear1: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2021", "2022")
    val taxYear2: String = messages("incomeSources.add.businessReportingMethod.taxYear", "2022", "2023")
    val chooseAnnualReport: String = messages("incomeSources.add.businessReportingMethod.chooseAnnualReport")
    val chooseQuarterlyReport: String = messages("incomeSources.add.businessReportingMethod.chooseQuarterlyReport")
    val error: String = messages("incomeSources.add.businessReportingMethod.error")
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "Individual - BusinessReportingMethodController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disableAllSwitches()
        mockBusinessIncomeSource()

        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestBusinessReportingMethodController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "New page - select reporting method with TY1 & TY2" when {
      "registering business within Tax Year 1" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n","") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1
        document.getElementsByClass("govuk-form-group").get(1).hasText shouldBe true
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2
        document.getElementsByClass("govuk-form-group").get(3).hasText shouldBe true
        document.getElementsByClass("govuk-form-group").size() shouldBe 4
      }

      "registering business within Tax Year 2, Tax Year 1 is crystallised" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(None, Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear2
        document.getElementsByClass("govuk-form-group").get(1).hasText shouldBe true
        document.getElementsByClass("govuk-form-group").size() shouldBe 2
      }

      "registering business within Tax Year 2, Tax Year 1 NOT crystallised" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1
        document.getElementsByClass("govuk-form-group").get(1).hasText shouldBe true
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2
        document.getElementsByClass("govuk-form-group").get(3).hasText shouldBe true
      }

      "registering business in Tax Year 3 and beyond (latency expired)" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("h1:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
        document.getElementsByClass("govuk-body").get(0).text shouldBe TestBusinessReportingMethodController.description1
        document.getElementsByClass("govuk-body").get(1).text shouldBe TestBusinessReportingMethodController.description2
        document.getElementsByClass("govuk-body").get(2).text shouldBe TestBusinessReportingMethodController.description3
        document.select("ul").get(1).select("li").toString.replaceAll("\n", "") shouldBe TestBusinessReportingMethodController.description4
        document.select("h1").get(1).text shouldBe TestBusinessReportingMethodController.chooseReport
        document.getElementsByClass("govuk-body").get(3).text shouldBe TestBusinessReportingMethodController.taxYear1
        document.getElementsByClass("govuk-form-group").get(1).hasText shouldBe true
        document.getElementsByClass("govuk-body").get(4).text shouldBe TestBusinessReportingMethodController.taxYear2
        document.getElementsByClass("govuk-form-group").get(3).hasText shouldBe true
      }
    }
  }

}
