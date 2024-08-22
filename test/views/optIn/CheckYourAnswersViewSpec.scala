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

package views.optIn

import controllers.optIn
import models.incomeSourceDetails.TaxYear
import models.optin.MultiYearCheckYourAnswersViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.CheckYourAnswersView

class CheckYourAnswersViewSpec extends TestSupport {

  val view: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  val forYearEnd = 2023
  val taxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  class Setup(isAgent: Boolean = true) {
    private val cancelURL = if (isAgent) optIn.routes.ReportingFrequencyPageController.show(true).url else
      optIn.routes.ReportingFrequencyPageController.show(false).url
    private val model = MultiYearCheckYourAnswersViewModel(intent = taxYear, isAgent = isAgent, cancelURL = cancelURL)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(model)))
  }

  object optInChooseTaxYear {
    val title: String = "Check your answers - Manage your Income Tax updates - GOV.UK"
    val heading: String = messages("optin.checkAnswers.heading")

    val optin: String = messages("optin.checkAnswers.optin")
    val taxYears: String = messages("optin.checkAnswers.taxYears", taxYear.startYear.toString, taxYear.endYear.toString)
    val change: String = messages("optin.checkAnswers.change")
    val optInSummary: String = messages("optin.checkAnswers.cy")

    val confirmButton: String = messages("optin.checkAnswers.confirm")
    val cancelButton: String = messages("optin.checkAnswers.cancel")
  }

  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe optInChooseTaxYear.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optInChooseTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementsByClass("govuk-summary-list__key").text() shouldBe optInChooseTaxYear.optin
      pageDocument.getElementsByClass("govuk-summary-list__value").text() shouldBe optInChooseTaxYear.taxYears
      pageDocument.getElementById("change").text() shouldBe optInChooseTaxYear.change
      pageDocument.getElementById("optIn-summary").text() shouldBe optInChooseTaxYear.optInSummary
      pageDocument.getElementById("confirm-button").text() shouldBe optInChooseTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementsByClass("govuk-summary-list__key").text() shouldBe optInChooseTaxYear.optin
      pageDocument.getElementsByClass("govuk-summary-list__value").text() shouldBe optInChooseTaxYear.taxYears
      pageDocument.getElementById("change").text() shouldBe optInChooseTaxYear.change
      pageDocument.getElementById("optIn-summary").text() shouldBe optInChooseTaxYear.optInSummary
      pageDocument.getElementById("confirm-button").text() shouldBe optInChooseTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
    }
  }
}
