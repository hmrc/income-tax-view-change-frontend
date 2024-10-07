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

package views.claimToAdjustPoa

import forms.adjustPoa.SelectYourReasonFormProvider
import models.claimToAdjustPoa.SelectYourReason
import models.core.NormalMode
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.SelectYourReasonView

class SelectYourReasonViewSpec extends TestSupport {

  val selectYourReasonView: SelectYourReasonView = app.injector.instanceOf[SelectYourReasonView]
  lazy val form: Form[SelectYourReason] = new SelectYourReasonFormProvider().apply()

  val view: Html = selectYourReasonView(form, TaxYear(fixedDate.getYear, fixedDate.getYear + 1), isAgent = false, NormalMode, useFallbackLink = true)
  val document: Document = Jsoup.parse(view.toString())

  val title = "Select Your Reason - Manage your Income Tax updates - GOV.UK"
  val caption = "2023 to 2024 tax year"
  val heading = "Select your reason"
  val paragraph1 = "You can only reduce your payments on account for one of the reasons listed below. If none of these apply to you, " +
    "you will not be able to continue."
  val paragraph2 = "If you cannot afford to pay your tax bill in full, you can contact HMRC to set up a payment plan (opens in new tab)."
  val paragraph2Link = "https://www.gov.uk/difficulties-paying-hmrc"
  val subheading = "Why are you reducing your payments on account?"
  val bullet1 = "My main income will be lower"
  val bullet1Hint = "For example, sole trader or property business profits."
  val bullet2 = "My other income will be lower"
  val bullet2Hint = "For example, dividend payments or pension income."
  val bullet3 = "My tax allowances or reliefs will be higher"
  val bullet3Hint = "For example, marriage allowance, pension relief or payments to charity."
  val bullet4 = "More of my income will be taxed at source"
  val bullet4Hint = "For example, under PAYE."
  val continue = "Continue"
  val continueLink = "/report-quarterly/income-and-expenses/view"
  val errorSummary = "Select the main reason you’re reducing your payments on account"

  "SelectYourReasonView" should {

    "render the correct title" in {
      document.title() shouldBe title
    }

    "render the correct caption" in {
      document.select("#main-content > div > div > div.govuk-\\!-margin-bottom-6 > h2").first().ownText() shouldBe caption
    }

    "render the correct heading" in {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe heading
    }

    "render the correct first paragraph" in {
      document.select("#value-hint > p:nth-child(1)").text() shouldBe paragraph1
    }

    "render the correct second paragraph" in {
      document.select("#value-hint > p:nth-child(2)").text() shouldBe paragraph2
    }

    "has the correct link in the second paragraph which takes the user to the HMRC payments plan page" in {
      document.select("#value-hint > p:nth-child(2) > a").attr("href") shouldBe paragraph2Link
    }

    "renders the correct subheading" in {
      document.getElementsByClass("govuk-heading-m").first().text() shouldBe subheading
    }

    "renders the correct first bullet point text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(1) > label").first().ownText() shouldBe
        bullet1
    }

    "renders the correct first bullet point hint text" in {
      document
        .select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(1) > label > span")
        .first().ownText() shouldBe bullet1Hint
    }

    "renders the correct second bullet point text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(2) > label")
        .first().ownText() shouldBe bullet2
    }

    "renders the correct second bullet point hint text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(2) > label > span")
        .first().ownText() shouldBe bullet2Hint
    }

    "renders the correct third bullet point text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(3) > label")
        .first().ownText() shouldBe bullet3
    }

    "renders the correct third bullet point hint text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(3) > label > span")
        .first().ownText() shouldBe bullet3Hint
    }

    "renders the correct fourth bullet point text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(4) > label")
        .first().ownText() shouldBe bullet4
    }

    "renders the correct fourth bullet point hint text" in {
      document.select("#select-your-reason-form > div.govuk-form-group > fieldset > div.govuk-radios > div:nth-child(4) > label > span")
        .first().ownText() shouldBe bullet4Hint
    }

    "renders the correct continue button" in {
      document.select("#select-your-reason-form > div.govuk-button-group > button").first().text() shouldBe continue
    }

    "has the correct link on the continue button" in {
      document.select("#cancel-link").attr("href") shouldBe continueLink
    }

    "when there are errors on the page" should {

      "render the correct error summary content" in {
        document.select("#error-summary-display") shouldBe errorSummary
      }
    }
  }

}
