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

package views.optOut

import forms.optOut.OptOutTaxYearQuestionForm
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutTaxYearQuestionViewModel
import org.jsoup.Jsoup
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.optout.{CurrentOptOutTaxYear, MultiYearOptOutDefault, NextOptOutTaxYear}
import testUtils.TestSupport
import views.html.optOut.OptOutTaxYearQuestionView

class OptOutTaxYearQuestionViewSpec extends TestSupport {

  val optOutTaxYearQuestionView: OptOutTaxYearQuestionView = app.injector.instanceOf[OptOutTaxYearQuestionView]

  val currentYearViewModel = OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026)), Some(MultiYearOptOutDefault))
  val nextYearViewModel = OptOutTaxYearQuestionViewModel(NextOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026))), Some(MultiYearOptOutDefault))

  class Setup(viewModel: OptOutTaxYearQuestionViewModel, withError: Boolean = false) {
    val form = if (withError) {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear).withError("opt-out-tax-year-question", "optout.taxYearQuestion.error", "2025", "2026")
    } else {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear)
    }

    val pageDocument = Jsoup.parse(contentAsString(optOutTaxYearQuestionView(true, viewModel, form, controllers.optOut.routes.OptOutTaxYearQuestionController.submit(true))))
  }

  object optOutTaxYearQuestionMessages {
    val currentYearHeading = "Opt out of Making Tax Digital for Income Tax from the current tax year"
    val currentYearTitle = "Opt out of Making Tax Digital for Income Tax from the current tax year - Manage your Self Assessment - GOV.UK"
    val currentYearDesc1 = "This would mean you no longer need to use software compatible with Making Tax Digital for Income Tax."
    val currentYearInset = "Quarterly updates that you’ve submitted will be deleted from our records if you opt out from that tax year. You’ll need to include any income from these updates in your tax return."
    val currentYearDesc2 = "You would need to go back to the way you have filed your tax return previously for all of your current businesses and any that you add in future."
    val currentYearDesc3 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val currentYearQuestion = "Do you want to opt out from the current tax year?"

    val nextYearHeading = "Opt out of Making Tax Digital for Income Tax from the next tax year"
    val nextYearTitle = "Opt out of Making Tax Digital for Income Tax from the next tax year - Manage your Self Assessment - GOV.UK"
    val nextYearDesc1 = "From 6 April 2026, this would mean you would no longer need to use software compatible with Making Tax Digital for Income Tax."
    val nextYearDesc2 = "You will also need to go back to the way you have filed your tax return previously for all of your current businesses and any that you add in future."
    val nextYearDesc3 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val nextYearQuestion = "Do you want to opt out from the next tax year?"
  }

  "Opt out tax year question page" when {
    "opting out for the current year onwards" should {
      "have the correct title" in new Setup(currentYearViewModel) {
        pageDocument.title() shouldBe optOutTaxYearQuestionMessages.currentYearTitle
      }
      "have the correct heading" in new Setup(currentYearViewModel) {
        pageDocument.select("h1").text() shouldBe optOutTaxYearQuestionMessages.currentYearHeading
      }
      "have the correct description" in new Setup(currentYearViewModel) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe optOutTaxYearQuestionMessages.currentYearDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe optOutTaxYearQuestionMessages.currentYearInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe optOutTaxYearQuestionMessages.currentYearDesc2
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe optOutTaxYearQuestionMessages.currentYearDesc3
      }
      "have the correct radio question" in new Setup(currentYearViewModel) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe optOutTaxYearQuestionMessages.currentYearQuestion
      }
      "display the correct error message when form has errors" in new Setup(currentYearViewModel, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for the next year onwards" should {
      "have the correct title" in new Setup(nextYearViewModel) {
        pageDocument.title() shouldBe optOutTaxYearQuestionMessages.nextYearTitle
      }
      "have the correct heading" in new Setup(nextYearViewModel) {
        pageDocument.select("h1").text() shouldBe optOutTaxYearQuestionMessages.nextYearHeading
      }
      "have the correct description" in new Setup(nextYearViewModel) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe optOutTaxYearQuestionMessages.nextYearDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe optOutTaxYearQuestionMessages.nextYearDesc2
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe optOutTaxYearQuestionMessages.nextYearDesc3
      }
      "have the correct radio question" in new Setup(nextYearViewModel) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe optOutTaxYearQuestionMessages.nextYearQuestion
      }
      "display the correct error message when form has errors" in new Setup(nextYearViewModel, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
  }
}
