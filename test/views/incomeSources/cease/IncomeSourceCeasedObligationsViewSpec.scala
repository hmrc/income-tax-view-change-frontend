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

package views.incomeSources.cease

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesFull
import testUtils.ViewSpec
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import java.time.LocalDate

class IncomeSourceCeasedObligationsViewSpec extends ViewSpec {

  object IncomeSourceCeasedMessages {
    val h1ForeignProperty: String = "Foreign property"
    val h1UKProperty: String = "UK property"
    val h1SelfEmployment: String = "Test Name"
    val h1SelfEmploymentIfEmpty: String = "Sole trader business"
    val headingBase: String = "has ceased"
    val h2Content: String = "What you must do"
    val quarterlyHeading: String = "Send quarterly updates"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val buttonText: String = "Your income sources"
  }

  val testId: String = "XAIS00000000005"

  val view: IncomeSourceCeasedObligations = app.injector.instanceOf[IncomeSourceCeasedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day = LocalDate.of(2022, 1, 1)

  val finalDeclarationDates = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val validUKPropertyBusinessCall: Html = view(sources = viewModel, isAgent = false, incomeSourceType = UkProperty, businessName = None)
  val validUKPropertyBusinessAgentCall: Html = view(sources = viewModel, isAgent = true, incomeSourceType = UkProperty, businessName = None)

  val validForeignPropertyBusinessCall: Html = view(sources = viewModel, isAgent = false, incomeSourceType = ForeignProperty, businessName = None)
  val validForeignPropertyBusinessAgentCall: Html = view(sources = viewModel, isAgent = true, incomeSourceType = ForeignProperty, businessName = None)

  val validSoleTreaderBusinessCall: Html = view(sources = viewModel, isAgent = false, incomeSourceType = SelfEmployment, businessName = Some("Test Name"))
  val validSoleTreaderBusinessAgentCall: Html = view(sources = viewModel, isAgent = true, incomeSourceType = SelfEmployment, businessName = Some("Test Name"))

  val validSoleTreaderBusinessWithNoBusinessNameCall: Html = view(sources = viewModel, isAgent = false, incomeSourceType = SelfEmployment, businessName = None)
  val validSoleTreaderBusinessWithNoBusinessNameAgentCall: Html =
    view(sources = viewModel, isAgent = true, incomeSourceType = SelfEmployment, businessName = None)

  val validCallWithData: Html = view(sources = viewModelWithAllData, isAgent = false, incomeSourceType = SelfEmployment, businessName = Some("Test Name"))
  val validAgentCallWithData: Html = view(sources = viewModelWithAllData, isAgent = true, incomeSourceType = SelfEmployment, businessName = Some("Test Name"))

  val ceaseIncomeSourceShowURL = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
  val ceaseIncomeSourceShowAgentURL = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url

  "Income Source Ceased Obligations - Individual" should {
    "Display the correct banner message and heading" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1UKProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1UKProperty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1ForeignProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1ForeignProperty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Self Employment Business" in new Setup(validSoleTreaderBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1SelfEmployment


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1SelfEmployment + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Self Employment Business and business name is empty" in new Setup(validSoleTreaderBusinessWithNoBusinessNameCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1SelfEmploymentIfEmpty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1SelfEmploymentIfEmpty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
    }
    "Not display a back button" in new Setup(validCallWithData) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }

    "Display quarterly obligations if the user has them" in new Setup(validCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(IncomeSourceCeasedMessages.quarterlyHeading)
      quarterlySection.text() should include(IncomeSourceCeasedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2024 to 2025")

      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading2)


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
      finalDecSection.text() should include(IncomeSourceCeasedMessages.finalDecHeading)
      finalDecSection.text() should include(IncomeSourceCeasedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("1 January 2022 to 2 January 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
    }

    "render the your income sources button" in new Setup(validCallWithData) {
      document.getElementById("continue-button").text() shouldBe (IncomeSourceCeasedMessages.buttonText)
    }

    s"has GET form action to $ceaseIncomeSourceShowURL" in new Setup(validCallWithData) {
      document.getElementsByTag("form").get(0).attr("method") shouldBe "GET"
      document.getElementsByTag("form").get(0).attr("action") shouldBe ceaseIncomeSourceShowURL
    }
  }

  "Income Source Ceased Obligations - Agent" should {
    "Display the correct banner message and heading" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1UKProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1UKProperty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1ForeignProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1ForeignProperty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Self Employment Business" in new Setup(validSoleTreaderBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1SelfEmployment


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1SelfEmployment + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
      "Business type is Self Employment Business and business name is empty" in new Setup(validSoleTreaderBusinessWithNoBusinessNameAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceCeasedMessages.h1SelfEmploymentIfEmpty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceCeasedMessages.h1SelfEmploymentIfEmpty + " " + IncomeSourceCeasedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceCeasedMessages.h2Content
      }
    }

    "Display quarterly obligations if the user has them" in new Setup(validCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include(IncomeSourceCeasedMessages.quarterlyHeading)
      quarterlySection.text() should include(IncomeSourceCeasedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1 + " 2024 to 2025")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading2)


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
      finalDecSection.text() should include(IncomeSourceCeasedMessages.finalDecHeading)
      finalDecSection.text() should include(IncomeSourceCeasedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading1)
      tableHeadings.text() should include(IncomeSourceCeasedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("1 January 2022 to 2 January 2022 3 January 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessAgentCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
    }

    "render the your income sources button" in new Setup(validAgentCallWithData) {
      document.getElementById("continue-button").text() shouldBe (IncomeSourceCeasedMessages.buttonText)
    }

    s"has GET form action to $ceaseIncomeSourceShowAgentURL" in new Setup(validAgentCallWithData) {
      document.getElementsByTag("form").get(0).attr("action") shouldBe ceaseIncomeSourceShowAgentURL
      document.getElementsByTag("form").get(0).attr("method") shouldBe "GET"
    }
  }

}
