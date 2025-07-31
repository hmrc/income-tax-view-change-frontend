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
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optout.OptOutTaxYearQuestionViewModel
import org.jsoup.Jsoup
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.optout.{CurrentOptOutTaxYear, MultiYearOptOutDefault, NextOptOutTaxYear, NextYearOptOut, OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated, OptOutState}
import testUtils.TestSupport
import views.html.optOut.OptOutTaxYearQuestionView
import views.messages.OptOutTaxYearQuestionMessages

class OptOutTaxYearQuestionViewSpec extends TestSupport {

  val optOutTaxYearQuestionView: OptOutTaxYearQuestionView = app.injector.instanceOf[OptOutTaxYearQuestionView]

  class Setup(optOutOfCY: Boolean, state: OptOutState, numberOfQuarterlyUpdates: Int, currentYearItsaStatus: ITSAStatus, withError: Boolean = false) {
    val currentYearTaxYear = CurrentOptOutTaxYear(currentYearItsaStatus, TaxYear(2025, 2026))
    val nextYearTaxYear = NextOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2026, 2027), currentYearTaxYear)

    val viewModel = OptOutTaxYearQuestionViewModel(
      taxYear = if (optOutOfCY) currentYearTaxYear else nextYearTaxYear,
      optOutState = Some(state),
      numberOfQuarterlyUpdates = numberOfQuarterlyUpdates,
      currentYearStatus = currentYearItsaStatus
    )

    val form = if (withError) {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear).withError("opt-out-tax-year-question", "optout.taxYearQuestion.error", viewModel.taxYear.taxYear.startYear.toString, viewModel.taxYear.taxYear.endYear.toString)
    } else {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear)
    }

    val pageDocument = Jsoup.parse(contentAsString(optOutTaxYearQuestionView(isAgent = true, viewModel, form, controllers.optOut.routes.OptOutTaxYearQuestionController.submit(isAgent = true))))
  }

  "Opt out tax year question page" when {
    "opting out for the current year onwards - Multi" should {
      "have the correct title" in new Setup(true, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.currentYearTitleMulti
      }
      "have the correct heading" in new Setup(true, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearHeadingMulti
      }
      "have the correct description" in new Setup(true, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc1Multi
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.currentYearInsetMulti
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc2Multi
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc3Multi
      }
      "have the correct radio question" in new Setup(true, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup(true, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for the next year onwards - Multi" should {
      "have the correct title" in new Setup(false, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearTitleMulti
      }
      "have the correct heading" in new Setup(false, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearHeadingMulti
      }
      "have the correct description" in new Setup(false, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc2Multi
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc3Multi
      }
      "have the correct radio question" in new Setup(false, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup(false, MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }
    "opting out for a single year followed by Mandated - No updates" should {
      "have the correct heading" in new Setup(true, OneYearOptOutFollowedByMandated, 0, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading
      }
      "have the correct description" in new Setup(true, OneYearOptOutFollowedByMandated, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedInset
      }
      "have the correct radio question" in new Setup(true, OneYearOptOutFollowedByMandated, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup(true, OneYearOptOutFollowedByMandated, 0, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for a single year followed by Mandated - With updates" should {
      "have the correct heading" in new Setup(true, OneYearOptOutFollowedByMandated, 1, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesHeading
      }
      "have the correct description" in new Setup(true, OneYearOptOutFollowedByMandated, 1, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesInset
      }
      "have the correct radio question" in new Setup(true, OneYearOptOutFollowedByMandated, 1, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup(true, OneYearOptOutFollowedByMandated, 1, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out of a single year followed by Annual - No updates" should {
      "have the correct heading" in new Setup(true, OneYearOptOutFollowedByAnnual, 0, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualHeading
      }
      "have the correct description" in new Setup(true, OneYearOptOutFollowedByAnnual, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc2
      }
      "have the correct radio question" in new Setup(true, OneYearOptOutFollowedByAnnual, 0, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualQuestion
      }
      "display the correct error message when form has errors" in new Setup(true, OneYearOptOutFollowedByAnnual, 0, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out of a single year followed by Annual - With updates" should {
      "have the correct heading" in new Setup(true, OneYearOptOutFollowedByAnnual, 1, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesHeading
      }
      "have the correct description" in new Setup(true, OneYearOptOutFollowedByAnnual, 1, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc2
      }
      "have the correct radio question" in new Setup(true, OneYearOptOutFollowedByAnnual, 1, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup(true, OneYearOptOutFollowedByAnnual, 1, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out of the next year - Annual CY" should {
      "have the correct heading" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutAnnualHeading
      }
      "have the correct description" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutAnnualDesc1
      }
      "have the correct radio question" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutAnnualQuestion
      }
      "display the correct error message when form has errors" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out of the next year - Mandated CY" should {
      "have the correct heading" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedHeading
      }
      "have the correct description" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc2
      }
      "have the correct radio question" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup(false, NextYearOptOut, 0, ITSAStatus.Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }
  }
}
