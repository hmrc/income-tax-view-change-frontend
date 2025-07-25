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

package views.manageBusinesses.manage

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.{LatencyYearsCrystallised, LatencyYearsQuarterly}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BusinessDetailsTestConstants._
import testUtils.{TestSupport, ViewSpec}
import views.constants.ManageIncomeSourceDetailsViewConstants._
import views.html.manageBusinesses.manage.ManageIncomeSourceDetailsView
import views.messages.ManageIncomeSourceDetailsViewMessages._

class ManageIncomeSourceDetailsViewSpec extends TestSupport with ViewSpec {

  val manageIncomeSourceDetailsView: ManageIncomeSourceDetailsView = app.injector.instanceOf[ManageIncomeSourceDetailsView]

  def reportingFrequencyLink(isAgent: Boolean): String =
    controllers.routes.ReportingFrequencyPageController.show(isAgent).url

  def backUrl(isAgent: Boolean): String = if (isAgent) controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url

  else controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url

  def summaryListRowKeys()(implicit document: Document) = document.getElementsByClass("govuk-summary-list__key")

  def summaryListRowValues()(implicit document: Document) = document.getElementsByClass("govuk-summary-list__value")

  def h1()(implicit document: Document) = document.getElementsByClass("govuk-heading-l")

  def changeLink(i: Int)(implicit document: Document) = document.getElementById(s"change-link-$i")


  class SelfEmploymentSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, SelfEmployment).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        selfEmploymentViewModel,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class SelfEmploymentUnknownsSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, incomeSourceType = SelfEmployment).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        selfEmploymentViewModelWithUnknowns,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class SelfEmploymentCrystallisedSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, incomeSourceType = SelfEmployment).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        selfEmploymentViewModelOneYearCrystallised,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class SelfEmploymentCYLatencyUnknownSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        selfEmploymentViewModelCYUnknown,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class UkSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, UkProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukViewModel,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))
  }

  class UkSetupUnknowns(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, UkProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukViewModelUnknowns,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))
  }

  class UkCrystallisedSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, UkProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukPropertyViewModelOneYearCrystallised,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class UkCYLatencyUnknownSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        ukPropertyViewModelCYUnknown,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class ForeignSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, ForeignProperty).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignViewModel,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))
  }

  class ForeignSetupUnknowns(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, ForeignProperty).url
    }

    val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignViewModelUnknowns,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))
  }

  class ForeignCrystallisedSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(isAgent, taxYear, changeTo, incomeSourceType = SelfEmployment).url
    }

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignPropertyViewModelOneYearCrystallised,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class ForeignCYLatencyUnknownSetup(isAgent: Boolean, startDateEnabled: Boolean = true) {

    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        foreignPropertyLatencyYearTwoUnknown,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))

  }

  class SelfEmploymentSetupWithOptInContentR17(isAgent: Boolean, startDateEnabled: Boolean = true) {
    lazy val view: HtmlFormat.Appendable = {
      manageIncomeSourceDetailsView(
        selfEmploymentViewModel,
        isAgent,
        backUrl = backUrl(isAgent),
        showStartDate = startDateEnabled,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = true,
        showReportingFrequencyLink = true
      )(messages, implicitly)
    }

    implicit val document: Document = Jsoup.parse(contentAsString(view))
  }

  "ManageSelfEmployment - Individual" should {

    "render the heading" in new SelfEmploymentSetup(false) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new SelfEmploymentSetup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }

    "render the whole page" in new SelfEmploymentSetup(false) {

      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe businessName
      summaryListRowKeys().eq(1).text() shouldBe businessAddress
      summaryListRowKeys().eq(2).text() shouldBe dateStarted
      summaryListRowKeys().eq(3).text() shouldBe typeOfTrade
      summaryListRowKeys().eq(4).text() shouldBe ukAccountingMethod
      summaryListRowKeys().eq(5).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(6).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(7).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "annual")
      summaryListRowValues().eq(0).text() shouldBe expectedBusinessName
      summaryListRowValues().eq(1).text() shouldBe expectedViewAddressString1
      summaryListRowValues().eq(2).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(4).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(5).text() shouldBe standard
      summaryListRowValues().eq(6).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(7).text() shouldBe quarterlyGracePeriod
      document.getElementById("reportingFrequency").text() shouldBe reportingFrequencyText
      document.getElementById("reportingFrequency-link").attr("href") shouldBe reportingFrequencyLink(false)
    }

    "not display the accounting method row when showAccountingMethod is false" in {
      val view = manageIncomeSourceDetailsView(
        selfEmploymentViewModel,
        isAgent = false,
        backUrl = backUrl(false),
        showStartDate = true,
        showAccountingMethod = false,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)

      implicit val document: Document = Jsoup.parse(contentAsString(view))
      val summaryKeys = summaryListRowKeys().eachText()

      summaryKeys should not contain isTraditionalAccountingMethod
    }

    "render the whole page with unknowns and no change links or inset text" in new SelfEmploymentUnknownsSetup(false) {

      summaryListRowKeys().eq(0).text() shouldBe businessName
      summaryListRowKeys().eq(1).text() shouldBe businessAddress
      summaryListRowKeys().eq(2).text() shouldBe dateStarted
      summaryListRowKeys().eq(3).text() shouldBe typeOfTrade
      summaryListRowKeys().eq(4).text() shouldBe ukAccountingMethod

      summaryListRowValues().eq(0).text() shouldBe unknown
      summaryListRowValues().eq(1).text() shouldBe unknown
      summaryListRowValues().eq(2).text() shouldBe unknown
      summaryListRowValues().eq(3).text() shouldBe unknown
      summaryListRowValues().eq(4).text() shouldBe "Cash basis accounting"
    }

    "do not render the reporting frequency rows when NO latency details" in new SelfEmploymentUnknownsSetup(false) {
      summaryListRowKeys().eq(6).isDefined shouldBe false
      summaryListRowKeys().eq(7).isDefined shouldBe false
    }

    "display standard update period dropdown when NO latency details" in new SelfEmploymentUnknownsSetup(false) {
      val expandableInfo = document.getElementById("standard-update-period-dropdown")
      expandableInfo.getElementsByClass("govuk-details__summary-text").eq(0).text() shouldBe expandableInfoStandardSummary
      expandableInfo.getElementById("expandable-standard-update-period").text() shouldBe expandableInfoStandardContentP1
      expandableInfo.getElementById("software-support").text() shouldBe expandableInfoStandardContentP2
      expandableInfo.getElementById("learn-about-quarters-link").text() shouldBe expandableInfoContentP3 + " " + opensInNewTabText
      expandableInfo.getElementById("learn-about-quarters-link").attr("href") shouldBe expandableMoreInfoLink

    }

    "render the reporting frequency rows and content IF there are latency details" in new SelfEmploymentSetup(false) {
      document.getElementById("up-to-two-tax-years").text() shouldBe "Because this is still a new business, you can change how often you report for it for up to 2 tax years. From April 2024, you could be required to report quarterly."
      summaryListRowKeys().eq(6).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(7).text() shouldBe reportingMethod2
    }

    "render the reporting frequency rows per NON CRYSTALLISED YEAR" in new SelfEmploymentCrystallisedSetup(false) {
      summaryListRowKeys().eq(6).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(7).text() shouldBe reportingMethod2
    }

    "render the change links where status is Quarterly" in new SelfEmploymentCrystallisedSetup(false) {
      Option(changeLink(1)) shouldBe None
      changeLink(2).text() shouldBe change
    }

    "do not display change link when CY & CY-1 ITSA Status are unknown" in new SelfEmploymentUnknownsSetup(false) {
      Option(changeLink(1)) shouldBe None
      Option(changeLink(2)) shouldBe None
    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new SelfEmploymentSetup(false, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }

    "render the correct text when OptInOptOutContentUpdateR17 feature is enabled" in new SelfEmploymentSetupWithOptInContentR17(false) {

      def insetText()(implicit document: Document) = document.getElementsByClass("up-to-two-tax-years").text()

      insetText shouldBe ""
    }

    "render the updated reporting frequency sentence when OptInOptOutContentUpdateR17 feature is enabled" in new SelfEmploymentSetupWithOptInContentR17(false) {

      document.getElementById("reportingFrequency").text shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
      document.getElementById("reportingFrequency-link").text shouldBe "view and change your reporting obligations for all your businesses"
      document.getElementById("reportingFrequency-link").attr("href") shouldBe reportingFrequencyLink(false)

    }

    "render the MTD opt-in rows correctly with 'Yes/No' status and 'Opt-out/Sign up' links when OptInOptOutContentUpdateR17 is enabled" in {
      val taxYear1 = "2025"
      val taxYear2 = "2026"

      val testViewModel = selfEmploymentViewModel.copy(
        latencyYearsQuarterly = LatencyYearsQuarterly(Some(true), Some(true)),
        latencyYearsCrystallised = LatencyYearsCrystallised(Some(false), Some(false)),
        latencyDetails = Some(testLatencyDetails3.copy(
          taxYear1 = "2025",
          latencyIndicator1 = "Q",
          taxYear2 = "2026",
          latencyIndicator2 = "A"
        ))
      )


      val view = manageIncomeSourceDetailsView(
        testViewModel,
        isAgent = false,
        showStartDate = true,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = true,
        showReportingFrequencyLink = true,
        backUrl = backUrl(false)
      )(messages, implicitly)

      implicit val document = Jsoup.parse(contentAsString(view))

      val expectedRow1 = s"Using Making Tax Digital for Income Tax for ${taxYear1.toInt - 1} to $taxYear1"
      val expectedRow2 = s"Using Making Tax Digital for Income Tax for ${taxYear2.toInt - 1} to $taxYear2"

      summaryListRowKeys().text() should include(expectedRow1)
      summaryListRowKeys().text() should include(expectedRow2)

      summaryListRowValues().eq(6).text() shouldBe "Yes"

      val actionLinks = document.select(".govuk-summary-list__actions a")
      actionLinks.eq(0).text() shouldBe "Opt out"
      actionLinks.eq(1).text() shouldBe "Sign up"
    }

    "not render an MTD opt-in row if one of the latency years is crystallised" in {

      val taxYear1 = "2025"
      val taxYear2 = "2026"

      val annualLatencyIndicator = "Q"
      val quarterlyLatencyIndicator = "A"

      val testViewModel = selfEmploymentViewModel.copy(
        latencyYearsQuarterly = LatencyYearsQuarterly(Some(true), Some(true)),
        latencyYearsCrystallised = LatencyYearsCrystallised(Some(false), Some(true)),
        latencyDetails = Some(testLatencyDetails3.copy(
          taxYear1 = taxYear1, latencyIndicator1 = annualLatencyIndicator,
          taxYear2 = taxYear2, latencyIndicator2 = quarterlyLatencyIndicator
        ))
      )


      val view = manageIncomeSourceDetailsView(
        testViewModel,
        isAgent = false,
        showStartDate = true,
        showAccountingMethod = true,
        showOptInOptOutContentUpdateR17 = true,
        showReportingFrequencyLink = true,
        backUrl = backUrl(false)
      )(messages, implicitly)

      implicit val document = Jsoup.parse(contentAsString(view))

      val allRows = summaryListRowKeys().eachText()

      val expectedRow1 = s"Using Making Tax Digital for Income Tax for ${taxYear1.toInt - 1} to $taxYear1"
      val expectedRow2 = s"Using Making Tax Digital for Income Tax for ${taxYear2.toInt - 1} to $taxYear2"

      allRows should contain(expectedRow1)
      allRows should contain (expectedRow2)

      val values = document.getElementsByClass("govuk-summary-list__value").eachText()
      values should contain("Yes")

      val actions = document.select(".govuk-summary-list__actions a").eachText()
      actions should contain("Opt out")
      actions should not contain ("Sign up")
    }


  }

  "ManageSelfEmployment - Agent" should {

    "render the heading" in new SelfEmploymentSetup(true) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new SelfEmploymentSetup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }

    "render the whole page" in new SelfEmploymentSetup(true) {

      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe businessName
      summaryListRowKeys().eq(1).text() shouldBe businessAddress
      summaryListRowKeys().eq(2).text() shouldBe dateStarted
      summaryListRowKeys().eq(3).text() shouldBe typeOfTrade
      summaryListRowKeys().eq(4).text() shouldBe ukAccountingMethod
      summaryListRowKeys().eq(5).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(6).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(7).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(id = "XA00001234", taxYear = "2023-2024", changeTo = "annual")
      summaryListRowValues().eq(0).text() shouldBe expectedBusinessName
      summaryListRowValues().eq(1).text() shouldBe expectedViewAddressString1
      summaryListRowValues().eq(2).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(4).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(5).text() shouldBe standard
      summaryListRowValues().eq(6).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(7).text() shouldBe quarterlyGracePeriod
      document.getElementById("reportingFrequency").text() shouldBe reportingFrequencyText
      document.getElementById("reportingFrequency-link").attr("href") shouldBe reportingFrequencyLink(true)

    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new SelfEmploymentSetup(true, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }
  }

  "Manage Uk Property - Individual" should {

    "render the heading" in new UkSetup(false) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new UkSetup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }

    "render the whole page" in new UkSetup(false) {

      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe ukAccountingMethod
      summaryListRowKeys().eq(2).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      summaryListRowValues().eq(0).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(1).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(2).text() shouldBe calendar
      summaryListRowValues().eq(3).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(4).text() shouldBe quarterlyGracePeriod
    }

    "not display the accounting method row when showAccountingMethod is false" in {
      val view = manageIncomeSourceDetailsView(
        ukViewModel,
        isAgent = false,
        backUrl = backUrl(false),
        showStartDate = true,
        showAccountingMethod = false,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)

      implicit val document = Jsoup.parse(contentAsString(view))
      val summaryKeys = summaryListRowKeys().eachText()

      summaryKeys should not contain ukAccountingMethod
    }

    "not display the accounting method row when showAccountingMethod is true but isTraditionalAccountingMethod in the view model is empty" in {
      val view = manageIncomeSourceDetailsView(
        ukViewModel.copy(isTraditionalAccountingMethod = None),
        isAgent = false,
        backUrl = backUrl(false),
        showStartDate = true,
        showAccountingMethod = false,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)

      val document = Jsoup.parse(contentAsString(view))
      val summaryKeys = document.getElementsByClass("govuk-summary-list__key").eachText()

      summaryKeys should not contain ukAccountingMethod
    }

    "render the whole page with unknowns and no change links" in new UkSetupUnknowns(false) {

      document.getElementById("standard-update-period-dropdown").text() shouldBe "What is a standard quarterly period? This business is reporting from 6 April in line with the tax year, also known as using standard update periods. If your software supports it, you can choose to report using calendar update periods which end on the last day of the month. Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("expandable-standard-update-period").text() shouldBe expandableInfoStandardContentP1
      document.getElementById("software-support").text() shouldBe expandableInfoStandardContentP2
      document.getElementById("learn-about-quarters-link").text() shouldBe "Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("learn-about-quarters-link").attr("href") shouldBe expandableMoreInfoLink


      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe ukAccountingMethod

      summaryListRowValues().eq(0).text() shouldBe unknown
      summaryListRowValues().eq(1).text() shouldBe "Cash basis accounting"
    }

    "Do not render the reporting frequency rows when NO latency details" in new UkSetupUnknowns(false) {
      summaryListRowKeys().eq(3).isDefined shouldBe false
      summaryListRowKeys().eq(4).isDefined shouldBe false
    }

    "render the reporting frequency rows IF there are latency details" in new UkSetup(false) {
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2
    }

    "render the reporting frequency rows per NON CRYSTALLISED YEAR" in new UkCrystallisedSetup(false) {
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2
    }

    "render the change links where status is Quarterly" in new UkCrystallisedSetup(false) {
      Option(changeLink(1)) shouldBe None
      changeLink(2).text() shouldBe change
    }

    "dont display change link when CY & CY-1 ITSA Status are unknown" in new UkSetupUnknowns(false) {
      Option(changeLink(1)) shouldBe None
      Option(changeLink(2)) shouldBe None
    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new UkSetup(false, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }

  }

  "Manage Uk Property - Agent" should {

    "render the heading" in new UkSetup(true) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new UkSetup(true) {

      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }

    "render the whole page" in new UkSetup(true) {

      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe ukAccountingMethod
      summaryListRowKeys().eq(2).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      summaryListRowValues().eq(0).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(1).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(2).text() shouldBe calendar
      summaryListRowValues().eq(3).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(4).text() shouldBe quarterlyGracePeriod

    }

    "render the whole page with unknowns and no change links" in new UkSetupUnknowns(true) {

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe ukAccountingMethod

      summaryListRowValues().eq(0).text() shouldBe unknown
      summaryListRowValues().eq(1).text() shouldBe "Cash basis accounting"

      document.getElementById("standard-update-period-dropdown").text() shouldBe "What is a standard quarterly period? This business is reporting from 6 April in line with the tax year, also known as using standard update periods. If your software supports it, you can choose to report using calendar update periods which end on the last day of the month. Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("expandable-standard-update-period").text() shouldBe expandableInfoStandardContentP1
      document.getElementById("software-support").text() shouldBe expandableInfoStandardContentP2
      document.getElementById("learn-about-quarters-link").text() shouldBe "Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("learn-about-quarters-link").attr("href") shouldBe expandableMoreInfoLink

    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new UkSetup(false, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }
  }

  "Manage Foreign Property - Individual" should {

    "render the heading" in new ForeignSetup(false) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new ForeignSetup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(false)
    }

    "render the whole page" in new ForeignSetup(false) {
      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe foreignAccountingMethod
      summaryListRowKeys().eq(2).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")
      summaryListRowValues().eq(0).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(1).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(2).text() shouldBe calendar
      summaryListRowValues().eq(3).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(4).text() shouldBe quarterlyGracePeriod

    }

    "not display the accounting method row when showAccountingMethod is false" in {
      val view = manageIncomeSourceDetailsView(
        foreignViewModel,
        isAgent = false,
        backUrl = backUrl(false),
        showStartDate = true,
        showAccountingMethod = false,
        showOptInOptOutContentUpdateR17 = false,
        showReportingFrequencyLink = true
      )(messages, implicitly)

      implicit val document = Jsoup.parse(contentAsString(view))
      val summaryKeys = summaryListRowKeys().eachText()

      summaryKeys should not contain foreignAccountingMethod
    }

    "render the whole page with unknowns and no change links or inset text" in new ForeignSetupUnknowns(false) {

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe foreignAccountingMethod

      summaryListRowValues().eq(0).text() shouldBe unknown
      summaryListRowValues().eq(1).text() shouldBe "Cash basis accounting"

      document.getElementById("standard-update-period-dropdown").text() shouldBe "What is a standard quarterly period? This business is reporting from 6 April in line with the tax year, also known as using standard update periods. If your software supports it, you can choose to report using calendar update periods which end on the last day of the month. Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("expandable-standard-update-period").text() shouldBe expandableInfoStandardContentP1
      document.getElementById("software-support").text() shouldBe expandableInfoStandardContentP2
      document.getElementById("learn-about-quarters-link").text() shouldBe "Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("learn-about-quarters-link").attr("href") shouldBe expandableMoreInfoLink
    }

    "Do not render the reporting frequency rows when NO latency details" in new ForeignSetupUnknowns(false) {
      summaryListRowKeys().eq(3).isDefined shouldBe false
      summaryListRowKeys().eq(4).isDefined shouldBe false
    }

    "render the reporting frequency rows IF there are latency details" in new ForeignSetup(false) {
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2
    }

    "render the reporting frequency rows per NON CRYSTALLISED YEAR" in new ForeignCrystallisedSetup(false) {
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2
    }

    "render the change links where status is Quarterly" in new ForeignCrystallisedSetup(false) {
      Option(changeLink(1)) shouldBe None
      changeLink(2).text() shouldBe change
    }

    "dont display change link when CY & CY-1 ITSA Status are unknown" in new ForeignSetupUnknowns(false) {
      Option(changeLink(1)) shouldBe None
      Option(changeLink(2)) shouldBe None
    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new ForeignSetup(false, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }
  }

  "Manage Foreign Property - Agent" should {

    "render the heading" in new ForeignSetup(true) {
      h1().text shouldBe heading
    }

    "render the back correct back Url" in new ForeignSetup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl(true)
    }

    "render the whole page" in new ForeignSetup(true) {

      document.getElementById("up-to-two-tax-years").text() shouldBe newBusinessInsetText

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe foreignAccountingMethod
      summaryListRowKeys().eq(2).text() shouldBe quarterlyPeriodType
      summaryListRowKeys().eq(3).text() shouldBe reportingMethod1
      summaryListRowKeys().eq(4).text() shouldBe reportingMethod2

      changeLink(1).text() shouldBe change
      changeLink(2).text() shouldBe change

      changeLink(1).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2022-2023", changeTo = "quarterly")
      changeLink(2).attr("href") shouldBe changeReportingMethodUrl(taxYear = "2023-2024", changeTo = "annual")

      summaryListRowValues().eq(0).text() shouldBe expectedBusinessStartDate
      summaryListRowValues().eq(1).text() shouldBe cashBasisAccounting
      summaryListRowValues().eq(2).text() shouldBe calendar
      summaryListRowValues().eq(3).text() shouldBe annuallyGracePeriod
      summaryListRowValues().eq(4).text() shouldBe quarterlyGracePeriod
    }

    "render the whole page with unknowns and no change links or inset text" in new ForeignSetupUnknowns(true) {

      summaryListRowKeys().eq(0).text() shouldBe dateStarted
      summaryListRowKeys().eq(1).text() shouldBe foreignAccountingMethod

      summaryListRowValues().eq(0).text() shouldBe unknown
      summaryListRowValues().eq(1).text() shouldBe cashBasisAccounting

      document.getElementById("standard-update-period-dropdown").text() shouldBe "What is a standard quarterly period? This business is reporting from 6 April in line with the tax year, also known as using standard update periods. If your software supports it, you can choose to report using calendar update periods which end on the last day of the month. Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("expandable-standard-update-period").text() shouldBe expandableInfoStandardContentP1
      document.getElementById("software-support").text() shouldBe expandableInfoStandardContentP2
      document.getElementById("learn-about-quarters-link").text() shouldBe "Learn more about standard and calendar quarters (opens in new tab)"
      document.getElementById("learn-about-quarters-link").attr("href") shouldBe expandableMoreInfoLink
    }

    "do not display start date if DisplayBusinessStartDate is disabled" in new ForeignSetup(true, startDateEnabled = false) {
      Option(document.getElementById("manage-details-table")).mkString("").contains("Date started") shouldBe false
    }

  }
}