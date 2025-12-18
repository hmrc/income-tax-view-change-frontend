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
import models.itsaStatus.ITSAStatus._
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
    "opting out from the previous year onwards without submitted updates - Multi-year Opt-Out" should {
      "have the correct heading" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearHeadingMulti
      }
      "have the correct description" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc2Multi
      }
      "have the correct radio question" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out from the current year onwards with voluntary next year without submitted updates - Multi-year Opt-Out" should {
      "have the correct title" in new Setup("current", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.currentYearTitleMulti
      }
      "have the correct heading" in new Setup("current", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearHeadingMulti
      }
      "have the correct description" in new Setup("current", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc2Multi
      }
      "have the correct radio question" in new Setup("current", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("current", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out from the current year onwards with voluntary next year with submitted updates - Multi-year Opt-Out" should {
      "have the correct title" in new Setup("current", MultiYearOptOutDefault, 3, Voluntary, Voluntary, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.currentYearTitleMulti
      }
      "have the correct heading" in new Setup("current", MultiYearOptOutDefault, 3, Voluntary, Voluntary, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearHeadingMulti
      }
      "have the correct description" in new Setup("current", MultiYearOptOutDefault, 3, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc1Multi
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuarterlyInsetMulti
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.currentYearDesc2Multi
      }
      "have the correct radio question" in new Setup("current", MultiYearOptOutDefault, 3, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.currentYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("current", MultiYearOptOutDefault, 3, Voluntary, Voluntary, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out for the current year onwards with annual next year without submitted updates - Single-year Opt-out" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, Voluntary, Voluntary, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, Voluntary, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc2
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, Voluntary, Voluntary, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out for the next year onwards without submitted updates - Single-year Opt-out" should {
      "have the correct title" in new Setup("next", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearTitleMulti
      }
      "have the correct heading" in new Setup("next", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearHeadingMulti
      }
      "have the correct description" in new Setup("next", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearDesc2Multi
      }
      "have the correct radio question" in new Setup("next", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("next", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out for a previous single year followed by Mandated - No updates" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for the current single year followed by Mandated - No updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out for a single year followed by Mandated - With updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 1, NoStatus, Voluntary, Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 1, NoStatus, Voluntary, Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 1, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesDesc1
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 1, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 1, NoStatus, Voluntary, Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }


    "opting out for a previous single year followed by Annual - No updates" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc2
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out of a single year followed by Annual - No updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, NoStatus, Voluntary, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, NoStatus, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, NoStatus, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc2
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, NoStatus, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByAnnual, 0, NoStatus, Voluntary, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out for current single year followed by Annual - With updates" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, NoStatus, Voluntary, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, NoStatus, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, NoStatus, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc1
        pageDocument.getElementById("opt-out-question-inset").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesInset
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc2
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, NoStatus, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByAnnual, 1, NoStatus, Voluntary, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out of the next year - Annual CY" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, NoStatus, Annual, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, NoStatus, Annual, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, NoStatus, Annual, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultDesc1
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, NoStatus, Annual, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, NoStatus, Annual, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out of the next year - Mandated CY" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, NoStatus, Mandated, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, NoStatus, Mandated, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, NoStatus, Mandated, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc2
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, NoStatus, Mandated, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, NoStatus, Mandated, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out for CY-1 in V-M-V scenario" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Mandated, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for the next year in V-M-V scenario with submitted updates - Single-year Opt-Out" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, Voluntary, Mandated, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, Voluntary, Mandated, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, Voluntary, Mandated, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc2
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, Voluntary, Mandated, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, Voluntary, Mandated, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out from the previous year in V-V-A scenario without submitted updates - Multi-year Opt-Out" should {
      "have the correct title" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousYearTitleMulti
      }
      "have the correct heading" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearHeadingMulti
      }
      "have the correct description" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc2Multi
      }
      "have the correct radio question" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for CY in V-V-M scenario" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Mandated, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }

    "opting out for CY-1 in V-D-V scenario" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Dormant, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Dormant, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Dormant, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Dormant, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByMandated, 2, Voluntary, Dormant, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for CY+1 in V-D-V scenario" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, Voluntary, Dormant, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, Voluntary, Dormant, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, Voluntary, Dormant, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultDesc1
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, Voluntary, Dormant, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, Voluntary, Dormant, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out for CY in V-V-D scenario" should {
      "have the correct title" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Dormant) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedTitle
      }
      "have the correct heading" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Dormant) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading
      }
      "have the correct description" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Dormant) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1
      }
      "have the correct radio question" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Dormant) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion
      }
      "display the correct error message when form has errors" in new Setup("current", OneYearOptOutFollowedByMandated, 0, NoStatus, Voluntary, Dormant, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2025 to 2026 tax year"
      }
    }
    "opting out for CY-1 in V-A-V scenario" should {
      "have the correct title" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearTitle
      }
      "have the correct heading" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearHeading
      }
      "have the correct description" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc1
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearDesc2
      }
      "have the correct radio question" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousSingleYearQuestion
      }
      "display the correct error message when form has errors" in new Setup("previous", OneYearOptOutFollowedByAnnual, 0, Voluntary, Annual, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for CY+1 in V-A-V scenario" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, Voluntary, Annual, Voluntary) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, Voluntary, Annual, Voluntary) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, Voluntary, Annual, Voluntary) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultDesc1
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, Voluntary, Annual, Voluntary) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, Voluntary, Annual, Voluntary, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }

    "opting out for CY-1 in V-V-A scenario" should {
      "have the correct heading" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearHeadingMulti
      }
      "have the correct description" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc1Multi
        pageDocument.getElementById("opt-out-question-desc-2").text() shouldBe OptOutTaxYearQuestionMessages.previousYearDesc2Multi
      }
      "have the correct radio question" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.previousYearQuestionMulti
      }
      "display the correct error message when form has errors" in new Setup("previous", MultiYearOptOutDefault, 0, Voluntary, Voluntary, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2024 to 2025 tax year"
      }
    }

    "opting out for CY+1 in V-V-A scenario" should {
      "have the correct title" in new Setup("next", NextYearOptOut, 0, Voluntary, Voluntary, Annual) {
        pageDocument.title() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultTitle
      }
      "have the correct heading" in new Setup("next", NextYearOptOut, 0, Voluntary, Voluntary, Annual) {
        pageDocument.select("h1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultHeading
      }
      "have the correct description" in new Setup("next", NextYearOptOut, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementById("opt-out-question-desc-1").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultDesc1
      }
      "have the correct radio question" in new Setup("next", NextYearOptOut, 0, Voluntary, Voluntary, Annual) {
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe OptOutTaxYearQuestionMessages.nextYearOptOutDefaultQuestion
      }
      "display the correct error message when form has errors" in new Setup("next", NextYearOptOut, 0, Voluntary, Voluntary, Annual, withError = true) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe "There is a problem"
        pageDocument.getElementsByClass("govuk-error-summary__body").text() shouldBe "Select yes to opt out for the 2026 to 2027 tax year"
      }
    }
  }
}
