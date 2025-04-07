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

package forms.incomeSources.add

import forms.mappings.Mappings
import implicits.ImplicitDateFormatter
import play.api.data.Form
import play.api.i18n.Messages
import services.DateServiceInterface

import java.time.LocalDate

class AddIncomeSourceStartDateFormProvider extends Mappings {

  def apply(messagesPrefix: String)(implicit messages: Messages, dateService: DateServiceInterface, dateFormatter: ImplicitDateFormatter): Form[LocalDate] = {

    // We tested max date to validation, allowing users to submit a date 6 days in advance/future
    val maximumAllowableDate: LocalDate = dateService.getCurrentDate.plusDays(6) // tested 11-4-2025
    val earliestInvalidDate: LocalDate = maximumAllowableDate.plusDays(1)

    val earliestDatePossible = LocalDate.of(1900, 1, 1)

    val earliestLongInvalidDate: String = dateFormatter.longDate(earliestInvalidDate).toLongDate
    val invalidFutureDateErrorMessage: String = messages(s"$messagesPrefix.error.future", earliestLongInvalidDate)

    val dateFormErrorPrefix = "dateForm.error"
    val invalidMessage = s"$dateFormErrorPrefix.invalid"

    Form(
      "value" -> localDate(
        invalidKey = s"$dateFormErrorPrefix.invalid",
        allRequiredKey = s"$messagesPrefix.required.all",
        twoRequiredKey = s"$dateFormErrorPrefix.required.two",
        requiredKey = s"$dateFormErrorPrefix.required"
      ).verifying(
        maxDate(maximumAllowableDate, invalidFutureDateErrorMessage), // max date in future error handling / form validation
        fourDigitValidYear(invalidMessage), // where is earliest day/year?
        minDate(
          minimum = LocalDate.of(1900, 1, 1), // new validation code/date to be added
          errorKey = "The earliest date must be after 1-1-1900"  //  we would use a message key here, with content driven from the UCD team
        )
      )
    )
  }
}
