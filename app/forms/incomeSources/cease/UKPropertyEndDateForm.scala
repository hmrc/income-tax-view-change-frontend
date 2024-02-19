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
import forms.validation.CustomConstraints
import play.api.data.Form
import play.api.data.Forms._
import services.DateServiceInterface

import java.time.LocalDate
import javax.inject.{Inject, Singleton}


@Singleton
class UKPropertyEndDateForm @Inject()(val dateService: DateServiceInterface) extends CustomConstraints {

  val dateMustBeEntered = "incomeSources.cease.UKPropertyEndDate.error.incomplete"
  val dateMustBeReal = "incomeSources.cease.UKPropertyEndDate.error.invalid"
  val dateMustNotBeInTheFuture = "incomeSources.cease.UKPropertyEndDate.error.future"
  val dateMustBeAfterStartDate = "incomeSources.cease.UKPropertyEndDate.error.beforeStartDate"


  def apply(implicit user: MtdItUser[_]): Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate
    val UKPropertyStartDate: Option[LocalDate] = user.incomeSources.properties.filter(_.isUkProperty).flatMap(_.tradingStartDate).headOption

    Form(
      mapping("uk-property-end-date" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(nonEmptyDateFields(dateMustBeEntered),
          validDate(dateMustBeReal))
        ).transform[LocalDate](
        { case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt) },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(minDate(UKPropertyStartDate.getOrElse(LocalDate.MIN), dateMustBeAfterStartDate))
        .verifying(maxDate(currentDate, dateMustNotBeInTheFuture))
      )(DateFormElement.apply)(DateFormElement.unapply))
  }
}
