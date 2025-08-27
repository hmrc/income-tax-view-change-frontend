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

package views.optIn

import forms.optIn.SignUpTaxYearQuestionForm
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optin.SignUpTaxYearQuestionViewModel
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear}
import testUtils.TestSupport
import views.html.optIn.SignUpTaxYearQuestionView

class SignUpTaxYearQuestionViewSpec extends TestSupport {

  val signUpTaxYearQuestion: SignUpTaxYearQuestionView = app.injector.instanceOf[SignUpTaxYearQuestionView]

  val currentYearViewModel = SignUpTaxYearQuestionViewModel(CurrentOptInTaxYear(ITSAStatus.Annual, TaxYear(2025, 2026)))
  val nextYearViewModel = SignUpTaxYearQuestionViewModel(NextOptInTaxYear(ITSAStatus.Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(ITSAStatus.Annual, TaxYear(2025, 2026))))

  class Setup(viewModel: SignUpTaxYearQuestionViewModel, withError: Boolean = false) {
    val form = if (withError) {
      SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, true).withError("sign-up-tax-year-question", "signUp.taxYearQuestion.error.currentYear")
    } else {
      SignUpTaxYearQuestionForm(viewModel.signUpTaxYear.taxYear, true)
    }

    val pageDocument = org.jsoup.Jsoup.parse(contentAsString(signUpTaxYearQuestion(isAgent = true, viewModel, form, controllers.optIn.routes.SignUpTaxYearQuestionController.submit(isAgent = true))))
  }

  object signUpTaxYearQuestionMessages {
    val currentYearHeading = "Voluntarily signing up for the current tax year"
    val currentYearTitle = "Voluntarily signing up for the current tax year - Manage your Self Assessment - GOV.UK"
    val currentYearDesc = "Signing up to the current tax year could mean you would have at least one quarterly update overdue."
    val currentYearDesc2 = "The quarterly update deadlines are:"
    val currentYearBullet1 = "7 August 2025"
    val currentYearBullet2 = "7 November 2025"
    val currentYearBullet3 = "7 February 2026"
    val currentYearBullet4 = "7 May 2026"
    val currentYearSubheading = "Voluntarily signing up and overdue quarterly updates"
    val currentYearDesc3 = "Every 3 months an update is due for each of your property and sole trader income sources. Each quarterly update is a running total of income and expenses for the tax year so far."
    val currentYearDesc4 = "If you sign up to the current tax year and start now, the more likely you will have overdue updates. The later you sign up in the tax year, the more information you will need to submit."
    val currentYearInset = "Because you would be voluntarily signing up, there would be no penalties for overdue quarterly updates."
    val currentYearDesc5 = "If in future you are required to use Making Tax Digital for Income Tax, then penalties would apply. We will send you a letter if this happens."
    val currentYearQuestion = "Do you want to sign up for the current tax year?"
    val currentYearRadioHint = "Signing up could result in immediate overdue quarterly updates."
    val currentYearErrorMessage = "Select yes to sign up for the current tax year"

    val nextYearHeading = "Confirm and sign up from the 2026 to 2027 tax year onwards"
    val nextYearTitle = "Confirm and sign up from the 2026 to 2027 tax year onwards - Manage your Self Assessment - GOV.UK"
    val nextYearDesc = "If you sign up for the next tax year onwards, from 6 April 2026 you will need to submit your quarterly updates through software compatible with Making Tax Digital for Income Tax."
    val nextYearQuestion = "Do you want to sign up from the 2026 to 2027 tax year?"
  }

  "Sign up tax year question page" when {
    "signing up for the current year" should {
      "have the correct title" in new Setup(currentYearViewModel) {
        pageDocument.title() shouldBe signUpTaxYearQuestionMessages.currentYearTitle
      }

      "have the correct heading" in new Setup(currentYearViewModel) {
        pageDocument.select("h1").text() shouldBe signUpTaxYearQuestionMessages.currentYearHeading
      }

      "have the correct subheading" in new Setup(currentYearViewModel) {
        pageDocument.getElementById("sign-up-question-subheading").text() shouldBe signUpTaxYearQuestionMessages.currentYearSubheading
      }

      "have the correct description" in new Setup(currentYearViewModel) {
        pageDocument.getElementById("sign-up-question-desc-1").text() shouldBe signUpTaxYearQuestionMessages.currentYearDesc
        pageDocument.getElementById("sign-up-question-desc-2").text() shouldBe signUpTaxYearQuestionMessages.currentYearDesc2
        pageDocument.getElementById("sign-up-question-desc-3").text() shouldBe signUpTaxYearQuestionMessages.currentYearDesc3
        pageDocument.getElementById("sign-up-question-desc-4").text() shouldBe signUpTaxYearQuestionMessages.currentYearDesc4
        pageDocument.getElementById("sign-up-question-inset").text() shouldBe signUpTaxYearQuestionMessages.currentYearInset
        pageDocument.getElementById("sign-up-question-desc-5").text() shouldBe signUpTaxYearQuestionMessages.currentYearDesc5
      }

      "have the correct bullet points" in new Setup(currentYearViewModel) {
        pageDocument.getElementById("quarterly-deadlines-1").text() shouldBe signUpTaxYearQuestionMessages.currentYearBullet1
        pageDocument.getElementById("quarterly-deadlines-2").text() shouldBe signUpTaxYearQuestionMessages.currentYearBullet2
        pageDocument.getElementById("quarterly-deadlines-3").text() shouldBe signUpTaxYearQuestionMessages.currentYearBullet3
        pageDocument.getElementById("quarterly-deadlines-4").text() shouldBe signUpTaxYearQuestionMessages.currentYearBullet4
      }

      "have the correct radio question" in new Setup(currentYearViewModel) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe signUpTaxYearQuestionMessages.currentYearQuestion
      }

      "have the correct radio hint" in new Setup(currentYearViewModel) {
        pageDocument.getElementsByClass("govuk-hint").text() shouldBe signUpTaxYearQuestionMessages.currentYearRadioHint
      }

      "have the correct error message when form has errors" in new Setup(currentYearViewModel, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe signUpTaxYearQuestionMessages.currentYearErrorMessage
      }
    }

    "signing up for the next year" should {
      "have the correct title" in new Setup(nextYearViewModel) {
        pageDocument.title() shouldBe signUpTaxYearQuestionMessages.nextYearTitle
      }

      "have the correct heading" in new Setup(nextYearViewModel) {
        pageDocument.select("h1").text() shouldBe signUpTaxYearQuestionMessages.nextYearHeading
      }

      "have the correct description" in new Setup(nextYearViewModel) {
        pageDocument.getElementById("sign-up-question-desc-1").text() shouldBe signUpTaxYearQuestionMessages.nextYearDesc
      }

      "have the correct radio question" in new Setup(nextYearViewModel) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe signUpTaxYearQuestionMessages.nextYearQuestion
      }

    }
  }
}
