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

package views.incomeSources.manage

import testUtils.TestSupport
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.viewmodels.{BusinessReportingMethodViewModel, ViewBusinessDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.BusinessDetailsTestConstants.{testLatencyDetailsViewModel1, testStartDate, testTradeName}
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageSelfEmployment

class ManageSelfEmploymentViewSpec extends TestSupport {

  val manageSelfEmploymentView: ManageSelfEmployment = app.injector.instanceOf[ManageSelfEmployment]

  val unknown = messages("incomeSources.generic.unknown")
  val heading = messages("incomeSources.manage.business-manage-details.heading")
  val soleTrader = messages("incomeSources.manage.business-manage-details.sole-trader-section")
  val businessName = messages("incomeSources.manage.business-manage-details.business-name")
  val businessAddress = messages("incomeSources.manage.business-manage-details.business-address")
  val dateStarted = messages("incomeSources.manage.business-manage-details.date-started")
  val accountingMethod = messages("incomeSources.manage.business-manage-details.accounting-method")
  val reportingMethod1 = messages("incomeSources.manage.business-manage-details.reporting-method", "2023", "2024")
  val reportingMethod2 = messages("incomeSources.manage.business-manage-details.reporting-method", "2024", "2025")
  val change = messages("incomeSources.manage.business-manage-details.change")
  val quarterly = messages("incomeSources.manage.business-manage-details.quarterly")
  val annually = messages("incomeSources.manage.business-manage-details.annually")
  val cash = messages("incomeSources.manage.business-manage-details.cash-accounting")
  val traditional = messages("incomeSources.manage.business-manage-details.traditional-accounting")
  val expectedViewModelAddressString1: Option[String] = Some("Line 1<br>Line 2<br>Line 3<br>Line 4<br>LN1 1NL<br>NI")
  val expectedViewAddressString1: String = "Line 1 Line 2 Line 3 Line 4 LN1 1NL NI"
  val expectedBusinessName: String = "nextUpdates.business"
  val expectedBusinessStartDate: String = "1 January 2022"
  //  val changeLinkHref1: String = "/report-quarterly/income-and-expenses/view/income-sources/manage/confirm-you-want-to-switch?id=XA00001234&taxYear=2023&changeTo=Q"
  //  val changeLinkHref2: String = "/report-quarterly/income-and-expenses/view/income-sources/manage/confirm-you-want-to-switch?id=XA00001234&taxYear=2024&changeTo=A"
  val changeLinkAgentHref1: String = "/report-quarterly/income-and-expenses/view/agents/income-sources/manage/confirm-you-want-to-switch?id=XA00001234&taxYear=2023&changeTo=Q"
  val changeLinkAgentHref2: String = "/report-quarterly/income-and-expenses/view/agents/income-sources/manage/confirm-you-want-to-switch?id=XA00001234&taxYear=2024&changeTo=A"

  val viewModel: ViewBusinessDetailsViewModel = ViewBusinessDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    address = expectedViewModelAddressString1,
    businessAccountingMethod = Some("cash"),
    itsaHasMandatedOrVoluntaryStatusCurrentYear = Some(true),
    taxYearOneCrystallised = Some(false),
    taxYearTwoCrystallised = Some(false),
    latencyDetails = Some(testLatencyDetailsViewModel1)
  )

  val viewModel2: ViewBusinessDetailsViewModel = ViewBusinessDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    businessAccountingMethod = None,
    itsaHasMandatedOrVoluntaryStatusCurrentYear = Some(false),
    taxYearOneCrystallised = None,
    taxYearTwoCrystallised = None,
    latencyDetails = None
  )


  class Setup(isAgent: Boolean, error: Boolean = false) {
    val backUrl: String = if (isAgent) {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url
    } else {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url
    }

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      if (isAgent) {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.showAgent(id, taxYear, changeTo: String).url
      } else {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.show(id, taxYear, changeTo: String).url
      }
    }

    lazy val view: HtmlFormat.Appendable = {
      manageSelfEmploymentView(
        viewModel,
        isAgent,
        backUrl
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))

  }

  class Setup2(isAgent: Boolean, error: Boolean = false) {
    val backUrl: String = if (isAgent) {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url
    } else {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url
    }

    def changeReportingMethodUrl2(id: String, taxYear: String, changeTo: String): String = {
      if (isAgent) {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.showAgent(id, taxYear, changeTo: String).url
      } else {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.show(id, taxYear, changeTo: String).url
      }
    }

    lazy val view: HtmlFormat.Appendable = {
      manageSelfEmploymentView(
        viewModel2,
        isAgent,
        backUrl
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))

  }

  "ManageSelfEmployment - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new Setup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl
    }
    "render the whole page" in new Setup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe accountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(5).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023", changeTo = "Q")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2024", changeTo = "A")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-address").text shouldBe expectedViewAddressString1
      document.getElementById("business-name").text shouldBe expectedBusinessName
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash

    }
    "render the whole page with unknowns and no change links" in new Setup2(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe accountingMethod

      document.getElementById("business-address").text shouldBe unknown
      document.getElementById("business-name").text shouldBe unknown
      document.getElementById("business-date-started").text shouldBe unknown
      document.getElementById("business-accounting-method").text shouldBe unknown
    }
  }
  "ManageSelfEmployment - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new Setup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl
    }
    "render the whole page" in new Setup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe accountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(5).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023", changeTo = "Q")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2024", changeTo = "A")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-address").text shouldBe expectedViewAddressString1
      document.getElementById("business-name").text shouldBe expectedBusinessName
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash

    }
  }

}
