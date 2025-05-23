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
import views.html.manageBusinesses.add.IncomeSourceRFCheckDetails

class IncomeSourceRFCheckDetailsViewSpec extends TestSupport {

  val view: IncomeSourceRFCheckDetails = app.injector.instanceOf[IncomeSourceRFCheckDetails]

  class Setup(incomeSourceType: IncomeSourceType, changeReportingFrequency: Boolean = true, isReportingQuarterlyCurrentYear: Boolean = false, isReportingQuarterlyForNextYear: Boolean = false, isAgent: Boolean = false) {

    private val backUrl = "/back-url"
    private val postAction = Call("", "")
    private val viewModel = ReportingFrequencyCheckDetailsViewModel(
      incomeSourceType,
      changeReportingFrequency,
      isReportingQuarterlyCurrentYear,
      isReportingQuarterlyForNextYear
    )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(viewModel, postAction, isAgent, backUrl)))
  }

  def runTest(incomeSourceType: IncomeSourceType, changeReportingFrequency: Boolean = true, isReportingQuarterlyCurrentYear: Boolean = true, isReportingQuarterlyForNextYear: Boolean = true): Unit = {
    "have the correct title" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) {
      pageDocument.title() shouldBe "Check your answers - Manage your Self Assessment - GOV.UK"
    }
    "have the correct heading" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) {
      pageDocument.select("h1").text() shouldBe "Check your answers"
    }

    "have the correct subheading" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) {
      val subHeading = incomeSourceType match {
        case SelfEmployment => "Sole trader"
        case UkProperty => "UK property"
        case ForeignProperty => "Foreign property"
      }

      pageDocument.getElementsByClass("govuk-caption-l").text().contains(subHeading) shouldBe true
    }

    "have the correct summary heading and page contents" in new Setup(incomeSourceType, changeReportingFrequency, isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) {
      pageDocument.getElementsByClass("govuk-summary-list__key").first().text() shouldBe "Do you want to change to report quarterly?"

      if (changeReportingFrequency) {
        pageDocument.getElementsByClass("govuk-summary-list__value").first().text() shouldBe "Yes"
        pageDocument.getElementsByClass("govuk-summary-list__key").last().text() shouldBe "Which tax year do you want to report quarterly for?"

        (isReportingQuarterlyCurrentYear, isReportingQuarterlyForNextYear) match {
          case (true, false) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2023 to 2024"
          case (false, true) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2024 to 2025"
          case (true, true) => pageDocument.getElementsByClass("govuk-summary-list__value").last().text() shouldBe "2023 to 2024 2024 to 2025"
        }

        pageDocument.getElementsByClass("govuk-summary-list__actions").last().text() shouldBe "Change"
        pageDocument.getElementsByClass("govuk-summary-list__actions").last().attr("href") shouldBe ""

      } else {
        pageDocument.getElementsByClass("govuk-summary-list__value").first().text() shouldBe "No"
      }
      pageDocument.getElementsByClass("govuk-summary-list__actions").first().text() shouldBe "Change"

      pageDocument.getElementById("confirm-button").text() shouldBe "Confirm and continue"
    }
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    s"run test for $incomeSourceType with change reporting frequency flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, changeReportingFrequency = false)
    }

    s"run test for $incomeSourceType with change reporting frequency flag set to true with both tax years chosen" should {
      runTest(incomeSourceType = incomeSourceType)
    }

    s"run test for $incomeSourceType with isReportingQuarterlyCurrentYear flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, isReportingQuarterlyCurrentYear = false)
    }

    s"run test for $incomeSourceType with isReportingQuarterlyForNextYear flag set to false" should {
      runTest(incomeSourceType = incomeSourceType, isReportingQuarterlyForNextYear = false)
    }
  }
}
