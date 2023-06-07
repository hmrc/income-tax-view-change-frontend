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

import forms.incomeSources.add.CheckUKPropertyStartDateForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testUtils.TestSupport
import views.html.incomeSources.add.CheckUKPropertyStartDate

import java.time.LocalDate

class CheckUKPropertyStartDateSpec extends TestSupport {

  val checkUKPropertyStartDate: CheckUKPropertyStartDate = app.injector.instanceOf[CheckUKPropertyStartDate]

  class Setup(isAgent: Boolean, error: Boolean = false) {
    val mockDateService: DateService = app.injector.instanceOf[DateService]
    val form: Form[_] = CheckUKPropertyStartDateForm.form
    val startDate: String = "2022-06-30"
    val formattedStartDate: String = mockImplicitDateFormatter.longDate(LocalDate.parse(startDate)).toLongDate
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.AddUKPropertyStartDateController.showAgent().url else
      controllers.incomeSources.add.routes.AddUKPropertyStartDateController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submitAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submit()


    lazy val view: HtmlFormat.Appendable = {
      checkUKPropertyStartDate(
        checkUKPropertyStartDateForm = form,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        startDate = formattedStartDate)(FakeRequest(), implicitly)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = {
      checkUKPropertyStartDate(
        checkUKPropertyStartDateForm = form.withError(FormError("check-uk-property-start-date",
          "incomeSources.add.checkUKPropertyStartDate.error.required")),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        startDate = formattedStartDate)(FakeRequest(), implicitly)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "CheckUKPropertyStartDate - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-heading-xl").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add UK Property Start Date page" in new Setup(false) {
      document.getElementById("check-uk-property-start-date").getElementsByTag("p").first.text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true) {
      document.getElementById("check-uk-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.checkUKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages("incomeSources.add.checkUKPropertyStartDate.error.required")
    }
    "render the back url" in new Setup(false, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }

  "CheckUKPropertyStartDate - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-heading-xl").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add UK Property Start Date page" in new Setup(false) {
      document.getElementById("check-uk-property-start-date").getElementsByTag("p").first.text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(true) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true) {
      document.getElementById("check-uk-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.checkUKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages("incomeSources.add.checkUKPropertyStartDate.error.required")
    }
    "render the back url" in new Setup(true, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }
}
