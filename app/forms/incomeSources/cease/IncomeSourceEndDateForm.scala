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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.models.DateFormElement
import forms.validation.Constraints
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import services.DateService
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import javax.inject.Inject

class IncomeSourceEndDateForm @Inject()(val dateService: DateService) extends Constraints {

  val dateMustBeComplete = "dateForm.error.dayMonthAndYear.required"
  val dateMustNotBeMissingDayField = "dateForm.error.day.required"
  val dateMustNotBeMissingMonthField = "dateForm.error.month.required"
  val dateMustNotBeMissingYearField = "dateForm.error.year.required"
  val dateMustNotBeMissingDayAndMonthField = "dateForm.error.dayAndMonth.required"
  val dateMustNotBeMissingDayAndYearField = "dateForm.error.dayAndYear.required"
  val dateMustNotBeMissingMonthAndYearField = "dateForm.error.monthAndYear.required"
  val dateMustNotBeInvalid = "error.invalid"
  val dateMustNotBeInFuture = "dateForm.error.future"
  val dateMustBeAfterBusinessStartDate = "dateFrom.error.beforeStartDate"
  val dateMustNotBeBefore6April2015 = "incomeSources.cease.endDate.selfEmployment.error.beforeEarliestDate"

  def apply(incomeSourceType: IncomeSourceType, id: Option[String] = None)(implicit user: MtdItUser[_]): Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate()
    val messagePrefix = incomeSourceType.endDateMessagePrefix
    val dateConstraints: List[Constraint[LocalDate]] = {

      val minimumDateConstraints = incomeSourceType match {
        case UkProperty =>
          val businessStartDate = user.incomeSources.properties.filter(_.isUkProperty).flatMap(_.tradingStartDate)
            .headOption.getOrElse(LocalDate.MIN)
          List(minDate(businessStartDate, dateMustBeAfterBusinessStartDate))
        case ForeignProperty =>
          val businessStartDate = user.incomeSources.properties.filter(_.isForeignProperty).flatMap(_.tradingStartDate)
            .headOption.getOrElse(LocalDate.MIN)
          List(minDate(businessStartDate, dateMustBeAfterBusinessStartDate))
        case SelfEmployment =>
          val incomeSourceId = id.get
          val businessStartDate = user.incomeSources.businesses
            .find(_.incomeSourceId == incomeSourceId).flatMap(_.tradingStartDate).getOrElse(LocalDate.MIN)
          List(minDate(businessStartDate, dateMustBeAfterBusinessStartDate),
            minDate(LocalDate.of(2015, 4, 6), dateMustNotBeBefore6April2015))
      }

      minimumDateConstraints :+ maxDate(currentDate, dateMustNotBeInFuture)
    }

    Form(
      mapping("income-source-end-date" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields,
          validDate(s"$messagePrefix.$dateMustNotBeInvalid")
        )).transform[LocalDate](
        {
          case (day, month, year) =>
            LocalDate.of(year.toInt, month.toInt, day.toInt)
        },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(firstError(dateConstraints: _*))
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

}
