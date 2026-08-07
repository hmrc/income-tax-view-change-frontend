/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.views

import common.testUtils.TestSupport
import hub.controllers.routes
import hub.forms.{ChooseTaxYearsRangeForm, ChooseTaxYearsRangeOption}
import hub.models.TaxYearRangeLabels
import hub.views.html.ChooseTaxYearsRangeView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}

class ChooseTaxYearsRangeViewSpec extends TestSupport {

  val view: ChooseTaxYearsRangeView = app.injector.instanceOf[ChooseTaxYearsRangeView]

  private class Setup(form: Form[ChooseTaxYearsRangeOption]) {
    val pageDocument: Document = Jsoup.parse(contentAsString(
      view(
        form,
        routes.ChooseTaxYearsRangeController.submit(),
        taxYearRangeLabels = TaxYearRangeLabels(
          mtdFromYear = "2018",
          mtdToYear = "2019",
          saFromYear = "2017",
          saToYear = "2018"
        )
      )
    ))
  }

  "Choose tax years range page" should {
    "render all page content" in new Setup(ChooseTaxYearsRangeForm()) {
      pageDocument.title() shouldBe "Which tax years do you want to view and manage? - Manage your Self Assessment - GOV.UK"
      pageDocument.select("h1").text() shouldBe "Which tax years do you want to view and manage?"
      pageDocument.getElementById("choose-tax-years-range-form").attr("action") shouldBe
        routes.ChooseTaxYearsRangeController.submit().url
      pageDocument.getElementsByClass("govuk-hint").text() should include("The tax year runs from 6 April to 5 April")
      pageDocument.select("input[type=radio][value=MTD]").size() shouldBe 1
      pageDocument.text() should include("2018 to 2019 onwards")
      pageDocument.select("input[type=radio][value=SA]").size() shouldBe 1
      pageDocument.text() should include("2017 to 2018 and earlier")
      pageDocument.getElementById("continue-button").text() shouldBe "Continue"
    }

    "render validation error when no option is selected" in new Setup(ChooseTaxYearsRangeForm().bind(Map.empty[String, String])) {
      pageDocument.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      pageDocument.select(".govuk-error-summary__list").text() should include("Select an option")
    }
  }
}
