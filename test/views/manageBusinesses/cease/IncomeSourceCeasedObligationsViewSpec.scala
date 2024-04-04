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

package views.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{DatesModel, IncomeSourceCeasedObligationsViewModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesFull
import testUtils.ViewSpec
import views.html.manageBusinesses.cease.IncomeSourceCeasedObligations

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
    val eopsHeading: String = "Send end of period statements"
    val eopsText: String = "You must submit end of period statements using your software by the deadline."
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val buttonText: String = "Your income sources"
    val yourNextUpComingUpdatesHeading = "Your next upcoming update"
  }


  val testId: String = "XAIS00000000005"

  val view: IncomeSourceCeasedObligations = app.injector.instanceOf[IncomeSourceCeasedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day: LocalDate = fixedDate
  val cessationDate: LocalDate = day.plusDays(1)

  val eopsDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false, obligationType = "EOPS")
  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")
  val finalDeclarationDates2: DatesModel = DatesModel(day.plusYears(1), day.plusDays(1).plusYears(1), day.plusDays(2).plusYears(1), "C", isFinalDec = true, obligationType = "Crystallised")

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(eopsDates),
    Seq(finalDeclarationDates, finalDeclarationDates2),
    2023,
    showPrevTaxYears = true
  )

  val viewModelAnnualObligation: ObligationsViewModel = viewModelWithAllData.copy(quarterlyObligationsDates = Seq.empty)

  val incomeSourceCeasedObligationsViewModel: IncomeSourceCeasedObligationsViewModel = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithAllData,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    cessationDate = cessationDate,
    businessName = None)

  val validUKPropertyBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(incomeSourceType = UkProperty))
  val validForeignPropertyBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(incomeSourceType = ForeignProperty))
  val validSoleTreaderBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(businessName = Some("Test Name")))
  val validSoleTreaderBusinessWithNoBusinessNameCall: Html = view(incomeSourceCeasedObligationsViewModel)
  val validCallWithData: Html = view(incomeSourceCeasedObligationsViewModel.copy(businessName = Some("Test Name")))
  val validCallWithNoQuarterlyDataOnlyOneFinalUpdate: Html = view(
    IncomeSourceCeasedObligationsViewModel(
      obligationsViewModel = viewModelAnnualObligation.copy(finalDeclarationDates = Seq(finalDeclarationDates)),
      isAgent = false,
      incomeSourceType = SelfEmployment,
      cessationDate = cessationDate,
      businessName = Some("Test Name")))
  val validCallWithNoQuarterlyDataMultipleFinalUpdates: Html = view(
    IncomeSourceCeasedObligationsViewModel(
      obligationsViewModel = viewModelAnnualObligation,
      isAgent = false,
      incomeSourceType = SelfEmployment,
      cessationDate = cessationDate,
      businessName = Some("Test Name")))

  val manageYourBusinessShowURL: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent = false).url
  val viewUpcomingUpdatesURL: String = controllers.routes.NextUpdatesController.getNextUpdates().url

  "Income Source Ceased Obligations " should {
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
    "show what you must do heading" in new Setup(validCallWithData) {
      val heading: Element = document.getElementById("heading-what-you-must-do")
      heading.text() shouldBe "What you must do"
    }
    "show warning messages" when {
      "view model has inset warning messages" in new Setup(validCallWithData) {
        document.getElementsByClass("govuk-inset-text").size() shouldBe 1
        document.getElementById("warning-inset-text").text shouldBe "You have 7 overdue updates. You must submit these updates with all required income and expenses through your record keeping software."
      }
      "view model has no inset warning messages" in new Setup(validCallWithNoQuarterlyDataOnlyOneFinalUpdate) {
        document.getElementsByClass("govuk-inset-text").size() shouldBe 0
      }
    }
    "show next upcoming update" when {
      "quarterly updates are present" in new Setup(validCallWithData) {
        val quarterlyAndFinalUpdateList: Element = document.getElementById("quarterly-and-final-update-list")

        document.getElementById("next-upcoming-updates-heading").text() shouldBe IncomeSourceCeasedMessages.yourNextUpComingUpdatesHeading
        quarterlyAndFinalUpdateList.child(0).text shouldBe s"Your next quarterly update for tax year 2022 to 2023 is due by 5 May 2022"
        quarterlyAndFinalUpdateList.child(1).text shouldBe s"Your final declaration for tax year 2023 to 2024 is due by 17 December 2023"
      }
      "quarterly updates are not present then only show final declaration" in new Setup(validCallWithNoQuarterlyDataOnlyOneFinalUpdate) {
        document.getElementById("next-upcoming-updates-heading").text() shouldBe IncomeSourceCeasedMessages.yourNextUpComingUpdatesHeading
        document.getElementById("final-declaration-update").text shouldBe s"Your final declaration for tax year 2023 to 2024 is due by 17 December 2023 ."
      }
      "quarterly updates are not present then only show two final declaration if present" in new Setup(validCallWithNoQuarterlyDataMultipleFinalUpdates) {
        val quarterlyAndFinalUpdateList: Element = document.getElementById("quarterly-and-final-update-list")
        document.getElementById("next-upcoming-updates-heading").text() shouldBe IncomeSourceCeasedMessages.yourNextUpComingUpdatesHeading
        quarterlyAndFinalUpdateList.child(0).text shouldBe s"Your final declaration for tax year 2023 to 2024 is due by 17 December 2023"
        quarterlyAndFinalUpdateList.child(1).text shouldBe s"Your final declaration for tax year 2024 to 2025 is due by 17 December 2024"
      }
    }
    "show view all update link" in new Setup(validCallWithData) {
      val link: Element = document.getElementById("view-all-updates")
      link.hasCorrectLink("View your overdue and upcoming updates", viewUpcomingUpdatesURL)
    }
    "show view all your business link" in new Setup(validCallWithData) {
      val link: Element = document.getElementById("view-all-business-link")
      link.hasCorrectLink("View all your businesses", manageYourBusinessShowURL)
    }
  }
}
