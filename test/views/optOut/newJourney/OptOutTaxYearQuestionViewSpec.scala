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

package views.optOut.newJourney

import forms.optOut.OptOutTaxYearQuestionForm
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optout.newJourney.OptOutTaxYearQuestionViewModel
import org.jsoup.Jsoup
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.optout._
import testUtils.TestSupport
import views.html.optOut.newJourney.OptOutTaxYearQuestionView
import views.messages.OptOutTaxYearQuestionMessages

class OptOutTaxYearQuestionViewSpec extends TestSupport {

  val optOutTaxYearQuestionView: OptOutTaxYearQuestionView = app.injector.instanceOf[OptOutTaxYearQuestionView]

  class Setup(submittedOptOutYear: String, state: OptOutState, numberOfQuarterlyUpdates: Int, previousItsaStatus: ITSAStatus, currentYearItsaStatus: ITSAStatus, nextYearItsaStatus: ITSAStatus, withError: Boolean = false) {
    val previousYearTaxYear = PreviousOptOutTaxYear(previousItsaStatus, TaxYear(2024, 2025), false)
    val currentYearTaxYear = CurrentOptOutTaxYear(currentYearItsaStatus, TaxYear(2025, 2026))
    val nextYearTaxYear = NextOptOutTaxYear(nextYearItsaStatus, TaxYear(2026, 2027), currentYearTaxYear)

    val optOutYear = submittedOptOutYear match {
      case "previous" => previousYearTaxYear
      case "current"  => currentYearTaxYear
      case "next"     => nextYearTaxYear
      case _          => throw new IllegalArgumentException(s"Invalid opt-out year: $submittedOptOutYear")
    }

    val viewModel = OptOutTaxYearQuestionViewModel(
      taxYear = optOutYear,
      optOutState = Some(state),
      numberOfQuarterlyUpdates = numberOfQuarterlyUpdates,
      currentYearStatus = currentYearItsaStatus,
      nextYearStatus = nextYearItsaStatus
    )

    val form = if (withError) {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear).withError("opt-out-tax-year-question", "optout.taxYearQuestion.error", viewModel.taxYear.taxYear.startYear.toString, viewModel.taxYear.taxYear.endYear.toString)
    } else {
      OptOutTaxYearQuestionForm(viewModel.taxYear.taxYear)
    }

    val pageDocument = Jsoup.parse(contentAsString(optOutTaxYearQuestionView(isAgent = true, viewModel, form, controllers.optOut.newJourney.routes.OptOutTaxYearQuestionController.submit(isAgent = true))))
  }

  "Opt out tax year question page" when {
    "opting out for the previous year - Multi" should {
      "have the correct heading" in new Setup("previous", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearHeadingMulti
      }
      "have the correct description" in new Setup("previous", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc1Multi
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.previousYearInsetMulti
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc2Multi
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc3Multi
      }
      "have the correct radio question" in new Setup("previous", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("previous", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }
    "opting out for the current year onwards with voluntary next year - Multi" should {
      "have the correct title" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.currentYearTitleMulti
      }
      "have the correct heading" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearHeadingMulti
      }
      "have the correct description" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc1Multi
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.currentYearInsetMulti
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc2Multi
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc3Multi
      }
      "have the correct radio question" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for the current year onwards with annual next year - Multi" should {
      "have the correct title" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.currentYearTitleMultiAnnualFollowing
      }
      "have the correct heading" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearHeadingMultiAnnualFollowing
      }
      "have the correct description" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc1MultiAnnualFollowing
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc2MultiAnnualFollowing
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc3MultiAnnualFollowing
      }
      "have the correct radio question" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuestionMultiAnnualFollowing
      }
      "display the correct error message when form has errors" in new Setup("current", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for the next year onwards - Multi" should {
      "have the correct title" in new Setup("next", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearTitleMulti
      }
      "have the correct heading" in new Setup("next", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearHeadingMulti
      }
      "have the correct description" in new Setup("next", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc2Multi
        pageDocument.getElementById("opt-out-question-desc-3").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc3Multi
      }
      "have the correct radio question" in new Setup("next", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("next", MultiYearOptOutDefault, 0, ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }
    "opting out for a previous single year followed by Mandated - No updates" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc2
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for a previous single year followed by Dormant - No updates" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Dormant, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Dormant, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Dormant, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc2
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Dormant, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, ITSAStatus.Voluntary, ITSAStatus.Dormant, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for a single year followed by Mandated - No updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedInset
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for a single year followed by Mandated - With updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesInset
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for a previous single year followed by Annual - No updates" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByAnnual, 2, ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByAnnual, 2, ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByAnnual, 2, ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc2
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByAnnual, 2, ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByAnnual, 2, ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }
    "opting out of a single year followed by Annual - No updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc2
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out of a single year followed by Annual - With updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc2
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, ITSAStatus.NoStatus, ITSAStatus.Voluntary, ITSAStatus.Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out of the next year - Annual CY" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Annual, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Annual, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Annual, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultDesc1
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Annual, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Annual, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out of the next year - Mandated CY" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Mandated, ITSAStatus.Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Mandated, ITSAStatus.Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Mandated, ITSAStatus.Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc2
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Mandated, ITSAStatus.Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, ITSAStatus.NoStatus, ITSAStatus.Mandated, ITSAStatus.Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }
  }
}
