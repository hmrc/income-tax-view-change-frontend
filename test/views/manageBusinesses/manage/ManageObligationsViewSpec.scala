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

import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesFull
import testUtils.ViewSpec
import views.html.manageBusinesses.manage.ManageObligationsView

import java.time.LocalDate

class ManageObligationsViewSpec extends ViewSpec {

  object ManageObligationsMessages {
    val willReport: String = "will report"
    val forTaxYear: String = "for the tax year"
    val to: String = "to"
    val h2: String = "What you must do"
    val quarterlyHeading: String = "Send quarterly updates"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val buttonText: String = "Your income sources"
    val headingR17 = "Your revised deadlines"
  }

  val view: ManageObligationsView = app.injector.instanceOf[ManageObligationsView]

  val day: LocalDate = LocalDate.of(2022, 1, 1)

  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val taxYear: TaxYear = TaxYear(2023, 2024)
  val taxYearString: String = "2023-2024"
  val quarterly = "quarterly"
  val annually = "annual"

  val emptyViewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)


  val validSECallWithName: Html = view(viewModelWithAllData, "test name", taxYear, quarterly, isAgent = false, testCall, showOptInOptOutContentUpdateR17 = false, isCurrentTaxYear = false)
  val validSECallNoName: Html = view(viewModelWithAllData, "Not Found", taxYear, annually, isAgent = false, testCall, showOptInOptOutContentUpdateR17 = false, isCurrentTaxYear = false)
  val validUKCall: Html = view(viewModelWithAllData, "UK property", taxYear, quarterly, isAgent = false, testCall, showOptInOptOutContentUpdateR17 = false, isCurrentTaxYear = false)
  val validFPCall: Html = view(viewModelWithAllData, "Foreign property", taxYear, annually, isAgent = false, testCall, showOptInOptOutContentUpdateR17 = false, isCurrentTaxYear = false)
  val validCallNoData: Html = view(emptyViewModel, "test name", taxYear, quarterly, isAgent = false, testCall, showOptInOptOutContentUpdateR17 = false, isCurrentTaxYear = false)
  val quarterlyCy: Html = view(viewModelWithAllData, "test name", TaxYear(2024, 2025), "quarterly", isAgent = false, testCall, showOptInOptOutContentUpdateR17 = true, isCurrentTaxYear = true)
  val quarterlyCyPlus1: Html = view(viewModelWithAllData, "test name", TaxYear(2025, 2026), "quarterly", isAgent = false, testCall, showOptInOptOutContentUpdateR17 = true, isCurrentTaxYear = false)
  val annualCy: Html = view(viewModelWithAllData, "test name", TaxYear(2024, 2025), "annual", isAgent = false, testCall, showOptInOptOutContentUpdateR17 = true, isCurrentTaxYear = false)
  val annualCyPlus1: Html = view(viewModelWithAllData, "test name", TaxYear(2025, 2026), "annual", isAgent = false, testCall, showOptInOptOutContentUpdateR17 = true, isCurrentTaxYear = false)

  //Testing banner for each mode/scenario, obligations are displayed the same for each so will only be tested once

  "Manage Obligations page" should {
    "Display the correct banner and heading" when {
      "Called on self employment with a name" in new Setup(validSECallWithName) {
        panelText shouldBe "test name will report quarterly for the tax year 2023 to 2024"
        firstH2Text shouldBe ManageObligationsMessages.h2
      }
      "Called on self employment with no name" in new Setup(validSECallNoName) {
        panelText shouldBe "Sole trader business will report annually for the tax year 2023 to 2024"
        firstH2Text shouldBe ManageObligationsMessages.h2
      }
      "Called on UK property" in new Setup(validUKCall) {
        panelText shouldBe "UK property will report quarterly for the tax year 2023 to 2024"
        firstH2Text shouldBe ManageObligationsMessages.h2
      }
      "Called on foreign property" in new Setup(validFPCall) {
        panelText shouldBe "Foreign property will report annually for the tax year 2023 to 2024"
        firstH2Text shouldBe ManageObligationsMessages.h2
      }
    }
    "Not display a back button" in new Setup(validSECallWithName) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "Display quarterly obligations if the user has them" in new Setup(validSECallWithName) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(ManageObligationsMessages.quarterlyHeading)
      quarterlySection.text() should include(ManageObligationsMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading1 + " 2024 to 2025")
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading1 + " ")


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("6 January 2022 to 5 April 2022")
      tableContent.text() should include("5 May 2022")

      tableContent.text() should include("6 January 2023 to 5 April 2023")
      tableContent.text() should include("5 May 2023")

      tableContent.text() should include("6 January 2024 to 5 April 2024")
      tableContent.text() should include("5 May 2024")
    }

    "Display final declaration obligations if the user has them" in new Setup(validSECallWithName) {
      val finalDecSection: Element = layoutContent.getElementById("finalDec")
      finalDecSection.text() should include(ManageObligationsMessages.finalDecHeading)
      finalDecSection.text() should include(ManageObligationsMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading1)
      tableHeadings.text() should include(ManageObligationsMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validCallNoData) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render OptInOptOutContentUpdateR17 content correctly" when {
      "panel body includes 'signed up to' when user switches to Quarterly" in new Setup(quarterlyCy) {
        panelText should include("signed up to")
      }

      "panel body includes 'opted out of' when user switches to Annual" in new Setup(annualCy) {
        panelText should include("opted out of")
      }

      "user switches to Quarterly in CY with overdue updates" in new Setup(quarterlyCy) {
        paragraphText should include("Even if they are not displayed right away")
        paragraphText should include("you may have overdue updates")
        paragraphText should include("View your businesses to add")
      }

      "user switches to Quarterly in CY+1" in new Setup(quarterlyCyPlus1) {
        paragraphText should include("Even if they are not displayed right away")
        paragraphText should not include "you may have overdue updates"
        paragraphText should include("View your businesses to add")
      }

      "user switches to Annual in CY" in new Setup(annualCy) {
        paragraphText should include("Even if they are not displayed right away")
        paragraphText should not include "you may have overdue updates"
        paragraphText should include("View your businesses to add")
      }

      "user switches to Annual in CY+1" in new Setup(annualCyPlus1) {
        paragraphText should include("Even if they are not displayed right away")
        paragraphText should not include "you may have overdue updates"
        paragraphText should include("View your businesses to add")
      }
    }
  }
}
