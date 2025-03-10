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
import forms.manageBusinesses.add.ChooseTaxYearForm
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.ChooseTaxYear

class ChooseTaxYearViewSpec extends TestSupport {

  val view: ChooseTaxYear = app.injector.instanceOf[ChooseTaxYear]

  class Setup(form: Form[ChooseTaxYearForm] = ChooseTaxYearForm(), incomeSourceType: IncomeSourceType) {
    val subHeadingText: String = incomeSourceType match {
      case SelfEmployment => "Sole trader"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }
    val postAction: Call = controllers.manageBusinesses.add.routes.ChooseTaxYearController.submit(false, incomeSourceType)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(form, isAgent = false, postAction, TaxYear(2023, 2024), TaxYear(2024, 2025), incomeSourceType)))
  }

  val incomeSourceTypes: Seq[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  incomeSourceTypes.foreach { incomeSourceType =>
    s"add sole trader choose tax year page for incomeSourceType: $incomeSourceType" should {
      "have the correct title" in new Setup(incomeSourceType = incomeSourceType) {
        pageDocument.title() shouldBe "Which tax year do you want to report quarterly for? - Manage your Income Tax updates - GOV.UK"
      }

      "have the correct heading" in new Setup(incomeSourceType = incomeSourceType) {
        pageDocument.select("h1").text().contains("Which tax year do you want to report quarterly for?") shouldBe true
      }

      "have the correct sub-heading" in new Setup(incomeSourceType = incomeSourceType) {
        pageDocument.getElementById("choose-tax-year-subheading").text() shouldBe subHeadingText
      }

      "have the correct checkbox contents" in new Setup(incomeSourceType = incomeSourceType) {
        pageDocument.getElementsByTag("label").first().text() shouldBe "2023 to 2024"
        pageDocument.getElementsByTag("label").last().text() shouldBe "2024 to 2025"
      }

      "have the correct button" in new Setup(incomeSourceType = incomeSourceType) {
        pageDocument.getElementById("continue-button").text() shouldBe "Continue"
        pageDocument.getElementById("continue-button").attr("href") shouldBe ""
      }

      "have the correct error summary" in new Setup(ChooseTaxYearForm().bind(Map("Invalid" -> "Invalid")), incomeSourceType) {
        pageDocument.getElementById("error-summary-title").text() shouldBe "There is a problem"
        pageDocument.getElementById("error-summary-link").text() shouldBe "Select the tax years you want to report quarterly"
        pageDocument.getElementById("error-summary-link").attr("href") shouldBe "#current-year-checkbox"
        pageDocument.getElementById("choose-tax-year-error").text() shouldBe "Error: Select the tax years you want to report quarterly"
      }
    }
  }
}
