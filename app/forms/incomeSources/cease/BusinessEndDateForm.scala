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

package forms.incomeSources.cease

import auth.MtdItUser
import forms.models.DateFormElement
import forms.validation.Constraints
import play.api.data
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import services.DateService

import java.time.LocalDate
import javax.inject.Inject

class BusinessEndDateForm @Inject()(val dateService: DateService) extends Constraints {

  val dateMustBeComplete = "incomeSources.cease.BusinessEndDate.error.incomplete.all"
  val dateMustNotBeMissingDayField = "incomeSources.cease.BusinessEndDate.error.incomplete.day"
  val dateMustNotBeMissingMonthField = "incomeSources.cease.BusinessEndDate.error.incomplete.month"
  val dateMustNotBeMissingYearField = "incomeSources.cease.BusinessEndDate.error.incomplete.year"
  val dateMustNotBeMissingDayAndMonthField = "incomeSources.cease.BusinessEndDate.error.incomplete.day.and.month"
  val dateMustNotBeMissingDayAndYearField = "incomeSources.cease.BusinessEndDate.error.incomplete.day.and.year"
  val dateMustNotBeMissingMonthAndYearField = "incomeSources.cease.BusinessEndDate.error.incomplete.month.and.year"
  val dateMustNotBeInvalid = "incomeSources.cease.BusinessEndDate.error.invalid"
  val dateMustNotBeInFuture = "incomeSources.cease.BusinessEndDate.error.future"
  val dateMustBeAfterBusinessStartDate = "incomeSources.cease.BusinessEndDate.error.beforeStartDate"
  val dateMustNotBeBefore6April2015 = "incomeSources.cease.BusinessEndDate.error.beforeEarliestDate"
  val sixthAprilTwentyFifteen: LocalDate = LocalDate.of(2015, 4, 6)

  def apply (implicit user: MtdItUser[_], businessStartDate: Option[LocalDate]): Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate()

    Form(
      mapping("business-end-date" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields,
          validDate(dateMustNotBeInvalid)
        )).transform[LocalDate](
        {
          case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt)
        },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(maxDate(currentDate, dateMustNotBeInFuture))
        .verifying(minDate(businessStartDate.getOrElse(LocalDate.MIN), dateMustBeAfterBusinessStartDate))
        .verifying(minDate6April2015(dateMustNotBeBefore6April2015))
      )(DateFormElement.apply)(DateFormElement.unapply))

  }

  private def checkRequiredFields: Constraint[(String, String, String)] = Constraint("constraints.requiredFields") {
    case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustBeComplete)))
    case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingDayAndYearField)))
    case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingDayAndMonthField)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingMonthAndYearField)))
    case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingDayField)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingMonthField)))
    case (day, month, year) if day.trim.nonEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustNotBeMissingYearField)))
    case _ =>
      Valid
  }

  object BusinessEndDateForm {
    def apply(dateService: DateService)(implicit user: MtdItUser[_]): Form[DateFormElement] = {
      new BusinessEndDateForm(dateService).apply(user, None)
    }
  }
}
