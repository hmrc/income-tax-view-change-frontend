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
    val h2Content: String = "Your revised deadlines"
    val quarterlyHeading: String = "Send quarterly updates"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
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
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val day: LocalDate = fixedDate
  val cessationDate: LocalDate = day.plusDays(1)

  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")
  val finalDeclarationDates2: DatesModel = DatesModel(day.plusYears(1), day.plusDays(1).plusYears(1), day.plusDays(2).plusYears(1), "C", isFinalDec = true, obligationType = "Crystallisation")

  val viewAllBusinessLink = "/report-quarterly/income-and-expenses/view/manage-your-businesses"
  val viewUpcomingUpdatesLink = "/report-quarterly/income-and-expenses/view/next-updates"
  val viewReportingObligationsLink = "/report-quarterly/income-and-expenses/view/reporting-frequency"

  val incomeSourceCeasedObligationsViewModel: IncomeSourceCeasedObligationsViewModel = IncomeSourceCeasedObligationsViewModel(
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = None,
    remainingLatentBusiness = false,
    allBusinessesCeased = false
  )

  val validUKPropertyBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(incomeSourceType = UkProperty), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink))
  val validForeignPropertyBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(incomeSourceType = ForeignProperty), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink))
  val validSoleTreaderBusinessCall: Html = view(incomeSourceCeasedObligationsViewModel.copy(businessName = Some("Test Name")), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink))
  val validSoleTreaderBusinessWithNoBusinessNameCall: Html = view(incomeSourceCeasedObligationsViewModel, viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink))
  val validCallWithData: Html = view(
    incomeSourceCeasedObligationsViewModel.copy(
      businessName = Some("Test Name")
    ), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink)
  )
  val validCallWithDataRemainingLatent: Html = view(
    incomeSourceCeasedObligationsViewModel.copy(
      businessName = Some("Test Name"),
      remainingLatentBusiness = true
    ), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink)
  )
  val validCallWithDataAllCeased: Html = view(
    incomeSourceCeasedObligationsViewModel.copy(
      businessName = Some("Test Name"),
      allBusinessesCeased = true
    ), viewAllBusinessLink, viewUpcomingUpdatesLink, Some(viewReportingObligationsLink)
  )
  val validCallWithDataRemainingLatentWithNoRfLink: Html = view(
    incomeSourceCeasedObligationsViewModel.copy(
      businessName = Some("Test Name"),
      remainingLatentBusiness = true
    ), viewAllBusinessLink, viewUpcomingUpdatesLink, None
  )
  val validCallWithDataAllCeasedWithNoRfLink: Html = view(
    incomeSourceCeasedObligationsViewModel.copy(
      businessName = Some("Test Name"),
      allBusinessesCeased = true
    ), viewAllBusinessLink, viewUpcomingUpdatesLink, None
  )

  val manageYourBusinessShowURL: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val viewUpcomingUpdatesURL: String = controllers.routes.NextUpdatesController.show().url

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
    "show 'Your revised deadlines' heading" in new Setup(validCallWithData) {
      val heading: Element = document.getElementById("your-revised-deadlines")
      heading.text() shouldBe "Your revised deadlines"
    }
    "show warning message" when {
      "view model has inset warning message for deadlines in a few minutes" in new Setup(validCallWithData) {
        document.getElementsByClass("govuk-inset-text").size() shouldBe 1
        document.getElementById("warning-inset-text").text shouldBe
          "Your deadlines for this business will be available in the next few minutes."
      }
    }


    "show first paragraph with 'updates and deadlines' link" in new Setup(validCallWithData) {
      val p: Element = document.getElementById("even-if-paragraph")
      val link: Element = document.getElementById("p1-link")
      p.text shouldBe "Even if they are not displayed right away on the updates and deadlines page, your account has been updated."
      link.hasCorrectLink("updates and deadlines", viewUpcomingUpdatesURL)
    }

    "show conditional paragraph with obligations link if remaining latent business and Reporting frequency FS is ON" in new Setup(
      validCallWithDataRemainingLatent) {
      val p: Element = document.getElementById("remaining-business")
      val link: Element = document.getElementById("remaining-business-link")
      p.text shouldBe "Because your remaining business is new, it is set to be opted out of Making Tax Digital for Income Tax for up to 2 tax years." +
        " You can decide at any time to sign back up on your reporting obligations page."
      link.hasCorrectLink("your reporting obligations", viewReportingObligationsLink)
    }

    "show conditional paragraph with obligations link if remaining latent business and Reporting frequency FS is OFF" in new Setup(
      validCallWithDataRemainingLatentWithNoRfLink) {
      val p: Element = document.getElementById("remaining-business")
      p.text shouldBe "Because your remaining business is new, it is set to be opted out of Making Tax Digital for Income Tax for up to 2 tax years."
    }

    "show conditional paragraph with obligations link if all businesses are ceased and Reporting frequency FS is ON" in new Setup(
      validCallWithDataAllCeased) {
      val p: Element = document.getElementById("all-business-ceased")
      val link: Element = document.getElementById("all-business-ceased-link")
      p.text shouldBe "In future, any new business you add will be opted out of Making Tax Digital for Income Tax." +
        " Find out more about your reporting obligations."
      link.hasCorrectLink("your reporting obligations", viewReportingObligationsLink)
    }

    "show conditional paragraph with obligations link if all businesses are ceased and Reporting frequency FS is OFF" in new Setup(
      validCallWithDataAllCeasedWithNoRfLink) {
      val p: Element = document.getElementById("all-business-ceased")
      p.text shouldBe "In future, any new business you add will be opted out of Making Tax Digital for Income Tax."
    }


    "show second paragraph with 'View your businesses' link" in new Setup(validCallWithData) {
      val p: Element = document.getElementById("view-your-businesses")
      val link: Element = document.getElementById("p2-link")
      p.text shouldBe "View your businesses to add, manage or cease a business or income source."
      link.hasCorrectLink("View your businesses", viewAllBusinessLink)
    }
  }
}
