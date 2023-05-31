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
import forms.validation.Constraints
import implicits.ImplicitDateFormatterImpl
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import services.DateService

import java.time.LocalDate


object AddUKPropertyStartDateForm extends Constraints {

  private val dateMustBeEntered = "incomeSources.add.UKPropertyStartDate.error.required"
  private val dateMustBeReal = "incomeSources.add.UKPropertyStartDate.error.invalid"
  private val dayRequired = "dateForm.error.day.required"
  private val monthRequired = "dateForm.error.month.required"
  private val yearRequired = "dateForm.error.year.required"
  private val dayAndMonthRequired = "dateForm.error.dayAndMonth.required"
  private val dayAndYearRequired = "dateForm.error.dayAndYear.required"
  private val monthAndYearRequired = "dateForm.error.monthAndYear.required"

  def apply()(implicit dateFormatter: ImplicitDateFormatterImpl, dateService: DateService, messages: Messages): Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate()
    val currentDatePlusOneWeek: LocalDate = currentDate.plusWeeks(1)
    val currentDatePlusOneWeekFormatted: String = dateFormatter.longDate(currentDate.plusWeeks(1)).toLongDate

    def dateMustNotBeInTheFuture(maximumDate: String): String = messages("incomeSources.add.UKPropertyStartDate.error.future", maximumDate)

    Form(
      mapping("add-uk-property-start-date" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields,
          validDate(dateMustBeReal)
        )).transform[LocalDate](
        {
          case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt)
        },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(maxDate(currentDatePlusOneWeek, dateMustNotBeInTheFuture(currentDatePlusOneWeekFormatted))),
      )(DateFormElement.apply)(DateFormElement.unapply))
  }

  private def checkRequiredFields: Constraint[(String, String, String)] = Constraint("constraints.requiredFields") {
    case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustBeEntered)))
    case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dayAndYearRequired)))
    case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(dayAndMonthRequired)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(monthAndYearRequired)))
    case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(dayRequired)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(monthRequired)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(yearRequired)))
    case _ =>
      Valid
  }
}

