/*
 * Copyright 2024 HM Revenue & Customs
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

package views.optOut.oldJourney

import forms.optOut.ConfirmOptOutSingleTaxYearForm
import models.incomeSourceDetails.TaxYear
import org.jsoup.nodes.Element
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import uk.gov.hmrc.http.HttpVerbs
import views.html.optOut.oldJourney.SingleYearOptOutWarningView

class SingleYearOptOutWarningViewSpec extends ViewSpec {

  val singleYearOptOutWarningView: SingleYearOptOutWarningView = app.injector.instanceOf[SingleYearOptOutWarningView]
  val taxYear: TaxYear = TaxYear.forYearEnd(2024)

  val form: Form[ConfirmOptOutSingleTaxYearForm] = ConfirmOptOutSingleTaxYearForm(taxYear)

  val errorMessage: String = "some error message"
  val formWithError: Form[ConfirmOptOutSingleTaxYearForm] = form.withError(ConfirmOptOutSingleTaxYearForm.confirmOptOutField, errorMessage)

  val submitAction: Call = Call(HttpVerbs.POST, "/some/url")
  val backUrl = "/some/back/url"
  val renderedView: HtmlFormat.Appendable = singleYearOptOutWarningView(taxYear, form, submitAction, backUrl, isAgent = false)
  val renderedErrorView: HtmlFormat.Appendable = singleYearOptOutWarningView(taxYear, formWithError, submitAction, backUrl, isAgent = false)

  "SingleYearOptOutWarningView" when {
    "called" should {
      "have the correct title" in new Setup(renderedView) {
        document.title() shouldBe messages("htmlTitle", "Opt out of quarterly reporting for a single tax year")
      }

      "have the correct heading" in new Setup(renderedView) {
        document.h1.text() shouldBe "Opt out of quarterly reporting for a single tax year"
      }

      "have the correct detail text" in new Setup(renderedView) {
        document.selectById("detail-text").text() shouldBe s"You can only opt out for the ${taxYear.startYear} to ${taxYear.endYear} tax year."
      }

      "have the correct inset text" in new Setup(renderedView) {
        document.selectById("warning-inset").text() shouldBe s"If you continue, from 6 April ${taxYear.endYear}. you’ll be required to send quarterly updates again through software."
      }

      "have correct form details" in new Setup(renderedView) {
        private val form: Element = document.selectById("confirm-single-year-opt-out-form")
        private val formTitle = form.selectFirst(".govuk-fieldset__legend--m")
        private val options = form.selectFirst(".govuk-radios")
        private val optionLabels = options.select(".govuk-radios__label")

        form.attr("method") shouldBe submitAction.method
        form.attr("action") shouldBe submitAction.url

        formTitle.text() shouldBe s"Do you still want to opt out for the ${taxYear.startYear} to ${taxYear.endYear} tax year?"

        options.childrenSize() shouldBe 2
        optionLabels.get(0).text() shouldBe "Yes"
        form.selectById("yes-response-item-hint").text() shouldBe "I want to opt out and report annually."
        optionLabels.get(1).text() shouldBe "No"

        form.selectById("no-response-item-hint").text() shouldBe "I want to continue reporting quarterly."
      }

      "have a continue button" in new Setup(renderedView) {
        document.selectById("continue-button").text() shouldBe "Continue"
      }

      "have a error summary" in new Setup(renderedErrorView) {
        document.select(".govuk-error-summary__body").get(0).text() shouldBe errorMessage
      }

    }
  }
}
