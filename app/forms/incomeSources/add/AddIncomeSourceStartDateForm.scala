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

import forms.models.DateFormElement
import forms.validation.CustomConstraints
import implicits.ImplicitDateFormatter
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import services.DateServiceInterface

import java.time.LocalDate

object AddIncomeSourceStartDateForm extends CustomConstraints {

  def apply(messagesPrefix: String)(implicit messages: Messages, dateService: DateServiceInterface, dateFormatter: ImplicitDateFormatter): Form[DateFormElement] = {

    val dateMustNotBeTooFarInFuture = s"$messagesPrefix.error.future"
    val dateMustBeEntered = s"$messagesPrefix.error.required"
    val dateMustBeReal = s"$messagesPrefix.error.invalid"
    val dayRequired = "dateForm.error.day.required"
    val monthRequired = "dateForm.error.month.required"
    val yearRequired = "dateForm.error.year.required"
    val dayAndMonthRequired = "dateForm.error.dayAndMonth.required"
    val dayAndYearRequired = "dateForm.error.dayAndYear.required"
    val monthAndYearRequired = "dateForm.error.monthAndYear.required"

    val dayInputFieldName: String = "day"
    val monthInputFieldName: String = "month"
    val yearInputFieldName: String = "year"

    val maximumAllowableDate: LocalDate = dateService.getCurrentDate().plusDays(6)
    val maximumAllowableDatePlusOneDay: LocalDate = maximumAllowableDate.plusDays(1)
    val futureErrorMessage: String = dateFormatter.longDate(maximumAllowableDatePlusOneDay).toLongDate

    def dateMustNotBeInTheFuture(date: String): String = messages(dateMustNotBeTooFarInFuture, date)

    def checkRequiredFields: Constraint[(String, String, String)] = Constraint("constraints.requiredFields")(date => {
      val (day, month, year) = date
      val Empty = true
      val NonEmpty = false
      (day.trim.isEmpty, month.trim.isEmpty, year.trim.isEmpty) match {
        case (Empty, Empty, Empty) => Invalid(Seq(ValidationError(dateMustBeEntered)))
        case (Empty, NonEmpty, Empty) => Invalid(Seq(ValidationError(dayAndYearRequired)))
        case (Empty, Empty, NonEmpty) => Invalid(Seq(ValidationError(dayAndMonthRequired)))
        case (NonEmpty, Empty, Empty) => Invalid(Seq(ValidationError(monthAndYearRequired)))
        case (Empty, NonEmpty, NonEmpty) => Invalid(Seq(ValidationError(dayRequired)))
        case (NonEmpty, Empty, NonEmpty) => Invalid(Seq(ValidationError(monthRequired)))
        case (NonEmpty, NonEmpty, Empty) => Invalid(Seq(ValidationError(yearRequired)))
        case _ => Valid
      }
    })

    Form(
      mapping("income-source-start-date" -> tuple(
        dayInputFieldName -> default(text(), ""),
        monthInputFieldName -> default(text(), ""),
        yearInputFieldName -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields,
          validDate(dateMustBeReal)
        )).transform[LocalDate](
          {
            case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt)
          },
          date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
        )
        .verifying(maxDate(maximumAllowableDate, dateMustNotBeInTheFuture(futureErrorMessage)))
      )(DateFormElement.apply)(DateFormElement.unapply))
  }
}

class AddIncomeSourceStartDateForm