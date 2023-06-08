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

package views.incomeSources.cease

import forms.incomeSources.cease.CeaseUKPropertyForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testNavHtml
import testUtils.TestSupport
import views.html.incomeSources.cease.CeaseUKProperty

class CeaseUKPropertyViewSpec extends TestSupport {
  val ceaseUKPropertyView: CeaseUKProperty = app.injector.instanceOf[CeaseUKProperty]

  class Setup(isAgent: Boolean, error: Boolean = false) {

    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      ceaseUKPropertyView(
        ceaseUKPropertyForm = CeaseUKPropertyForm.form,
        postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submitAgent,
        isAgent = true,
        backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
        btaNavPartial = testNavHtml)(FakeRequest(), implicitly)
    } else {
      ceaseUKPropertyView(
        ceaseUKPropertyForm = CeaseUKPropertyForm.form,
        postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit,
        isAgent = false,
        backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
        btaNavPartial = testNavHtml)(FakeRequest(), implicitly)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = if (isAgent) {
      ceaseUKPropertyView(
        ceaseUKPropertyForm = CeaseUKPropertyForm.form
          .withError(CeaseUKPropertyForm.declarationUnselectedError, messages("incomeSources.ceaseUKProperty.radioError")),
        postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submitAgent,
        isAgent = true,
        backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
        btaNavPartial = testNavHtml)(FakeRequest(), implicitly)
    } else {
      ceaseUKPropertyView(
        ceaseUKPropertyForm = CeaseUKPropertyForm.form
          .withError(CeaseUKPropertyForm.declarationUnselectedError, messages("incomeSources.ceaseUKProperty.radioError")),
        postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit,
        isAgent = false,
        backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
        btaNavPartial = testNavHtml)(FakeRequest(), implicitly)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "CeaseUKPropertyView - Individual" should {
    "render the legend" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.ceaseUKProperty.heading")
    }
    "render the checkbox" in new Setup(false) {
      document.getElementById("cease-uk-property-declaration").attr("type") shouldBe "checkbox"
    }
    "render the checkbox label" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.ceaseUKProperty.radioLabel")
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.ceaseUKProperty.radioError")
    }
  }
  "CeaseUKPropertyView - Agent" should {
    "render the legend" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.ceaseUKProperty.heading")
    }
    "render the checkbox label" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.ceaseUKProperty.radioLabel")
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.ceaseUKProperty.radioError")
    }
  }
}
