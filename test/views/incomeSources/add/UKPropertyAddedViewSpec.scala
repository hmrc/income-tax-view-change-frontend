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

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testPropertyIncomeId
import testUtils.TestSupport
import views.html.incomeSources.add.UKPropertyAdded
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._

class UKPropertyAddedViewSpec extends TestSupport {

  val ukPropertyAddedView: UKPropertyAdded = app.injector.instanceOf[UKPropertyAdded]

  class Setup(isAgent: Boolean) {
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testPropertyIncomeId).url else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testPropertyIncomeId).url
    val view: HtmlFormat.Appendable = ukPropertyAddedView(viewModel, isAgent, backUrl)
    val document: Document = Jsoup.parse(contentAsString(view))
  }

  object UKPropertyAddedConstants {
    val heading: String = messages("incomeSourceAdded.ukProperty") + " " + messages("incomeSourceAdded.heading")
    val title: String = messages("htmlTitle", heading)
    val titleAgent: String = messages("htmlTitle.agent", heading)
    val subheading: String = messages("incomeSourceAdded.subheading")
  }

  "UKPropertyReportingMethodView - Individual" should {
    "display the correct HTML title" in new Setup(false) {
      document.getElementsByTag("title").text() shouldBe UKPropertyAddedConstants.title
    }
    "display the confirmation banner" in new Setup(false) {
      document.getElementsByClass("govuk-panel govuk-panel--confirmation").text() shouldBe UKPropertyAddedConstants.heading
    }
    "display the h2 'What you must do'" in new Setup(false) {
      document.getElementsByClass("govuk-heading-l govuk-!-margin-top-8").text() shouldBe UKPropertyAddedConstants.subheading
    }
    "display the back button" in new Setup(false) {
      document.getElementById("back").attr("href") shouldBe
        controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testPropertyIncomeId).url
    }
    "display the 'Your income sources' button" in new Setup(false) {
      document.getElementById("income-sources-link").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "display the 'Send quarterly updates' section" in new Setup(false) {
      val quarterlyUpdatesSelector: Element = document.getElementsByClass("box-simple").get(0)

      quarterlyUpdatesSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.quarterly.heading")
      quarterlyUpdatesSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.quarterly.text")
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading") + " 2023 to 2024"
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      // Tax year 1
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe q1Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe q1DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe q2Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe q2DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(4).text() shouldBe q3Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(5).text() shouldBe q3DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(6).text() shouldBe q4Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(7).text() shouldBe q4DeadlineText
      // Tax year 2
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(8).text() shouldBe q1Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(9).text() shouldBe q1DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(10).text() shouldBe q2Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(11).text() shouldBe q2DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(12).text() shouldBe q3Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(13).text() shouldBe q3DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(14).text() shouldBe q4Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(15).text() shouldBe q4DeadlineText_2024
    }
    "display the 'Send end of period statements' section" in new Setup(false) {
      val eopsSelector: Element = document.getElementsByClass("box-simple").get(1)

      eopsSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.eops.heading")
      eopsSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.eops.text")
      eopsSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading")
      eopsSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      eopsSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe "2023 to 2024"
      eopsSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe "31 January 2025"
      eopsSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe "2024 to 2025"
      eopsSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe "31 January 2026"
    }
    "display the 'Submit final declarations and pay your tax' section" in new Setup(false) {
      val finalDeclarationSelector: Element = document.getElementsByClass("box-simple").get(2)

      finalDeclarationSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.finalDeclaration.heading")
      finalDeclarationSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.finalDeclaration.text")
      finalDeclarationSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading")
      finalDeclarationSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe "2023 to 2024"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe "31 January 2025"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe "2024 to 2025"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe "31 January 2026"
    }
    "display the 'Previous tax years' section" in new Setup(false) {
      val previousTaxYearsSelector: Element = document.getElementsByClass("box-simple").get(3)

      previousTaxYearsSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.previousTaxYears.heading")
      previousTaxYearsSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.previousTaxYears.text") + " 2022 to 2023."
    }
  }

  "UKPropertyReportingMethodView - Agent" should {
    "display the correct HTML title" in new Setup(true) {
      document.getElementsByTag("title").text() shouldBe UKPropertyAddedConstants.titleAgent
    }
    "display the confirmation banner" in new Setup(true) {
      document.getElementsByClass("govuk-panel govuk-panel--confirmation").text() shouldBe UKPropertyAddedConstants.heading
    }
    "display the h2 'What you must do'" in new Setup(true) {
      document.getElementsByClass("govuk-heading-l govuk-!-margin-top-8").text() shouldBe UKPropertyAddedConstants.subheading
    }
    "display the back button" in new Setup(true) {
      document.getElementById("back").attr("href") shouldBe
        controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testPropertyIncomeId).url
    }
    "display the 'Your income sources' button" in new Setup(true) {
      document.getElementById("income-sources-link").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "display the 'Send quarterly updates' section" in new Setup(true) {
      val quarterlyUpdatesSelector: Element = document.getElementsByClass("box-simple").get(0)

      quarterlyUpdatesSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.quarterly.heading")
      quarterlyUpdatesSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.quarterly.text")
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading") + " 2023 to 2024"
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      // Tax year 1
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe q1Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe q1DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe q2Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe q2DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(4).text() shouldBe q3Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(5).text() shouldBe q3DeadlineText
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(6).text() shouldBe q4Text
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(7).text() shouldBe q4DeadlineText
      // Tax year 2
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(8).text() shouldBe q1Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(9).text() shouldBe q1DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(10).text() shouldBe q2Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(11).text() shouldBe q2DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(12).text() shouldBe q3Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(13).text() shouldBe q3DeadlineText_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(14).text() shouldBe q4Text_2024
      quarterlyUpdatesSelector.getElementsByClass("govuk-table__cell").get(15).text() shouldBe q4DeadlineText_2024
    }
    "display the 'Send end of period statements' section" in new Setup(true) {
      val eopsSelector: Element = document.getElementsByClass("box-simple").get(1)

      eopsSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.eops.heading")
      eopsSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.eops.text")
      eopsSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading")
      eopsSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      eopsSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe "2023 to 2024"
      eopsSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe "31 January 2025"
      eopsSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe "2024 to 2025"
      eopsSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe "31 January 2026"
    }
    "display the 'Submit final declarations and pay your tax' section" in new Setup(true) {
      val finalDeclarationSelector: Element = document.getElementsByClass("box-simple").get(2)

      finalDeclarationSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.finalDeclaration.heading")
      finalDeclarationSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.finalDeclaration.text")
      finalDeclarationSelector.getElementsByClass("govuk-table__header").get(0).text() shouldBe
        messages("incomeSourceAdded.table.taxYear.heading")
      finalDeclarationSelector.getElementsByClass("govuk-table__header").get(1).text() shouldBe
        messages("incomeSourceAdded.table.deadline.heading")
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(0).text() shouldBe "2023 to 2024"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(1).text() shouldBe "31 January 2025"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(2).text() shouldBe "2024 to 2025"
      finalDeclarationSelector.getElementsByClass("govuk-table__cell").get(3).text() shouldBe "31 January 2026"
    }
    "display the 'Previous tax years' section" in new Setup(true) {
      val previousTaxYearsSelector: Element = document.getElementsByClass("box-simple").get(3)

      previousTaxYearsSelector.getElementsByClass("page_headers govuk-heading-m").text() shouldBe messages("incomeSourceAdded.previousTaxYears.heading")
      previousTaxYearsSelector.getElementsByClass("govuk-body").text() shouldBe messages("incomeSourceAdded.previousTaxYears.text") + " 2022 to 2023."
    }
  }

}
