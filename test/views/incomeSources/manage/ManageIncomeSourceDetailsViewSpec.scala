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
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import models.incomeSourceDetails.{LatencyYearsCrystallised, LatencyYearsQuarterly, QuarterTypeCalendar, QuarterTypeStandard}
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

  val unknown: String = messages("incomeSources.generic.unknown")
  val heading: String = messages("incomeSources.manage.business-manage-details.heading")
  val soleTrader: String = messages("incomeSources.manage.business-manage-details.sole-trader-section")
  val businessName: String = messages("incomeSources.manage.business-manage-details.business-name")
  val businessAddress: String = messages("incomeSources.manage.business-manage-details.business-address")
  val dateStarted: String = messages("incomeSources.manage.business-manage-details.date-started")
  val isTraditionalAccountingMethod: String = messages("incomeSources.manage.business-manage-details.accounting-method")
  val ukAccountingMethod: String = messages("incomeSources.manage.uk-property-manage-details.accounting-method")
  val quarterlyPeriodType: String = messages("incomeSources.manage.quarterly-period")
  val foreignAccountingMethod: String = messages("incomeSources.manage.foreign-property-manage-details.accounting-method")
  val reportingMethod1: String = messages("incomeSources.manage.business-manage-details.reporting-method", "2022", "2023")
  val reportingMethod2: String = messages("incomeSources.manage.business-manage-details.reporting-method", "2023", "2024")
  val change: String = messages("incomeSources.manage.business-manage-details.change")
  val quarterly: String = messages("incomeSources.manage.business-manage-details.quarterly")
  val annually: String = messages("incomeSources.manage.business-manage-details.annually")
  val cash: String = messages("incomeSources.manage.business-manage-details.cash-accounting")
  val standard: String = messages("incomeSources.manage.quarterly-period.standard")
  val calendar: String = messages("incomeSources.manage.quarterly-period.calendar")
  val traditional: String = messages("incomeSources.manage.business-manage-details.traditional-accounting")
  val expectedAddress: Option[AddressModel] = Some(AddressModel(Some("Line 1"), Some("Line 2"), Some("Line 3"), Some("Line 4"), Some("LN12 2NL"), Some("NI")))
  val expectedViewAddressString1: String = "Line 1 Line 2 Line 3 Line 4 LN12 2NL United Kingdom"
  val expectedBusinessName: String = "nextUpdates.business"
  val expectedBusinessStartDate: String = "1 January 2022"
  val expandableInfoStandardSummary: String = messages("incomeSources.manage.quarterly-period.standard.summary")
  val expandableInfoCalendarSummary: String = messages("incomeSources.manage.quarterly-period.calendar.summary")
  val expandableInfoStandardContentP1: String = messages("incomeSources.manage.quarterly-period.standard.content.p1")
  val expandableInfoStandardContentP2: String = messages("incomeSources.manage.quarterly-period.standard.content.p2")
  val expandableInfoCalendarContentP1: String = messages("incomeSources.manage.quarterly-period.calendar.content.p1")
  val expandableInfoCalendarContentP2: String = messages("incomeSources.manage.quarterly-period.calendar.content.p2")
  val expandableInfoContentP3: String = messages("incomeSources.manage.quarterly-period.content.p3")
  val expandableMoreInfoLink: String = "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#send-quarterly-updates"
  val opensInNewTabText: String = messages("pagehelp.opensInNewTabText")
  val cashBasisAccounting: String = "Cash basis accounting"

  val viewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = Some(testTradeName),
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    address = expectedAddress,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear = Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = SelfEmployment,
    quarterReportingType = Some(QuarterTypeStandard)
  )

  val viewModel2: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear = Some(false)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = None,
      secondYear = None
    ),
    latencyDetails = None,
    incomeSourceType = SelfEmployment,
    quarterReportingType = None
  )

  val ukViewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = Some(testStartDate),
    address = None,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear = Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = UkProperty,
    quarterReportingType = Some(QuarterTypeCalendar)
  )

  val ukViewModelUnknowns: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear =  Some(false)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = None,
      secondYear = None
    ),
    latencyDetails = None,
    incomeSourceType = UkProperty,
    quarterReportingType = None
  )

  val foreignViewModel: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = Some(testStartDate),
    address = None,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear =  Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = ForeignProperty,
    quarterReportingType = Some(QuarterTypeCalendar)
  )

  val foreignViewModelUnknowns: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = None,
    address = None,
    isTraditionalAccountingMethod = false,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = None,
      secondYear =  Some(false)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = None,
      secondYear = None
    ),
    latencyDetails = None,
    incomeSourceType = ForeignProperty,
    quarterReportingType = None
  )

  def backUrl(isAgent: Boolean): String =
    controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent).url

  class Setup(isAgent: Boolean, error: Boolean = false) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      if(isAgent) {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.showAgent(taxYear, changeTo, incomeSourceType = SelfEmployment).url
      }else{
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, incomeSourceType = SelfEmployment).url
      }
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
      if (isAgent) {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.showAgent(taxYear, changeTo, UkProperty).url
      }else {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, UkProperty).url
      }
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
      if(isAgent) {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.showAgent(taxYear, changeTo, UkProperty).url
      }else{
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo, UkProperty).url
      }
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
      if(isAgent) {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.showAgent(taxYear, changeTo,  ForeignProperty).url
      }else{
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo,  ForeignProperty).url
      }
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
      if(isAgent) {
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.showAgent(taxYear, changeTo,  ForeignProperty).url
      }else{
        controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, changeTo,  ForeignProperty).url
      }
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
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new Setup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe isTraditionalAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(5).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(6).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessName
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe expectedViewAddressString1
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe standard
      document.getElementsByClass("govuk-summary-list__value").eq(5).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(6).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoStandardSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoStandardContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoStandardContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink

    }
    "render the whole page with unknowns and no change links" in new Setup2(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe isTraditionalAccountingMethod

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe "Cash basis accounting"
    }
  }
  "ManageSelfEmployment - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new Setup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new Setup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe businessAddress
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe isTraditionalAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(5).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(6).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessName
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe expectedViewAddressString1
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe standard
      document.getElementsByClass("govuk-summary-list__value").eq(5).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(6).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoStandardSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoStandardContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoStandardContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink
    }
  }

  "Manage Uk Property - Individual" should {
    "render the heading" in new ukSetup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new ukSetup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new ukSetup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe calendar
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoCalendarSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoCalendarContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoCalendarContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink
    }
    "render the whole page with unknowns and no change links" in new ukSetupUnknowns(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "Cash basis accounting"
    }
  }
  "Manage Uk Property - Agent" should {
    "render the heading" in new ukSetup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new ukSetup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new ukSetup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe calendar
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoCalendarSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoCalendarContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoCalendarContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink
    }
    "render the whole page with unknowns and no change links" in new ukSetupUnknowns(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe ukAccountingMethod

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "Cash basis accounting"
    }
  }

  "Manage Foreign Property - Individual" should {
    "render the heading" in new foreignSetup(false) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new foreignSetup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }
    "render the whole page" in new foreignSetup(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe calendar
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoCalendarSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoCalendarContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoCalendarContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink
    }
    "render the whole page with unknowns and no change links" in new foreignSetupUnknowns(false) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe "Cash basis accounting"
    }
  }
  "Manage Foreign Property - Agent" should {
    "render the heading" in new foreignSetup(true) {
      document.getElementsByClass("govuk-heading-l").text shouldBe heading
    }
    "render the back correct back Url" in new foreignSetup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }
    "render the whole page" in new foreignSetup(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe quarterlyPeriodType
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe reportingMethod1
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe reportingMethod2

      document.getElementById("change-link-1").text() shouldBe change
      document.getElementById("change-link-2").text() shouldBe change

      document.getElementById("change-link-1").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      document.getElementById("change-link-2").attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe expectedBusinessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe cash
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe calendar
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe annually
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe quarterly

      val expandableInfo = document.getElementById("expandable-info")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoCalendarSummary
      expandableInfo.getElementById("expandable-info-p1").text() shouldBe expandableInfoCalendarContentP1
      expandableInfo.getElementById("expandable-info-p2").text() shouldBe expandableInfoCalendarContentP2
      expandableInfo.getElementById("expandable-more-info-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("expandable-more-info-link").attr("href") shouldBe expandableMoreInfoLink
    }
    "render the whole page with unknowns and no change links" in new foreignSetupUnknowns(true) {

      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe dateStarted
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe foreignAccountingMethod

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe unknown
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe cashBasisAccounting
    }
  }
}