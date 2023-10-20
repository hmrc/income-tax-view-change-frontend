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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.core.AddressModel
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.BusinessDetailsTestConstants.{testLatencyDetails3, testStartDate, testTradeName}
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageIncomeSourceDetails

class ManageIncomeSourceDetailsViewSpec extends TestSupport {

  val manageIncomeSourceDetailsView: ManageIncomeSourceDetails = app.injector.instanceOf[ManageIncomeSourceDetails]

  val unknown = messages("incomeSources.generic.unknown")
  val heading = messages("incomeSources.manage.business-manage-details.heading")
  val soleTrader = messages("incomeSources.manage.business-manage-details.sole-trader-section")
  val businessName = messages("incomeSources.manage.business-manage-details.business-name")
  val businessAddress = messages("incomeSources.manage.business-manage-details.business-address")
  val dateStarted = messages("incomeSources.manage.business-manage-details.date-started")
  val businessAccountingMethod = messages("incomeSources.manage.business-manage-details.accounting-method")
  val ukAccountingMethod = messages("incomeSources.manage.uk-property-manage-details.accounting-method")
  val foreignAccountingMethod = messages("incomeSources.manage.foreign-property-manage-details.accounting-method")
  val reportingMethod1 = messages("incomeSources.manage.business-manage-details.reporting-method", "2023", "2024")
  val reportingMethod2 = messages("incomeSources.manage.business-manage-details.reporting-method", "2024", "2025")
  val change = messages("incomeSources.manage.business-manage-details.change")
  val quarterly = messages("incomeSources.manage.business-manage-details.quarterly")
  val annually = messages("incomeSources.manage.business-manage-details.annually")
  val cash = messages("incomeSources.manage.business-manage-details.cash-accounting")
  val traditional = messages("incomeSources.manage.business-manage-details.traditional-accounting")
  val expectedAddress: Option[AddressModel] = Some(AddressModel("Line 1", Some("Line 2"), Some("Line 3"), Some("Line 4"), Some("LN12 2NL"), "NI"))
  val expectedViewAddressString1: String = "Line 1 Line 2 Line 3 Line 4 LN12 2NL United Kingdom"
  val expectedBusinessName: String = "nextUpdates.business"
  val expectedBusinessStartDate: String = "1 January 2022"

  val viewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    address = expectedAddress,
    businessAccountingMethod = Some(false),
    itsaHasMandatedOrVoluntaryStatusCurrentYear = true,
    taxYearOneCrystallised = Some(false),
    taxYearTwoCrystallised = Some(false),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = SelfEmployment
  )

  val viewModel2: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    businessAccountingMethod = None,
    itsaHasMandatedOrVoluntaryStatusCurrentYear = false,
    taxYearOneCrystallised = None,
    taxYearTwoCrystallised = None,
    latencyDetails = None,
    incomeSourceType = SelfEmployment
  )

  val ukViewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = Some(testStartDate),
    address = None,
    businessAccountingMethod = Some(false),
    itsaHasMandatedOrVoluntaryStatusCurrentYear = true,
    taxYearOneCrystallised = Some(false),
    taxYearTwoCrystallised = Some(false),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = UkProperty
  )

  val ukViewModelUnknowns: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    businessAccountingMethod = None,
    itsaHasMandatedOrVoluntaryStatusCurrentYear = false,
    taxYearOneCrystallised = None,
    taxYearTwoCrystallised = None,
    latencyDetails = None,
    incomeSourceType = UkProperty
  )

  val foreignViewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = Some(testStartDate),
    address = None,
    businessAccountingMethod = Some(false),
    itsaHasMandatedOrVoluntaryStatusCurrentYear = true,
    taxYearOneCrystallised = Some(false),
    taxYearTwoCrystallised = Some(false),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = ForeignProperty
  )

  val foreignViewModelUnknowns: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    businessAccountingMethod = None,
    itsaHasMandatedOrVoluntaryStatusCurrentYear = false,
    taxYearOneCrystallised = None,
    taxYearTwoCrystallised = None,
    latencyDetails = None,
    incomeSourceType = ForeignProperty
  )

  def backUrl(isAgent: Boolean): String =
    controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent).url

  class Setup(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController
        .show(taxYear, changeTo, incomeSourceType = SelfEmployment, isAgent = isAgent).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        viewModel,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))

  }

  class Setup2(isAgent: Boolean, error: Boolean = false) {

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        viewModel2,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))

  }

  class ukSetup(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, isAgent, UkProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukViewModel,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  class ukSetupUnknowns(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, isAgent, UkProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukViewModelUnknowns,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  class foreignSetup(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, isAgent, ForeignProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignViewModel,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  class foreignSetupUnknowns(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, isAgent, ForeignProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignViewModelUnknowns,
        isAgent,
        backUrl(isAgent)
      )(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "ManageSelfEmployment - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new Setup(false) {

      document.getElementById("manage-details-table").text() should include(businessName)
      document.getElementById("manage-details-table").text() should include(businessAddress)
      document.getElementById("manage-details-table").text() should include(dateStarted)
      document.getElementById("manage-details-table").text() should include(businessAccountingMethod)
      document.getElementById("manage-details-table").text() should include(reportingMethod1)
      document.getElementById("manage-details-table").text() should include(reportingMethod2)

      document.getElementById("manage-details-table").text() should include(change)
      document.getElementById("manage-details-table").text() should include(change)

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("manage-details-table").text() should include(annually)
      document.getElementById("manage-details-table").text() should include(quarterly)
      document.getElementById("manage-details-table").text() should include(expectedViewAddressString1)
      document.getElementById("manage-details-table").text() should include(expectedBusinessName)
      document.getElementById("manage-details-table").text() should include(expectedBusinessStartDate)
      document.getElementById("manage-details-table").text() should include(cash)

    }
    "render the whole page with unknowns and no change links" in new Setup2(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe businessAccountingMethod

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
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new Setup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe businessAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(5).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-address").text shouldBe expectedViewAddressString1
      document.getElementById("business-name").text shouldBe expectedBusinessName
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash
    }
  }

  "Manage Uk Property - Individual" should {
    "render the heading" in new ukSetup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new ukSetup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new ukSetup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash
    }
    "render the whole page with unknowns and no change links" in new ukSetupUnknowns(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod

      document.getElementById("business-date-started").text shouldBe unknown
      document.getElementById("business-accounting-method").text shouldBe unknown
    }
  }
  "Manage Uk Property - Agent" should {
    "render the heading" in new ukSetup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new ukSetup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new ukSetup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash
    }
    "render the whole page with unknowns and no change links" in new ukSetupUnknowns(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod

      document.getElementById("business-date-started").text shouldBe unknown
      document.getElementById("business-accounting-method").text shouldBe unknown
    }
  }

  "Manage Foreign Property - Individual" should {
    "render the heading" in new foreignSetup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new foreignSetup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new foreignSetup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash
    }
    "render the whole page with unknowns and no change links" in new foreignSetupUnknowns(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod

      document.getElementById("business-date-started").text shouldBe unknown
      document.getElementById("business-accounting-method").text shouldBe unknown
    }
  }
  "Manage Foreign Property - Agent" should {
    "render the heading" in new foreignSetup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new foreignSetup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new foreignSetup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod2

      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe change
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2024-2025", changeTo = "annual")
      document.getElementById("reporting-method-1").text shouldBe annually
      document.getElementById("reporting-method-2").text shouldBe quarterly
      document.getElementById("business-date-started").text shouldBe expectedBusinessStartDate
      document.getElementById("business-accounting-method").text shouldBe cash
    }
    "render the whole page with unknowns and no change links" in new foreignSetupUnknowns(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod

      document.getElementById("business-date-started").text shouldBe unknown
      document.getElementById("business-accounting-method").text shouldBe unknown
    }
  }
}
