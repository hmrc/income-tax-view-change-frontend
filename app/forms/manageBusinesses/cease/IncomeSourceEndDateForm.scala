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

package forms.manageBusinesses.cease

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.models.DateFormElement
import forms.validation.CustomConstraints
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import services.DateService

import java.time.LocalDate
import javax.inject.Inject

class IncomeSourceEndDateForm @Inject()(dateService: DateService)(implicit val appConfig: FrontendAppConfig) extends CustomConstraints with FeatureSwitching {

  val dateMustBeCompleteSE = "dateForm.error.dayMonthAndYear.required.se"
  val dateMustBeCompleteUK = "dateForm.error.dayMonthAndYear.required.uk"
  val dateMustBeCompleteFP = "dateForm.error.dayMonthAndYear.required.fp"
  val dateMustNotBeMissingDayField = "dateForm.error.day.required"
  val dateMustNotBeMissingMonthField = "dateForm.error.month.required"
  val dateMustNotBeMissingYearField = "dateForm.error.year.required"
  val dateMustNotBeMissingDayAndMonthField = "dateForm.error.dayAndMonth.required"
  val dateMustNotBeMissingDayAndYearField = "dateForm.error.dayAndYear.required"
  val dateMustNotBeMissingMonthAndYearField = "dateForm.error.monthAndYear.required"

  def dateMustNotBeInFuture(incomeSourceType: IncomeSourceType) = s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.future"

  def dateMustBeAfterBusinessStartDate(incomeSourceType: IncomeSourceType) = s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeStartDate"

  def dateMustNotBeBefore6April2015(incomeSourceType: IncomeSourceType) = s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeEarliestDate"

  def apply(incomeSourceType: IncomeSourceType, id: Option[String] = None)(implicit user: MtdItUser[_]): Form[DateFormElement] = {

    val currentDate: LocalDate = dateService.getCurrentDate

    val dateConstraints: List[Constraint[LocalDate]] = {

      val minimumDateConstraints = incomeSourceType match {
        case UkProperty =>
          val ukStartDate = user.incomeSources.properties.filter(_.isUkProperty).filter(!_.isCeased).map(_.getTradingStartDateForCessation).headOption.getOrElse(LocalDate.MIN)

          List(minDate(ukStartDate, dateMustBeAfterBusinessStartDate(UkProperty)))
        case ForeignProperty =>
          val foreignStartDate = user.incomeSources.properties.filter(_.isForeignProperty).filter(!_.isCeased).map(_.getTradingStartDateForCessation).headOption.getOrElse(LocalDate.MIN)

          List(minDate(foreignStartDate, dateMustBeAfterBusinessStartDate(ForeignProperty)))
        case SelfEmployment =>
          val errorMessage: String = "missing income source ID"
          val incomeSourceId = id.getOrElse(throw new Exception(errorMessage))
          val businessStartDate = user.incomeSources.businesses.find(_.incomeSourceId == incomeSourceId).map(_.getTradingStartDateForCessation).getOrElse(LocalDate.MIN)

          List(minDate(businessStartDate, dateMustBeAfterBusinessStartDate(SelfEmployment)),
            minDate(LocalDate.of(2015, 4, 6), dateMustNotBeBefore6April2015(SelfEmployment)))
      }

      minimumDateConstraints :+ maxDate(currentDate, dateMustNotBeInFuture(incomeSourceType))
    }

    Form(
      mapping("income-source-end-date" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(
          checkRequiredFields(incomeSourceType),
          validDate("dateForm.error.invalid")
        )).transform[LocalDate](
        {
          case (day, month, year) =>
            LocalDate.of(year.toInt, month.toInt, day.toInt)
        },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(firstError(dateConstraints: _*))
      )(
        date =>
          DateFormElement(date)
      )(
        form =>
          Some(form.date)
      ))
  }

  private def dateMustBeCompleteKey(incomeSourceType: IncomeSourceType): String = incomeSourceType match {
    case SelfEmployment => dateMustBeCompleteSE
    case UkProperty => dateMustBeCompleteUK
    case ForeignProperty => dateMustBeCompleteFP
  }

  private def checkRequiredFields(incomeSourceType: IncomeSourceType): Constraint[(String, String, String)] = Constraint("constraints.requiredFields") {
    case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.isEmpty =>
      Invalid(Seq(ValidationError(dateMustBeCompleteKey(incomeSourceType))))
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
