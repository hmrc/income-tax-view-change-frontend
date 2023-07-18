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
import play.api.Logger
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.incomeSources.add.BusinessAddedObligations

import java.time.LocalDate

class BusinessAddedObligationsViewSpec extends ViewSpec{

  object BusinessAddedMessages {
    val h1: String = "has been added to your account"
    val h2: String = "What you must do"
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

  val testId: String = "XAIS00000000001"
  val backUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(testId).url
  val agentBackUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(testId).url

  val businessAddedView: BusinessAddedObligations = app.injector.instanceOf[BusinessAddedObligations]

  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)
  val validCall: Html = businessAddedView("test name", viewModel, testCall, backUrl, isAgent = false)
  val validAgentCall: Html = businessAddedView("test name", viewModel, testCall, agentBackUrl, isAgent = true)

  val day = LocalDate.of(2022,1,1)
  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false)),
    Seq(DatesModel(day.plusYears(1), day.plusYears(1).plusDays(1), day.plusYears(1).plusDays(2), "#001", isFinalDec = false)),
    Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false)),
    Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true)),
    2023,
    showPrevTaxYears = true
  )
  val validCallWithData: Html = businessAddedView("test name", viewModelWithAllData, testCall, backUrl, isAgent = false)
  val validAgentCallWithData: Html = businessAddedView("test name", viewModelWithAllData, testCall, agentBackUrl, isAgent = true)

  "Business Added Obligations page" should {
    "Display the correct banner message and heading" in new Setup(validCall) {
      val banner: Element = layoutContent.getElementsByTag("h1").first()
      banner.text() shouldBe "test name"

      val heading: Element = layoutContent.getElementsByTag("h2").first()
      heading.text shouldBe BusinessAddedMessages.h2
    }

    "Display quarterly obligations if the user has them" in new Setup(validCallWithData) {
      val quarterlySection: Element = layoutContent.getElementById("quarterly")
      quarterlySection.text() should include (BusinessAddedMessages.quarterlyHeading)
      quarterlySection.text() should include (BusinessAddedMessages.quarterlyText)

      val tableHeadings: Elements = quarterlySection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include (BusinessAddedMessages.tableHeading1 + " 2022 to 2023")
      tableHeadings.text() should include (BusinessAddedMessages.tableHeading1 + " 2023 to 2024")
      tableHeadings.text() should include (BusinessAddedMessages.tableHeading2)


      val tableContent: Elements = quarterlySection.getElementsByClass("govuk-table__body")
      tableContent.text() should include ("1 January 2022 to 2 January 2022")
      tableContent.text() should include ("3 January 2022")

      tableContent.text() should include ("1 January 2023 to 2 January 2023")
      tableContent.text() should include ("3 January 2023")
    }

    "Display EOPS obligations if the user has them" in new Setup(validCallWithData) {
      val eopsSection: Element = layoutContent.getElementById("eops")
      eopsSection.text() should include (BusinessAddedMessages.eopsHeading)
      eopsSection.text() should include (BusinessAddedMessages.eopsText)

      val tableHeadings: Elements = eopsSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include (BusinessAddedMessages.tableHeading1)
      tableHeadings.text() should include (BusinessAddedMessages.tableHeading2)

      val tableContent: Elements = eopsSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include ("2022 to 2022")
      tableContent.text() should include ("3 January 2022")
    }

    "Display final declaration obligations if the user has them" in new Setup(validCallWithData) {
      val finalDecSection: Element = layoutContent.getElementById("finalDec")
      finalDecSection.text() should include (BusinessAddedMessages.finalDecHeading)
      finalDecSection.text() should include (BusinessAddedMessages.finalDecText)

      val tableHeadings: Elements = finalDecSection.getElementsByClass("govuk-table__head")
      tableHeadings.text() should include(BusinessAddedMessages.tableHeading1)
      tableHeadings.text() should include(BusinessAddedMessages.tableHeading2)

      val tableContent: Elements = finalDecSection.getElementsByClass("govuk-table__body")
      tableContent.text() should include("2022 to 2022")
      tableContent.text() should include("3 January 2022")
    }

    "Display previous tax year message" in new Setup(validCallWithData) {
      val prevYearsSection: Element = layoutContent.getElementById("prevYears")
      prevYearsSection.text() should include (BusinessAddedMessages.prevYearsHeading)
      prevYearsSection.text() should include (BusinessAddedMessages.prevYearsText)
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("eops")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }
  }

}
