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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, UkProperty}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.cease.DeclarePropertyCeased

class DeclarePropertyCeasedViewSpec extends TestSupport {
  val declarePropertyCeasedView: DeclarePropertyCeased = app.injector.instanceOf[DeclarePropertyCeased]

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, error: Boolean = false) {
    val (postAction, backAction) = if (isAgent) {
      (controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submitAgent(incomeSourceType),
        controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent())
    } else {
      (controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(incomeSourceType),
        controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show())
    }
    lazy val view: HtmlFormat.Appendable = declarePropertyCeasedView(
      declarePropertyCeasedForm = DeclareIncomeSourceCeasedForm.form(incomeSourceType),
      incomeSourceType = incomeSourceType,
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backAction.url
    )(individualUser, implicitly)


    val formWithError = DeclareIncomeSourceCeasedForm.form(incomeSourceType)
      .withError(
        DeclareIncomeSourceCeasedForm.declaration,
        messages(s"incomeSources.cease.${incomeSourceType.key}.checkboxError"))

    lazy val viewWithInputErrors: HtmlFormat.Appendable = declarePropertyCeasedView(
      declarePropertyCeasedForm = formWithError,
      incomeSourceType = incomeSourceType,
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backAction.url
    )(individualUser, implicitly)


    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }


  "Declare UK property Ceased View - Individual" should {
    "render the legend" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.cease.UK.heading")
    }
    "render the checkbox" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById(DeclareIncomeSourceCeasedForm.declaration).attr("type") shouldBe "checkbox"
    }
    "render the checkbox label" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.cease.UK.checkboxLabel")
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error summary" in new Setup(isAgent = false, incomeSourceType = UkProperty, error = true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UK.checkboxError")
    }
  }
  "Declare UK property Ceased View - Agent" should {
    "render the legend" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.cease.UK.heading")
    }
    "render the checkbox label" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.cease.UK.checkboxLabel")
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
    }
    "render the error summary" in new Setup(isAgent = true, incomeSourceType = UkProperty, error = true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UK.checkboxError")
    }
  }

  "Declare Foreign property Ceased View - Individual" should {
    "render the legend" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.cease.FP.heading")
    }
    "render the checkbox" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById(DeclareIncomeSourceCeasedForm.declaration).attr("type") shouldBe "checkbox"
    }
    "render the checkbox label" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.cease.FP.checkboxLabel")
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error summary" in new Setup(isAgent = false, incomeSourceType = ForeignProperty, error = true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.FP.checkboxError")
    }
  }
  "Declare Foreign property Ceased View - Agent" should {
    "render the legend" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.cease.FP.heading")
    }
    "render the checkbox label" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe messages("incomeSources.cease.FP.checkboxLabel")
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
    }
    "render the error summary" in new Setup(isAgent = true, incomeSourceType = ForeignProperty, error = true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.FP.checkboxError")
    }
  }
}
