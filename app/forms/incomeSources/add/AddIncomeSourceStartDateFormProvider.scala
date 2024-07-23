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
import forms.models.DateFormElement
import forms.validation.CustomConstraints
import implicits.ImplicitDateFormatter
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import services.DateServiceInterface

import java.time.LocalDate

class AddIncomeSourceStartDateFormProvider extends Mappings {

  def apply(messagesPrefix: String)(implicit messages: Messages, dateService: DateServiceInterface, dateFormatter: ImplicitDateFormatter): Form[LocalDate] = {

    val maximumAllowableDate: LocalDate = dateService.getCurrentDate.plusDays(6)
    val earliestInvalidDate: LocalDate = maximumAllowableDate.plusDays(1)

    val earliestLongInvalidDate: String = dateFormatter.longDate(earliestInvalidDate).toLongDate
    val invalidFutureDateErrorMessage = messages(s"$messagesPrefix.error.future", earliestLongInvalidDate)

    Form(
      "value" -> localDate(
        invalidKey     = s"$messagesPrefix.date.error.invalid",
        allRequiredKey = s"$messagesPrefix.date.error.required.all",
        twoRequiredKey = s"$messagesPrefix.date.error.required.two",
        requiredKey    = s"$messagesPrefix.date.error.required"
      ).verifying(
        maxDate(maximumAllowableDate, invalidFutureDateErrorMessage, maximumAllowableDate.formatAsString)
      )
    )
  }
}
