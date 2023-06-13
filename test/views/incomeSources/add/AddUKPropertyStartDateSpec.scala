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

import forms.incomeSources.add.AddUKPropertyStartDateForm
import forms.models.DateFormElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testUtils.TestSupport
import views.html.incomeSources.add.AddUKPropertyStartDate

class AddUKPropertyStartDateSpec extends TestSupport {

  val addUKPropertyStartDate: AddUKPropertyStartDate = app.injector.instanceOf[AddUKPropertyStartDate]

  class Setup(isAgent: Boolean, error: Boolean = false) {
    val mockDateService: DateService = app.injector.instanceOf[DateService]

    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url else
      controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.AddUKPropertyStartDateController.submitAgent() else
      controllers.incomeSources.add.routes.AddUKPropertyStartDateController.submit()


    lazy val view: HtmlFormat.Appendable = {
      addUKPropertyStartDate(
        addUKPropertyStartDateForm = AddUKPropertyStartDateForm()(mockImplicitDateFormatter, dateService, messages),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl)(individualUser, implicitly)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = {
      addUKPropertyStartDate(
        addUKPropertyStartDateForm = AddUKPropertyStartDateForm()(mockImplicitDateFormatter, dateService, messages)
          .withError(FormError("add-uk-property-start-date", "incomeSources.add.UKPropertyStartDate.error.required")),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl)(individualUser, implicitly)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "AddUKPropertyStartDate - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(false) {
      document.getElementById("add-uk-property-start-date-hint").text() shouldBe messages("incomeSources.add.UKPropertyStartDate.hint") +
       " " +  messages("dateForm.hint")
    }
    "render the date form" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(false, true) {
      document.getElementById("add-uk-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

  "AddUKPropertyStartDate - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(true) {
      document.getElementById("add-uk-property-start-date-hint").text() shouldBe messages("incomeSources.add.UKPropertyStartDate.hint") +
        " " +  messages("dateForm.hint")
    }
    "render the date form" in new Setup(true) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(true, true) {
      document.getElementById("add-uk-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

}
