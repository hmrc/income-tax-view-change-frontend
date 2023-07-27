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

package views.incomeSources.add

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.incomeSources.add.ForeignPropertyAddedObligations

import java.time.LocalDate

class ForeignPropertyAddedObligationsViewSpec extends ViewSpec{

  object ForeignPropertyAddedMessages {
    val h1: String = "Foreign property"
    val h2: String = "has been added to your account"
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

  val testId: String = "XAIS00000000005"
  val backUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(testId).url
  val agentBackUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(testId).url

  val foreignPropertyAddedView: ForeignPropertyAddedObligations = app.injector.instanceOf[ForeignPropertyAddedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day = LocalDate.of(2022,1,1)

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

  val validCall: Html = foreignPropertyAddedView(viewModel, testCall, backUrl, isAgent = false)
  val validAgentCall: Html = foreignPropertyAddedView(viewModel, testCall, agentBackUrl, isAgent = true)

  val validCallWithData: Html = foreignPropertyAddedView(viewModelWithAllData, testCall, backUrl, isAgent = false)
  val validAgentCallWithData: Html = foreignPropertyAddedView(viewModelWithAllData, testCall, agentBackUrl, isAgent = true)

  "Foreign Property Added Obligations - Individual" should {
    "Display the correct banner message and heading" in new Setup(validCall) {
      val banner: Element = layoutContent.getElementsByTag("h1").first()
      banner.text() shouldBe ForeignPropertyAddedMessages.h1


      val subText: Option[Element] = layoutContent.select("div").eq(3)

      subText match {
        case Some(heading) => heading.text shouldBe ForeignPropertyAddedMessages.h1 + " " + ForeignPropertyAddedMessages.h2
        case _ => fail("No 2nd h2 element found.")
      }

      val subHeading: Element = layoutContent.getElementsByTag("h2").last()
      subHeading.text shouldBe ForeignPropertyAddedMessages.h2Content
    }

    "Display quarterly obligations if the user has them" in new Setup(validCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include (ForeignPropertyAddedMessages.quarterlyHeading)
      quarterlySection.text() should include (ForeignPropertyAddedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include (ForeignPropertyAddedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include (ForeignPropertyAddedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include (ForeignPropertyAddedMessages.tableHeading2)


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include ("1 January 2022 to 2 January 2022")
      tableContent.text() should include ("3 January 2022")

      tableContent.text() should include ("1 January 2023 to 2 January 2023")
      tableContent.text() should include ("3 January 2023")
    }

    "Display EOPS obligations if the user has them" in new Setup(validCallWithData) {
      val eopsSection: Element = layoutContent.getElementById("eops")
      eopsSection.text() should include (ForeignPropertyAddedMessages.eopsHeading)
      eopsSection.text() should include (ForeignPropertyAddedMessages.eopsText)

      val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include (ForeignPropertyAddedMessages.tableHeading1)
      tableHeadings.text() should include (ForeignPropertyAddedMessages.tableHeading2)

      val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include ("2022 to 2022")
      tableContent.text() should include ("3 January 2022")
    }

    "Display final declaration obligations if the user has them" in new Setup(validCallWithData) {
      val finalDecSection: Element = layoutContent.getElementById("finalDeclaration")
      finalDecSection.text() should include (ForeignPropertyAddedMessages.finalDecHeading)
      finalDecSection.text() should include (ForeignPropertyAddedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading1)
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Display previous tax year message" in new Setup(validCallWithData) {
      val prevYearsSection: Element = layoutContent.getElementById("prevYears")
      prevYearsSection.text() should include (ForeignPropertyAddedMessages.prevYearsHeading)
      prevYearsSection.text() should include (ForeignPropertyAddedMessages.prevYearsText)
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("eops")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the your income sources button" in new Setup(validCallWithData) {
      document.getElementById("continue-button").text() shouldBe (ForeignPropertyAddedMessages.buttonText)
    }
  }

  "Foreign Property Added Obligations - Agent" should {
    "Display the correct banner message and heading" in new Setup(validAgentCallWithData) {
      val banner: Element = layoutContent.getElementsByTag("h1").first()
      banner.text() shouldBe ForeignPropertyAddedMessages.h1

      val subText: Option[Element] = layoutContent.select("div").eq(3)

      subText match {
        case Some(heading) => heading.text shouldBe ForeignPropertyAddedMessages.h1 + " " + ForeignPropertyAddedMessages.h2
        case _ => fail("No 2nd h2 element found.")
      }

      val subHeading: Element = layoutContent.getElementsByTag("h2").last()
      subHeading.text shouldBe ForeignPropertyAddedMessages.h2Content
    }

    "Display quarterly obligations if the user has them" in new Setup(validAgentCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(ForeignPropertyAddedMessages.quarterlyHeading)
      quarterlySection.text() should include(ForeignPropertyAddedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading2)


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("1 January 2022 to 2 January 2022")
      tableContent.text() should include("3 January 2022")

      tableContent.text() should include("1 January 2023 to 2 January 2023")
      tableContent.text() should include("3 January 2023")
    }

    "Display EOPS obligations if the user has them" in new Setup(validAgentCallWithData) {
      val eopsSection: Element = layoutContent.getElementById("eops")
      eopsSection.text() should include(ForeignPropertyAddedMessages.eopsHeading)
      eopsSection.text() should include(ForeignPropertyAddedMessages.eopsText)

      val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading1)
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading2)

      val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Display final declaration obligations if the user has them" in new Setup(validAgentCallWithData) {
      val finalDecSection: Element = layoutContent.getElementById("finalDeclaration")
      finalDecSection.text() should include(ForeignPropertyAddedMessages.finalDecHeading)
      finalDecSection.text() should include(ForeignPropertyAddedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading1)
      tableHeadings.text() should include(ForeignPropertyAddedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Display previous tax year message" in new Setup(validAgentCallWithData) {
      val prevYearsSection: Element = layoutContent.getElementById("prevYears")
      prevYearsSection.text() should include(ForeignPropertyAddedMessages.prevYearsHeading)
      prevYearsSection.text() should include(ForeignPropertyAddedMessages.prevYearsText)
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validAgentCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("eops")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the your income sources button" in new Setup(validAgentCallWithData) {
      document.getElementById("continue-button").text() shouldBe (ForeignPropertyAddedMessages.buttonText)
    }
  }

}
