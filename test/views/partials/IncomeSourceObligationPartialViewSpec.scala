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

package views.partials

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.partials.IncomeSourceObligationsPartial

import java.time.LocalDate


class IncomeSourceObligationPartialViewSpec extends ViewSpec {

  object IncomeSourceObligationMessages {
    val h1ForeignProperty: String = "Foreign property"
    val h1UKProperty: String = "UK property"
    val h1SoleTreaderProperty: String = "Test Name"
    val headingBase: String = "has been added to your account"
    val h2Content: String = "What you must do"
    val quarterlyHeading: String = "Send quarterly updates"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val eopsHeading: String = "Send end of period statements"
    val eopsText: String = "You must submit end of period statements using your software by the deadline."
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val buttonText: String = "Your income sources"
  }

  val day = LocalDate.of(2022, 1, 1)

  lazy val incomeSourceObligationsPartial = app.injector.instanceOf[IncomeSourceObligationsPartial]
  val quarterlyDatesYearOne = DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false)
  val quarterlyDatesYearTwo = DatesModel(day.plusYears(1), day.plusYears(1).plusDays(1), day.plusYears(1).plusDays(2), "#001", isFinalDec = false)
  val eopsDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false)
  val finalDeclarationDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true)


  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    Seq(quarterlyDatesYearOne),
    Seq(quarterlyDatesYearTwo),
    Seq(eopsDates),
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val obligationDataView: Html = incomeSourceObligationsPartial(viewModelWithAllData)
  val noObligationDataView: Html = incomeSourceObligationsPartial(viewModel)

  "Display quarterly obligations if the user has them" in new Setup(obligationDataView) {
    val quarterlySection: Element = document.getElementById("quarterly")
    quarterlySection.text() should include(IncomeSourceObligationMessages.quarterlyHeading)
    quarterlySection.text() should include(IncomeSourceObligationMessages.quarterlyText)

    val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading1 + " 2022 to 2023")
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading1 + " 2023 to 2024")
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading2)


    val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
    tableContent.text() should include("1 January 2022 to 2 January 2022")
    tableContent.text() should include("3 January 2022")

    tableContent.text() should include("1 January 2023 to 2 January 2023")
    tableContent.text() should include("3 January 2023")
  }

  "Display EOPS obligations if the user has them" in new Setup(obligationDataView) {
    val eopsSection: Element = document.getElementById("eops")
    eopsSection.text() should include(IncomeSourceObligationMessages.eopsHeading)
    eopsSection.text() should include(IncomeSourceObligationMessages.eopsText)

    val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading1)
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading2)

    val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
    tableContent.text() should include("2022 to 2022")
    tableContent.text() should include("3 January 2022")
  }

  "Display final declaration obligations if the user has them" in new Setup(obligationDataView) {
    val finalDecSection: Element = document.getElementById("finalDeclaration")
    finalDecSection.text() should include(IncomeSourceObligationMessages.finalDecHeading)
    finalDecSection.text() should include(IncomeSourceObligationMessages.finalDecText)

    val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading1)
    tableHeadings.text() should include(IncomeSourceObligationMessages.tableHeading2)

    val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
    tableContent.text() should include("2022 to 2022")
    tableContent.text() should include("3 January 2022")
  }

  "Display previous tax year message" in new Setup(obligationDataView) {
    val prevYearsSection: Element = document.getElementById("prevYears")
    prevYearsSection.text() should include(IncomeSourceObligationMessages.prevYearsHeading)
    prevYearsSection.text() should include(IncomeSourceObligationMessages.prevYearsText)
  }

  "Not display any obligation sections when user has no obligations" in new Setup(noObligationDataView) {
    Option(document.getElementById("quarterly")) shouldBe None
    Option(document.getElementById("eops")) shouldBe None
    Option(document.getElementById("prevyears")) shouldBe None
  }

}
