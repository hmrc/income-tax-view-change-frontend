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
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import models.obligations._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.optout.OptOutProposition
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
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

    val html: HtmlFormat.Appendable = nextUpdatesHelper(isAgent, currentObligations, optOutProposition)(implicitly, getIndividualUser(FakeRequest()))

    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  lazy val obligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    twoObligationsSuccessModel.obligations
  ))).obligationsByDate(isR17ContentEnabled = true).map{
    case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
  })


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
        pageDocument.getElementById("current-year-compatible-software-desc").text() shouldBe "As you are opted out of Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)"
        pageDocument.getElementById("annual-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are opted out of Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)"
        pageDocument.getElementById("annual-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Annual) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }
    }

    "CY Status is Voluntary/Mandated and CY+1 is Annual" should {
      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the quarterly updates subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-quarterly-heading").text() shouldBe "Quarterly updates due"
      }

      "display the quarterly updates description for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-quarterly-desc").text() shouldBe "Every 3 months an update is due for each of your property and sole trader income sources."
      }

      "display the dropdown heading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-details__summary-text").text() shouldBe "Find out more about quarterly updates"
      }

      "display the description in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-dropdown-desc").text() shouldBe "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"
      }

      "display the bullet points in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "new information and corrections made since the last update any information you’ve already provided that has not changed"
      }

      "display the description containing the compatible software link within the dropdown in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-dropdown-desc2").text() shouldBe "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link-dropdown").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the upcoming deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-quarterly-table-heading").text() shouldBe "Upcoming deadlines"
      }

      "display the tax year summary description for the current year tab - individuals" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }

      "display the tax year summary description for the current year tab - agents" in new Setup(isAgent = true, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showAgentTaxYears().url
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-return-due-heading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-return-due-desc").text() shouldBe "If you have submitted quarterly updates for the tax year, in your tax return you will also provide any other taxable income. You will then need to file your return using software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct table headings for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("table-head-name-deadline").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("quarterly-deadline-date-0").text() shouldBe "30 Oct 2017"
        pageDocument.getElementById("quarterly-period-0").text() shouldBe "1 Jul 2017 to 30 Sep 2017"
        pageDocument.getElementById("quarterly-income-sources-0").text() shouldBe "Business income"
        pageDocument.getElementById("quarterly-deadline-date-1").text() shouldBe "31 Oct 2017"
        pageDocument.getElementById("quarterly-period-1").text() shouldBe "6 Apr 2017 to 5 Apr 2018"
        pageDocument.getElementById("quarterly-income-sources-1").text() shouldBe "Business income"
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Voluntary, Annual) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are opted out of Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)"
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
        pageDocument.getElementById("current-year-compatible-software-desc").text() shouldBe "As you are opted out of Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)"
        pageDocument.getElementById("annual-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are either required or voluntarily signed up for the 2026 to 2027 tax year, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Annual, Voluntary) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }
    }

    "CY Status is Voluntary/Mandated and CY+1 is Voluntary/Mandated" should {

      "display the correct heading for the current year" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-heading").text() shouldBe "Up to 2025 to 2026 tax year"
      }

      "display the description for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-desc").text() shouldBe "This page shows your upcoming due dates and any missed deadlines."
      }

      "display the quarterly updates subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-quarterly-heading").text() shouldBe "Quarterly updates due"
      }

      "display the quarterly updates description for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-quarterly-desc").text() shouldBe "Every 3 months an update is due for each of your property and sole trader income sources."
      }

      "display the dropdown heading for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementsByClass("govuk-details__summary-text").text() shouldBe "Find out more about quarterly updates"
      }

      "display the description in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-dropdown-desc").text() shouldBe "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"
      }

      "display the bullet points in the dropdown for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "new information and corrections made since the last update any information you’ve already provided that has not changed"
      }

      "display the description containing the compatible software link within the dropdown in the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-dropdown-desc2").text() shouldBe "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link-dropdown").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the upcoming deadlines subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-quarterly-table-heading").text() shouldBe "Upcoming deadlines"
      }

      "display the tax year summary description for the current year tab - individuals" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }

      "display the tax year summary description for the current year tab - agents" in new Setup(isAgent = true, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-tax-year-summary-desc").text() shouldBe "To view previously submitted updates visit the tax year summary page."
        pageDocument.getElementById("tax-year-summary-link").attr("href") shouldBe controllers.routes.TaxYearsController.showAgentTaxYears().url
      }

      "display the tax return due subheading for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-return-due-heading").text() shouldBe "Tax return due"
      }

      "display the description containing the compatible software link in the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-return-due-desc").text() shouldBe "If you have submitted quarterly updates for the tax year, in your tax return you will also provide any other taxable income. You will then need to file your return using software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the correct tax return due date for the 2025-26 tax year in the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("current-year-return-due-date").text() shouldBe "Your return for the 2025 to 2026 tax year is due by 31 January 2027."
      }

      "display the correct table headings for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("table-head-name-deadline").text() shouldBe "Deadline"
        pageDocument.getElementById("table-head-name-period").text() shouldBe "Period"
        pageDocument.getElementById("table-head-name-updates-due").text() shouldBe "Income source updates due"
      }

      "display the correct table content for the current year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("quarterly-deadline-date-0").text() shouldBe "30 Oct 2017"
        pageDocument.getElementById("quarterly-period-0").text() shouldBe "1 Jul 2017 to 30 Sep 2017"
        pageDocument.getElementById("quarterly-income-sources-0").text() shouldBe "Business income"
        pageDocument.getElementById("quarterly-deadline-date-1").text() shouldBe "31 Oct 2017"
        pageDocument.getElementById("quarterly-period-1").text() shouldBe "6 Apr 2017 to 5 Apr 2018"
        pageDocument.getElementById("quarterly-income-sources-1").text() shouldBe "Business income"
      }

      "display the correct heading for the next year" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-heading").text() shouldBe "2026 to 2027 tax year"
      }

      "display the description containing the compatible software link in the next year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-desc").text() shouldBe "As you are either required or voluntarily signed up for the 2026 to 2027 tax year, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)"
        pageDocument.getElementById("quarterly-compatible-software-link-ny").attr("href") shouldBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      }

      "display the deadline description for the next year tab" in new Setup(isAgent = false, obligationsModel, Mandated, Mandated) {
        pageDocument.getElementById("next-year-desc2").text() shouldBe "Deadlines will not be visible until it becomes the current tax year."
      }

    }
  }
}

