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

import forms.manageBusinesses.add.IncomeSourceReportingFrequencyForm
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddSoleTraderChooseTaxYear

class AddSoleTraderChooseTaxYearViewSpec extends TestSupport {

  val view: AddSoleTraderChooseTaxYear = app.injector.instanceOf[AddSoleTraderChooseTaxYear]

  class Setup(form: Form[IncomeSourceReportingFrequencyForm] = IncomeSourceReportingFrequencyForm()) {
    val postAction: Call = controllers.manageBusinesses.add.routes.AddSoleTraderChooseTaxYearController.submit(false)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(form, isAgent = false, postAction, TaxYear(2023, 2024), TaxYear(2024, 2025))))
  }

  "add sole trader choose tax year page" should {
    "have the correct title" in new Setup() {
      pageDocument.title() shouldBe "Which tax year do you want to report quarterly for? - Manage your Income Tax updates - GOV.UK"
    }

    "have the correct heading" in new Setup() {
      pageDocument.select("h1").text().contains("Which tax year do you want to report quarterly for?") shouldBe true
    }

    "have the correct sub-heading" in new Setup() {
      pageDocument.getElementById("choose-tax-year-subheading").text() shouldBe "Sole Trader"
    }

    "have the correct checkbox contents" in new Setup() {
      pageDocument.getElementsByTag("label").first().text() shouldBe "2023 to 2024"
      pageDocument.getElementsByTag("label").last().text() shouldBe "2024 to 2025"
    }

    "have the correct button" in new Setup() {
      pageDocument.getElementById("continue-button").text() shouldBe "Continue"
      pageDocument.getElementById("continue-button").attr("href") shouldBe ""
    }

    "have the correct error summary" in new Setup(IncomeSourceReportingFrequencyForm().bind(Map("Invalid" -> "Invalid"))) {
      pageDocument.getElementById("error-summary-title").text() shouldBe "There is a problem"
      pageDocument.getElementById("error-summary-link").text() shouldBe "Select the tax years you want to report quarterly"
      pageDocument.getElementById("error-summary-link").attr("href") shouldBe "#current-year-checkbox"
      pageDocument.getElementById("choose-tax-year-error").text() shouldBe "Error: Select the tax years you want to report quarterly"
    }
  }
}
