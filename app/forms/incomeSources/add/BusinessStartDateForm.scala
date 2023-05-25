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

import auth.MtdItUser
import forms.models.DateFormElement
import forms.validation.Constraints
import implicits.ImplicitDateFormatterImpl
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import services.DateService

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class BusinessStartDateForm @Inject()(dateService: DateService, dateFormatter: ImplicitDateFormatterImpl, messages: Messages) extends Constraints {

  private val messagePrefix = "add-business-start-date"
  private val dateMustNotBeTooFarInFuture = s"$messagePrefix.error.future"
  private val dateMustBeEntered = s"$messagePrefix.error.error.required"
  private val dateMustBeReal = s"$messagePrefix.error.invalid"
  private val dayRequired = s"dateForm.error.day.required"
  private val monthRequired = s"dateForm.error.month.required"
  private val yearRequired = s"dateForm.error.year.required"
  private val dayAndMonthRequired = s"dateForm.error.dayAndMonth.required"
  private val dayAndYearRequired = s"dateForm.error.dayAndYear.required"
  private val monthAndYearRequired = s"dateForm.error.monthAndYear.required"

  val day: String = "day"
  val month: String = "month"
  val year: String = "year"

  val form: Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate()
    val currentDatePlusOneWeek: String = dateFormatter.longDate(currentDate.plusWeeks(1))(messages).toLongDate
    def dateMustNotBeInTheFuture(maximumDate: String): String = messages(s"$messagePrefix.error.future", maximumDate)

    Form(
      mapping(s"$messagePrefix" -> tuple(
        day -> default(text(), ""),
        month -> default(text(), ""),
        year -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields,
          validDate(dateMustBeReal)
        )).transform[LocalDate](
        {
          case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt)
        },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      )
        .verifying(maxDate(currentDate, dateMustNotBeInTheFuture(currentDatePlusOneWeek)))
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

object BusinessStartDateForm {
  def apply()(implicit messages: Messages, dateService: DateService, dateFormatter: ImplicitDateFormatterImpl): Form[DateFormElement] = {
    new BusinessStartDateForm(dateService, dateFormatter, messages).form
  }
}
