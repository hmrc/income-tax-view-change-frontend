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
import controllers.predicates.IncomeSourceDetailsPredicate
import forms.models.DateFormElement
import forms.validation.Constraints
import play.api.data.Form
import play.api.data.Forms._
import services.{DateService, IncomeSourceDetailsService}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class DateUKPropertyCeasedForm @Inject()(val dateService: DateService,
                                         val incomeSourceDetailsService: IncomeSourceDetailsService,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate) extends Constraints {
  val dateMustBeEntered = "incomeSources.cease.dateUKPropertyCeased.dateMustBeEntered"
  val dateMustBeReal = "incomeSources.cease.dateUKPropertyCeased.dateMustBeReal"
  val dateMustNotBeInTheFuture = "incomeSources.cease.dateUKPropertyCeased.dateMustNotBeInTheFuture"
  val dateMustBeAfterStartDate = "incomeSources.cease.dateUKPropertyCeased.dateMustBeAfterStartDate"

  def apply(implicit user: MtdItUser[_]): Form[DateFormElement] = {
    val propertyBusinessStartDate: Option[LocalDate] = user.incomeSources.properties.filter(_.isUkProperty).map(_.tradingStartDate).head
    Form(
      mapping("date-uk-property-stopped" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(nonEmptyDate(dateMustBeEntered),
          validDate(dateMustBeReal))
        ).transform[LocalDate](
        { case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt) },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(minDate(propertyBusinessStartDate.getOrElse(LocalDate.MIN), dateMustBeAfterStartDate))
        .verifying(maxDate(dateService.getCurrentDate(), dateMustNotBeInTheFuture))
      )(DateFormElement.apply)(DateFormElement.unapply))
  }
}
