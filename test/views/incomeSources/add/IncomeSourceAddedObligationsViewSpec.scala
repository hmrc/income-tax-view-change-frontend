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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesFull
import testUtils.ViewSpec
import views.html.incomeSources.add.IncomeSourceAddedObligations

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

  val view: IncomeSourceAddedObligations = app.injector.instanceOf[IncomeSourceAddedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day = LocalDate.of(2022, 1, 1)

  val eopsDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false, obligationType = "EOPS")
  val finalDeclarationDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(eopsDates),
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val validUKPropertyBusinessCall: Html = view(viewModel, isAgent = false, UkProperty, None)
  val validUKPropertyBusinessAgentCall: Html = view(viewModel, isAgent = true, UkProperty, None)

  val validForeignPropertyBusinessCall: Html = view(viewModel, isAgent = false, ForeignProperty, None)
  val validForeignPropertyBusinessAgentCall: Html = view(viewModel, isAgent = true, ForeignProperty, None)

  val validSoleTreaderBusinessCall: Html = view(viewModel, isAgent = false, SelfEmployment, Some("Test Name"))
  val validSoleTreaderBusinessAgentCall: Html = view(viewModel, isAgent = true, SelfEmployment, Some("Test Name"))

  val validCallWithData: Html = view(viewModelWithAllData, isAgent = false, SelfEmployment, Some("Test Name"))
  val validAgentCallWithData: Html = view(viewModelWithAllData, isAgent = true, SelfEmployment, Some("Test Name"))

  val addIncomeSourceShowURL = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  val addIncomeSourceShowAgentURL = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url

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

    "Display EOPS obligations if the user has them" in new Setup(validCallWithData) {
      val eopsSection: Element = layoutContent.getElementById("eops")
      eopsSection.text() should include(IncomeSourceAddedMessages.eopsHeading)
      eopsSection.text() should include(IncomeSourceAddedMessages.eopsText)

      val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)

      val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
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
      Option(layoutContent.getElementById("eops")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the your income sources button" in new Setup(validCallWithData) {
      document.getElementById("continue-button").text() shouldBe (IncomeSourceAddedMessages.buttonText)
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

    "Display EOPS obligations if the user has them" in new Setup(validAgentCallWithData) {
      val eopsSection: Element = layoutContent.getElementById("eops")
      eopsSection.text() should include(IncomeSourceAddedMessages.eopsHeading)
      eopsSection.text() should include(IncomeSourceAddedMessages.eopsText)

      val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceAddedMessages.tableHeading2)

      val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
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
      Option(layoutContent.getElementById("eops")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the your income sources button" in new Setup(validAgentCallWithData) {
      document.getElementById("continue-button").text() shouldBe (IncomeSourceAddedMessages.buttonText)
    }

    s"has GET form action to $addIncomeSourceShowAgentURL" in new Setup(validAgentCallWithData) {
      document.getElementsByTag("form").get(0).attr("action") shouldBe addIncomeSourceShowAgentURL
      document.getElementsByTag("form").get(0).attr("method") shouldBe "GET"
    }
  }

}
