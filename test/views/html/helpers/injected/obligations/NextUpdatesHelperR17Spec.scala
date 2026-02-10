/*
 * Copyright 2025 HM Revenue & Customs
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

package views.html.helpers.injected.obligations

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Exempt, ITSAStatus, Mandated, Voluntary}
import models.obligations._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.optout.OptOutProposition
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants.{quarterlyBusinessObligation, twoObligationsSuccessModel}
import testUtils.TestSupport

import java.time.LocalDate

class NextUpdatesHelperR17Spec extends TestSupport {

  class Setup(isAgent: Boolean, currentObligations: NextUpdatesViewModel, currentYearStatus: ITSAStatus, nextYearStatus: ITSAStatus) {
    val nextUpdatesHelper = app.injector.instanceOf[NextUpdatesHelperR17]

    val optOutProposition = OptOutProposition.createOptOutProposition(
      currentYear = TaxYear(2025, 2026),
      previousYearCrystallised = true,
      previousYearItsaStatus = Annual,
      currentYearItsaStatus = currentYearStatus,
      nextYearItsaStatus = nextYearStatus
    )

    val html: HtmlFormat.Appendable = nextUpdatesHelper(isAgent, currentObligations, optOutProposition, false, taxYearStatusesCyNy = (currentYearStatus, nextYearStatus))(implicitly, getIndividualUser(FakeRequest()))

    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  lazy val obligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    twoObligationsSuccessModel.obligations
  ))).obligationsByDate(isR17ContentEnabled = true).map{
    case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
  }, Seq(DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, LocalDate.of(2025, 1, 31), Seq(ObligationWithIncomeType("Quarter", quarterlyBusinessObligation)), Seq.empty)))


  "Next updates helper for Release 17" when {

    "displaying for all scenarios" should {
      "display the correct number of tabs" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.select(".govuk-tabs__list-item").size() shouldBe 2
      }
    }

    "CY Status is Annual and CY+1 is Annual" should {

      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-subheading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-compatible-software-desc").text() shouldBe "As you are not using Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are not using Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }
    }

    "CY Status is Voluntary/Mandated and CY+1 is Annual" should {
      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the quarterly updates subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-subheading").text() shouldBe "Quarterly updates due"
      }

      "display the quarterly updates description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-subdesc").text() shouldBe "Every 3 months an update is due for each of your property and sole trader income sources."
      }

      "display the dropdown heading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-details__summary-text").text() shouldBe "Find out more about quarterly updates"
      }

      "display the description in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-dropdown-desc").text() shouldBe "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"
      }

      "display the bullet points in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "new information and corrections made since the last update any information you’ve already provided that has not changed"
      }

      "display the description containing the compatible software link within the dropdown in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-dropdown-desc2").text() shouldBe "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("active-quarterly-compatible-software-link-dropdown").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the tax year summary description for the current year tab - individuals" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }

      "display the tax year summary description for the current year tab - agents" in new Setup(isAgent = true, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showAgentTaxYears().url
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-heading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-desc").text() shouldBe "If you have submitted quarterly updates for the tax year, in your tax return you will also provide any other taxable income. You will then need to file your return using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("quarterly-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      //Upcoming Deadlines
      "display the upcoming deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-table-heading").text() shouldBe "Upcoming deadlines"
      }

      "display the correct table headings for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("table-head-name-deadline").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("quarterly-deadline-date-upcoming-0").text() shouldBe "30 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-0").text() should fullyMatch regex """1\sJul\s2017\sto\s30\s(?:Sep|Sept)\s2017"""
        pageDocument.getElementById("quarterly-income-sources-upcoming-0").text() shouldBe "Business income"
        pageDocument.getElementById("quarterly-deadline-date-upcoming-1").text() shouldBe "31 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-1").text() shouldBe "6 Apr 2017 to 5 Apr 2018"
        pageDocument.getElementById("quarterly-income-sources-upcoming-1").text() shouldBe "Business income"
      }

      // Missed Deadlines
      "display the missing deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("missed-deadlines-table-heading").text() shouldBe "Missed deadlines"
      }

      "display the correct table headings for the current year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("table-head-name-deadline-missed").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period-missed").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due-missed").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("quarterly-deadline-date-missed-0").text() shouldBe "31 Jan 2025"
        pageDocument.getElementById("quarterly-period-missed-0").text() should fullyMatch regex """1\sJul\s2017\sto\s30\s(?:Sep|Sept)\s2017"""
        pageDocument.getElementById("quarterly-income-sources-missed-0").text() shouldBe "Quarter"
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are not using Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }
    }

    "CY Status is Annual and CY+1 is Voluntary/Mandated" should {
      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-subheading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-compatible-software-desc").text() shouldBe "As you are not using Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "not display the description for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        Option(pageDocument.getElementById("active-quarterly-desc")) shouldBe None
      }

      "display the quarterly updates subheading for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-subheading").text() shouldBe "Quarterly updates due"
      }

      "display the quarterly updates description for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-subdesc").text() shouldBe "Every 3 months an update is due for each of your property and sole trader income sources."
      }

      "display the dropdown heading for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementsByClass("govuk-details__summary-text").text() shouldBe "Find out more about quarterly updates"
      }

      "display the description in the dropdown for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-dropdown-desc").text() shouldBe "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"
      }

      "display the bullet points in the dropdown for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "new information and corrections made since the last update any information you’ve already provided that has not changed"
      }

      "display the description containing the compatible software link within the dropdown in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-dropdown-desc2").text() shouldBe "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("active-quarterly-compatible-software-link-dropdown").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the upcoming deadlines subheading for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-table-heading").text() shouldBe "Upcoming deadlines"
      }

      "display the tax year summary description for the next year tab - individuals" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }

      "display the tax year summary description for the next year tab - agents" in new Setup(isAgent = true, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showAgentTaxYears().url
      }

      "display the tax return due subheading for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-return-due-heading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-return-due-desc").text() shouldBe "If you have submitted quarterly updates for the tax year, in your tax return you will also provide any other taxable income. You will then need to file your return using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("quarterly-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the correct tax return due date for the 2025-26 tax year in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("active-quarterly-return-due-date").text() shouldBe "Your return for the 2026 to 2027 tax year is due by 31 January 2028."
      }

      "display the correct table headings for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("table-head-name-deadline").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("quarterly-deadline-date-upcoming-0").text() shouldBe "30 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-0").text() should fullyMatch regex """1\sJul\s2017\sto\s30\s(?:Sep|Sept)\s2017"""
        pageDocument.getElementById("quarterly-income-sources-upcoming-0").text() shouldBe "Business income"
        pageDocument.getElementById("quarterly-deadline-date-upcoming-1").text() shouldBe "31 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-1").text() shouldBe "6 Apr 2017 to 5 Apr 2018"
        pageDocument.getElementById("quarterly-income-sources-upcoming-1").text() shouldBe "Business income"
      }

      // Missed Deadlines
      "not display the missing deadlines subheading for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        Option(pageDocument.getElementById("missed-deadlines-table-heading")) shouldBe None
      }

      "not display the correct table headings for the next year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        Option(pageDocument.getElementById("table-head-name-deadline-missed")) shouldBe None
        Option(pageDocument.getElementById("table-head-name-period-missed")) shouldBe None
        Option(pageDocument.getElementById("table-head-name-updates-due-missed")) shouldBe None
      }

      "not display the correct table content for the next year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        Option(pageDocument.getElementById("quarterly-deadline-date-missed-0")) shouldBe None
        Option(pageDocument.getElementById("quarterly-period-missed-0")) shouldBe None
        Option(pageDocument.getElementById("quarterly-income-sources-missed-0")) shouldBe None
      }
    }

    "CY Status is Voluntary/Mandated and CY+1 is Voluntary/Mandated" should {

      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the quarterly updates subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-subheading").text() shouldBe "Quarterly updates due"
      }

      "display the quarterly updates description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-subdesc").text() shouldBe "Every 3 months an update is due for each of your property and sole trader income sources."
      }

      "display the dropdown heading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-details__summary-text").text() shouldBe "Find out more about quarterly updates"
      }

      "display the description in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-dropdown-desc").text() shouldBe "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"
      }

      "display the bullet points in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "new information and corrections made since the last update any information you’ve already provided that has not changed"
      }

      "display the description containing the compatible software link within the dropdown in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-dropdown-desc2").text() shouldBe "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("active-quarterly-compatible-software-link-dropdown").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the upcoming deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-table-heading").text() shouldBe "Upcoming deadlines"
      }

      "display the tax year summary description for the current year tab - individuals" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }

      "display the tax year summary description for the current year tab - agents" in new Setup(isAgent = true, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").text() shouldBe "tax year summary"
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showAgentTaxYears().url
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-heading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-desc").text() shouldBe "If you have submitted quarterly updates for the tax year, in your tax return you will also provide any other taxable income. You will then need to file your return using software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("quarterly-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("active-quarterly-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct table headings for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("table-head-name-deadline").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("quarterly-deadline-date-upcoming-0").text() shouldBe "30 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-0").text() should fullyMatch regex """1\sJul\s2017\sto\s30\s(?:Sep|Sept)\s2017"""
        pageDocument.getElementById("quarterly-income-sources-upcoming-0").text() shouldBe "Business income"
        pageDocument.getElementById("quarterly-deadline-date-upcoming-1").text() shouldBe "31 Oct 2017"
        pageDocument.getElementById("quarterly-period-upcoming-1").text() shouldBe "6 Apr 2017 to 5 Apr 2018"
        pageDocument.getElementById("quarterly-income-sources-upcoming-1").text() shouldBe "Business income"
      }

      // Missed Deadlines
      "display the voluntary missing deadlines description if the user is reporting voluntary for CY" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("missed-deadline-voluntary-desc").text() shouldBe "Because you are not required to use Making Tax Digital for Income Tax, there are no penalties for missing these quarterly deadlines."
      }

      "not display the voluntary missing deadlines description if the user is reporting mandated for CY" in new Setup(isAgent = false, obligationsModel, Mandated, Annual) {
        Option(pageDocument.getElementById("missed-deadline-voluntary-desc")) shouldBe None
      }

      "display the warning text for missed deadlines in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("missed-deadlines-warning").text() shouldBe "! Warning You have missed deadlines for one or more quarterly updates."
      }

      "display the missing deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("missed-deadlines-table-heading").text() shouldBe "Missed deadlines"
      }

      "display the correct table headings for the current year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("table-head-name-deadline-missed").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period-missed").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due-missed").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab - Missed Deadlines" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("quarterly-deadline-date-missed-0").text() shouldBe "31 Jan 2025"
        pageDocument.getElementById("quarterly-period-missed-0").text() should fullyMatch regex """1\sJul\s2017\sto\s30\s(?:Sep|Sept)\s2017"""
        pageDocument.getElementById("quarterly-income-sources-missed-0").text() shouldBe "Quarter"
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are either required or voluntarily signed up for the 2026 to 2027 tax year, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
        pageDocument.getElementById("quarterly-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }
    }

    "CY Status is Exempt/DigitallyExempt and CY+1 is Exempt/DigitallyExempt" should {

      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("current-year-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("current-year-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the quarterly updates subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("current-year-subheading").text() shouldBe "Tax return due"
      }

      "display the compatible software link section in the current year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("current-year-compatible-software-desc").text() shouldBe "As you are exempt from Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are exempt from Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById("annual-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Exempt, Exempt) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."

      }
    }
  }
}

