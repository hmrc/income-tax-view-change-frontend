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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.ReportingFrequencyCheckDetailsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceRFCheckDetailsView

class IncomeSourceRFCheckDetailsViewSpec extends TestSupport {

  val view: IncomeSourceRFCheckDetailsView = app.injector.instanceOf[IncomeSourceRFCheckDetailsView]

  class Setup(incomeSourceType: IncomeSourceType, changeReportingFrequency: Boolean = true, isReportingQuarterlyCurrentYear: Boolean = false, isReportingQuarterlyForNextYear: Boolean = false, isAgent: Boolean = false, displayR17Content: Boolean) {

    private val backUrl = "/back-url"
    private val postAction = Call("", "")
    private val viewModel = ReportingFrequencyCheckDetailsViewModel(
      incomeSourceType,
      changeReportingFrequency,
      isReportingQuarterlyCurrentYear,
      isReportingQuarterlyForNextYear,
      displayR17Content
    )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(viewModel, postAction, isAgent, backUrl)))
  }

  def runTest(incomeSourceType: IncomeSourceType, changeReportingFrequency: Boolean = true, isReportingQuarterlyCurrentYear: Boolean = true, isReportingQuarterlyForNextYear: Boolean = true, displayR17Content: Boolean): Unit = {
    "have the correct title" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear, displayR17Content = displayR17Content) {
      pageDocument.title() shouldBe "Check your answers - Manage your Self Assessment - GOV.UK"
    }
    "have the correct heading" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear, displayR17Content = displayR17Content) {
      pageDocument.select("h1").text() shouldBe "Check your answers"
    }

    "have the correct subheading" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear, displayR17Content = displayR17Content) {
      val subHeading: String = incomeSourceType match {
        case SelfEmployment => "Sole trader"
        case UkProperty => "UK property"
        case ForeignProperty => "Foreign property"
      }

      pageDocument.getElementsByClass("govuk-caption-xl").text().contains(subHeading) shouldBe true
    }

    "have the correct summary heading and page contents" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear, displayR17Content = displayR17Content) {

      val visuallyHidden = pageDocument.getElementsByClass("govuk-summary-list__actions").first().select(".govuk-visually-hidden").text()
      val visuallyHiddenLast = pageDocument.getElementsByClass("govuk-summary-list__actions").last().select(".govuk-visually-hidden").text()

      if(displayR17Content) {
        pageDocument.getElementsByClass("govuk-summary-list__key").first().text() shouldBe "Do you want to sign this new business up to Making Tax Digital for Income Tax?"
        pageDocument.getElementsByClass("govuk-summary-list__actions").first().select(".govuk-visually-hidden").text() shouldBe "Do you want to sign this new business up to Making Tax Digital for Income Tax?"
      } else {
        pageDocument.getElementsByClass("govuk-summary-list__key").first().text() shouldBe "Do you want to change to report quarterly?"
        pageDocument.getElementsByClass("govuk-summary-list__actions").first().select(".govuk-visually-hidden").text() shouldBe "Do you want to change to report quarterly?"
      }

      if (changeReportingFrequency) {
        pageDocument.getElementsByClass("govuk-summary-list__value").first().text() shouldBe "Yes"
        if(displayR17Content) {
          pageDocument.getElementsByClass("govuk-summary-list__key").last().text() shouldBe "Which tax year(s) do you want to sign up for?"
          pageDocument.getElementsByClass("govuk-summary-list__actions").last().select(".govuk-visually-hidden").text() shouldBe "Which tax year(s) do you want to sign up for?"
        } else {
          pageDocument.getElementsByClass("govuk-summary-list__key").last().text() shouldBe "Which tax year(s) do you want to report quarterly for?"
          pageDocument.getElementsByClass("govuk-summary-list__actions").last().select(".govuk-visually-hidden").text() shouldBe "Which tax year(s) do you want to report quarterly for?"
        }

        (isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) match {
          case (true, false) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2023 to 2024"
          case (false, true) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2024 to 2025"
          case (true, true) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2023 to 2024 2024 to 2025"
        }

        pageDocument.getElementsByClass("govuk-summary-list__actions").last().text().replace(visuallyHiddenLast, "").trim shouldBe "Change"
        pageDocument.getElementsByClass("govuk-summary-list__actions").last().attr("href") shouldBe ""

      } else {
        pageDocument.getElementsByClass("govuk-summary-list__value").first().text() shouldBe "No"
      }
      pageDocument.getElementsByClass("govuk-summary-list__actions").first().text().replace(visuallyHidden, "").trim shouldBe "Change"

      pageDocument.getElementById("confirm-button").text() shouldBe "Confirm and continue"
    }
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    s"run test for $incomeSourceType with change reporting frequency flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, changeReportingFrequency = false, displayR17Content = false)
    }

    s"run test for $incomeSourceType with change reporting frequency flag set to true with both tax years chosen" should {
      runTest(incomeSourceType = incomeSourceType, displayR17Content = false)
    }

    s"run test for $incomeSourceType with isReportingQuarterlyCurrentYear flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, isReportingQuarterlyCurrentYear = false, displayR17Content = false)
    }

    s"run test for $incomeSourceType with isReportingQuarterlyForNextYear flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, isReportingQuarterlyForNextYear = false, displayR17Content = false)
    }

    s"run test for $incomeSourceType with isDisplayR17content flag set to true" should {
      runTest(incomeSourceType = incomeSourceType, displayR17Content = true)
    }
  }
}
