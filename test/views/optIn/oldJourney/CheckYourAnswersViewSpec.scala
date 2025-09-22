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

package views.optIn.oldJourney

import models.incomeSourceDetails.TaxYear
import models.optin.MultiYearCheckYourAnswersViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.oldJourney.CheckYourAnswersView

class CheckYourAnswersViewSpec extends TestSupport {

  val view: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  val forYearEnd = 2023
  val taxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  class Setup(isAgent: Boolean = true, intent: TaxYear, intentIsNextYear: Boolean) {

    private val cancelURL =
      controllers.routes.ReportingFrequencyPageController.show(isAgent).url

    private val model =
      MultiYearCheckYourAnswersViewModel(
        intentTaxYear = intent,
        isAgent = isAgent,
        cancelURL = cancelURL,
        intentIsNextYear = intentIsNextYear
      )
    val pageDocument: Document = Jsoup.parse(contentAsString(view(model)))
  }

  object optInChooseTaxYear {
    val title: String = "Check your answers - Manage your Self Assessment - GOV.UK"
    val heading: String = messages("optin.checkAnswers.heading")

    val optin: String = messages("optin.checkAnswers.optin")

    def taxYears(intent: TaxYear): String = messages("optin.checkAnswers.taxYears", intent.startYear.toString, intent.endYear.toString)

    val change: String = messages("optin.checkAnswers.change")

    val optInSummaryCy: String = messages("optin.checkAnswers.cy")

    def optInSummaryNy(intent: TaxYear): String = messages("optin.checkAnswers.ny", intent.startYear.toString, intent.endYear.toString)

    val confirmButton: String = messages("optin.checkAnswers.confirm")
    val cancelButton: String = messages("optin.checkAnswers.cancel")
  }

  def runTest(intent: TaxYear, intentIsNextYear: Boolean): Unit = {

    "have the correct title" in new Setup(false, intent, intentIsNextYear) {
      pageDocument.title() shouldBe optInChooseTaxYear.title
    }

    "have the correct heading" in new Setup(false, intent, intentIsNextYear) {
      pageDocument.select("h1").text() shouldBe optInChooseTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(false, intent, intentIsNextYear) {
      pageDocument.getElementsByClass("govuk-summary-list__key").text() shouldBe optInChooseTaxYear.optin
      pageDocument.getElementsByClass("govuk-summary-list__value").text() shouldBe optInChooseTaxYear.taxYears(intent)
      pageDocument.getElementById("change").text() shouldBe optInChooseTaxYear.change

      private val expectedSummary = if (intentIsNextYear) optInChooseTaxYear.optInSummaryNy(intent)
      else optInChooseTaxYear.optInSummaryCy
      pageDocument.getElementById("optIn-summary").text() shouldBe expectedSummary
      pageDocument.getElementById("confirm-button").text() shouldBe optInChooseTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true, intent, intentIsNextYear) {
      pageDocument.getElementsByClass("govuk-summary-list__key").text() shouldBe optInChooseTaxYear.optin
      pageDocument.getElementsByClass("govuk-summary-list__value").text() shouldBe optInChooseTaxYear.taxYears(intent)
      pageDocument.getElementById("change").text() shouldBe optInChooseTaxYear.change

      private val expectedSummary = if (intentIsNextYear) optInChooseTaxYear.optInSummaryNy(intent)
      else optInChooseTaxYear.optInSummaryCy
      pageDocument.getElementById("optIn-summary").text() shouldBe expectedSummary
      pageDocument.getElementById("confirm-button").text() shouldBe optInChooseTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
    }

  }

  "run test when intent is current year" should {
    runTest(taxYear, intentIsNextYear = false)
  }

  "run test when intent is next year" should {
    runTest(taxYear.nextYear, intentIsNextYear = true)
  }

}
