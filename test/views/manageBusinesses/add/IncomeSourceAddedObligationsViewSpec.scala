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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._
import testUtils.ViewSpec
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import java.time.LocalDate

class IncomeSourceAddedObligationsViewSpec extends ViewSpec {

  object IncomeSourceAddedMessages {
    val h1ForeignProperty: String = "Foreign property"
    val h1UKProperty: String = "UK property"
    val h1SelfEmployment: String = "Test Name"
    val headingBase: String = "has been added to your account"
    val h2Content: String = "What you must do"
    val quarterlyHeading: String = "Send quarterly updates"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val viewAllBusinessesText: String = "View all your businesses"
    val insetSingleOverdueUpdateText: (String, Int) => String = (month, year) => s"As your business started in $month $year, you have 1 overdue update."
    val insetMultipleOverdueUpdateText: (String, Int, Int) => String = (month, year, overdueUpdates) => s"As your business started in $month $year, you have $overdueUpdates overdue updates."
    val insetWarningOverdueUpdatesText: Int => String = startTaxYear => s"You must make sure that you have sent all the required income and expenses for tax years earlier than $startTaxYear to ${startTaxYear + 1}."
  }

  val testId: String = "XAIS00000000005"

  val view: IncomeSourceAddedObligations = app.injector.instanceOf[IncomeSourceAddedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day = LocalDate.of(2022, 1, 1)
  val dayForOverdueObligations = LocalDate.of(2022, 8, 6)

  val finalDeclarationDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")
  val multipleFinalDeclarationDates = Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised"),
    DatesModel(day.plusYears(1), day.plusYears(1).plusDays(1), day.plusYears(1).plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised"))

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithSingleQuarterlyOverdue: ObligationsViewModel = ObligationsViewModel(
    Seq(quarterlyDatesYearOneSimple),
    Seq(),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithSameYearQuarterlyOverdue: ObligationsViewModel = ObligationsViewModel(
    singleYearTwoQuarterlyDates,
    Seq(),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithCurrentAndPreviousYearsQuarterlyOverdue: ObligationsViewModel = ObligationsViewModel(
    currentAndPreviousYearsQuarterlyObligationDates,
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithPreviousYearsQuarterlyOverdue: ObligationsViewModel = ObligationsViewModel(
    previousYearsQuarterlyObligationDates,
    multipleFinalDeclarationDates,
    2025,
    showPrevTaxYears = true
  )

  val viewModelWithSingleOverdueAnnualObligation: ObligationsViewModel = ObligationsViewModel(
    Seq(),
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithMultipleOverdueAnnualObligations: ObligationsViewModel = ObligationsViewModel(
    Seq(),
    multipleFinalDeclarationDates,
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithHybridReportingOverdue: ObligationsViewModel = ObligationsViewModel(
    Seq(quarterlyDatesYearOne),
    multipleFinalDeclarationDates,
    2023,
    showPrevTaxYears = true
  )

  val emptyOverdueObligationsContent: (String, Seq[String]) = ("", Seq())

  val validUKPropertyBusinessCall: Html = view(viewModel, isAgent = false, UkProperty, None, day, isBusinessHistoric = false)
  val validUKPropertyBusinessAgentCall: Html = view(viewModel, isAgent = true, UkProperty, None, day, isBusinessHistoric = false)

  val validForeignPropertyBusinessCall: Html = view(viewModel, isAgent = false, ForeignProperty, None, day, isBusinessHistoric = false)
  val validForeignPropertyBusinessAgentCall: Html = view(viewModel, isAgent = true, ForeignProperty, None, day, isBusinessHistoric = false)

  val validSoleTreaderBusinessCall: Html = view(viewModel, isAgent = false, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false)
  val validSoleTreaderBusinessAgentCall: Html = view(viewModel, isAgent = true, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false)

  val validCallWithData: Html = view(viewModelWithAllData, isAgent = false, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false)
  val validAgentCallWithData: Html = view(viewModelWithAllData, isAgent = true, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false)

  val validCallWithData2: Html = view(viewModelWithCurrentAndPreviousYearsQuarterlyOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)
  val validCallWithData3: Html = view(viewModelWithPreviousYearsQuarterlyOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = true)

  val validAnnualCallWithData: Html = view(viewModelWithSingleOverdueAnnualObligation, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)
  val validAnnualCallWithData2: Html = view(viewModelWithMultipleOverdueAnnualObligations, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)

  val validCallWithSingleQuarterlyOverdue: Html = view(viewModelWithSingleQuarterlyOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)
  val validCallWithSameYearQuarterlyOverdue: Html = view(viewModelWithSameYearQuarterlyOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)

  val validHistoricCallWithData: Html = view(viewModelWithHybridReportingOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)

  val validHybridCallWithData: Html = view(viewModelWithHybridReportingOverdue, isAgent = false, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)
  val validAgentHybridCallWithData: Html = view(viewModelWithHybridReportingOverdue, isAgent = true, SelfEmployment, Some("Test Name"), LocalDate.of(2024, 6, 1), isBusinessHistoric = false)

  val addIncomeSourceShowURL = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url
  val addIncomeSourceShowAgentURL = controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent().url

  val nextUpdatesUrl = controllers.routes.NextUpdatesController.show().url
  val nextUpdatesAgentUrl = controllers.routes.NextUpdatesController.showAgent.url

  "Income Source Added Obligations - Individual" should {
    "Display the correct banner message and heading" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1UKProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1UKProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1ForeignProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1ForeignProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
      "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1SelfEmployment


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1SelfEmployment + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
    }
    "Not display a back button" in new Setup(validCallWithData) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }

    "Not display inset warning text because there are no overdue obligations" in new Setup(validCallWithData) {
      withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
    }

    "Display the correct inset warning text" when {
      "There is a single overdue quartlerly obligation in the current tax year" in new Setup(validCallWithSingleQuarterlyOverdue) {
        Option(document.getElementById("warning-inset")) match {
          case Some(insetText) => insetText.text() shouldBe "You have 1 overdue update for the first 3 months of the 2022 to 2023 tax year. You must submit these updates with all required income and expenses through your compatible software."
          case None => fail("No inset text was found")
        }
      }

      "There are multiple overdue quarterly obligations in the current tax year" in new Setup(validCallWithSameYearQuarterlyOverdue) {
        Option(document.getElementById("warning-inset")) match {
          case Some(insetText) => insetText.text() shouldBe "You have 2 overdue updates for the first 6 months of the 2022 to 2023 tax year. You must submit these updates with all required income and expenses through your compatible software."
          case None => fail("No inset text was found")
        }
      }

//      "There are multiple overdue quarterly obligations across multiple tax years including the current tax year" in new Setup(validCallWithData2) {
//        Option(document.getElementById("warning-inset")) match {
//          case Some(insetText) => insetText.text() shouldBe "This test should fail."
//          case None => fail("No inset text was found")
//        }
//      }

      "There are multiple overdue quarterly obligations across multiple tax years not including the current tax year for a business with a historic start date" in new Setup(validCallWithData3) {
        Option(document.getElementById("warning-inset")) match {
          case Some(insetText) => insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
          case None => fail("No inset text was found")
        }
      }

      "There is a single overdue annual obligation in the previous tax year" in new Setup(validAnnualCallWithData) {
        Option(document.getElementById("warning-inset")) match {
          case Some(insetText) => insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
          case None => fail("No inset text was found")
        }
      }

      "There are multiple overdue annual obligations from previous tax years" in new Setup(validAnnualCallWithData2) {
        Option(document.getElementById("warning-inset")) match {
          case Some(insetText) => insetText.text() shouldBe "You have 2 overdue updates. You must submit your yearly tax return and pay the tax you owe."
          case None => fail("No inset text was found")
        }
      }
    }

    "Display the correct view overdue and upcoming updates link if the user has them" in new Setup(validHybridCallWithData) {
      Option(document.getElementById("view-overdue-upcoming-updates")) match {
        case Some(link) =>
          link.text() shouldBe "View your overdue and upcoming updates"
          link.getElementsByTag("a").first().attr("href") shouldBe nextUpdatesUrl
        case None => fail("No link was found")
      }
    }
    "Not display the view overdue and upcoming updates link when the user does not have them" in new Setup(validCallWithData) {
      Option(document.getElementById("view-overdue-upcoming-updates")).isDefined shouldBe false
    }

    "Display quarterly obligations if the user has them" in new Setup(validCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(IncomeSourceAddedMessages.quarterlyHeading)
      quarterlySection.text() should include(IncomeSourceAddedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2024 to 2025")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("6 January 2022 to 5 April 2022")
      tableContent.text() should include("5 May 2022")

      tableContent.text() should include("6 January 2023 to 5 April 2023")
      tableContent.text() should include("5 May 2023")

      tableContent.text() should include("6 January 2024 to 5 April 2024")
      tableContent.text() should include("5 May 2024")
    }

    "Display final declaration obligations if the user has them" in new Setup(validCallWithData) {
      val finalDecSection: Element = layoutContent.getElementById("finalDeclaration")
      finalDecSection.text() should include(IncomeSourceAddedMessages.finalDecHeading)
      finalDecSection.text() should include(IncomeSourceAddedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Display previous tax year message" in new Setup(validCallWithData) {
      val prevYearsSection: Element = layoutContent.getElementById("prevYears")
      prevYearsSection.text() should include(IncomeSourceAddedMessages.prevYearsHeading)
      prevYearsSection.text() should include(IncomeSourceAddedMessages.prevYearsText)
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the view all your business link" in new Setup(validCallWithData) {
      document.getElementById("view-all-businesses-link").text() shouldBe IncomeSourceAddedMessages.viewAllBusinessesText
    }

    s"has GET form action to $addIncomeSourceShowURL" in new Setup(validCallWithData) {
      document.getElementsByTag("form").get(0).attr("action") shouldBe addIncomeSourceShowURL
      document.getElementsByTag("form").get(0).attr("method") shouldBe "GET"
    }
  }

  "Income Source Added Obligations - Agent" should {
    "Display the correct banner message and heading" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1UKProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1UKProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1ForeignProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1ForeignProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
      "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1SelfEmployment


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1SelfEmployment + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.h2Content
      }
    }

    "Display the correct view overdue and upcoming updates link if the user has them" in new Setup(validAgentHybridCallWithData) {
      Option(document.getElementById("view-overdue-upcoming-updates")) match {
        case Some(link) =>
          link.text() shouldBe "View your overdue and upcoming updates"
          link.getElementsByTag("a").first().attr("href") shouldBe nextUpdatesAgentUrl
        case None => fail("No link was found")
      }
    }
    "Not display the view overdue and upcoming updates link when the user does not have them" in new Setup(validAgentCallWithData) {
      Option(document.getElementById("view-overdue-upcoming-updates")).isDefined shouldBe false
    }

    "Display quarterly obligations if the user has them" in new Setup(validAgentCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(IncomeSourceAddedMessages.quarterlyHeading)
      quarterlySection.text() should include(IncomeSourceAddedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1 + " 2024 to 2025")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("6 January 2022 to 5 April 2022")
      tableContent.text() should include("5 May 2022")

      tableContent.text() should include("6 January 2023 to 5 April 2023")
      tableContent.text() should include("5 May 2023")

      tableContent.text() should include("6 January 2024 to 5 April 2024")
      tableContent.text() should include("5 May 2024")
    }

    "Display final declaration obligations if the user has them" in new Setup(validAgentCallWithData) {
      val finalDecSection: Element = layoutContent.getElementById("finalDeclaration")
      finalDecSection.text() should include(IncomeSourceAddedMessages.finalDecHeading)
      finalDecSection.text() should include(IncomeSourceAddedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }


    "Display previous tax year message" in new Setup(validAgentCallWithData) {
      val prevYearsSection: Element = layoutContent.getElementById("prevYears")
      prevYearsSection.text() should include(IncomeSourceAddedMessages.prevYearsHeading)
      prevYearsSection.text() should include(IncomeSourceAddedMessages.prevYearsText)
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessAgentCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the view all your businesses link" in new Setup(validAgentCallWithData) {
      document.getElementById("view-all-businesses-link").text() shouldBe IncomeSourceAddedMessages.viewAllBusinessesText
    }

    s"has GET form action to $addIncomeSourceShowAgentURL" in new Setup(validAgentCallWithData) {
      document.getElementsByTag("form").get(0).attr("action") shouldBe addIncomeSourceShowAgentURL
      document.getElementsByTag("form").get(0).attr("method") shouldBe "GET"
    }
  }

}
