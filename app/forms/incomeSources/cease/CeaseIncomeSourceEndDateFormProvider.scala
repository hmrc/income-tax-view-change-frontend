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
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Valid}
import play.api.i18n.Messages
import services.DateServiceInterface

import java.time.LocalDate

class CeaseIncomeSourceEndDateFormProvider extends Mappings {

  def apply(incomeSourceType: IncomeSourceType, id: Option[String] = None, newIncomeSourceJourney: Boolean)
           (implicit user: MtdItUser[_], dateService: DateServiceInterface, messages: Messages): Form[LocalDate] = {

    val messagePrefix = incomeSourceType.endDateMessagePrefix
    val dateFormPrefix = "dateForm.error"
    val invalidMessage = if (newIncomeSourceJourney) "dateForm.error.invalid" else
      s"$messagePrefix.error.invalid"

    def dateMustBeAfterBusinessStartDate(incomeSourceType: IncomeSourceType): String =
      s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeStartDate"

    def dateMustNotBeBefore6April2015(incomeSourceType: IncomeSourceType): String =
      s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeEarliestDate"

    def dateMustNotBeInFuture(incomeSourceType: IncomeSourceType): String = s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.future"

    val checkMinDateIfSE: Constraint[LocalDate] = {
      if (incomeSourceType == SelfEmployment) minDate(LocalDate.of(2015, 4, 6), dateMustNotBeBefore6April2015(SelfEmployment))
      else Constraint { _ => Valid }
    }

    val minimumDate: Constraint[LocalDate] = incomeSourceType match {
      case UkProperty =>
        val ukStartDate = user.incomeSources.properties.filter(_.isUkProperty).filter(!_.isCeased).map(_.getTradingStartDateForCessation).headOption.getOrElse(LocalDate.MIN)

        minDate(ukStartDate, dateMustBeAfterBusinessStartDate(UkProperty))
      case ForeignProperty =>
        val foreignStartDate = user.incomeSources.properties.filter(_.isForeignProperty).filter(!_.isCeased).map(_.getTradingStartDateForCessation).headOption.getOrElse(LocalDate.MIN)

        minDate(foreignStartDate, dateMustBeAfterBusinessStartDate(ForeignProperty))
      case SelfEmployment =>
        val errorMessage: String = "missing income source ID"
        val incomeSourceId = id.getOrElse(throw new Exception(errorMessage))
        val businessStartDate = user.incomeSources.businesses.find(_.incomeSourceId == incomeSourceId).map(_.getTradingStartDateForCessation).getOrElse(LocalDate.MIN)

        minDate(businessStartDate, dateMustBeAfterBusinessStartDate(SelfEmployment))
    }

    val currentDate = dateService.getCurrentDate

    Form(
      "value" -> localDate(
        invalidKey = invalidMessage,
        allRequiredKey = s"$dateFormPrefix.dayMonthAndYear.required.${incomeSourceType.key.toLowerCase}",
        twoRequiredKey = s"$dateFormPrefix.required.two",
        requiredKey = s"$dateFormPrefix.required"
      ).verifying(firstError(
        minimumDate,
        checkMinDateIfSE,
        fourDigitValidYear(invalidMessage),
        maxDate(currentDate, dateMustNotBeInFuture(incomeSourceType)))
      )
    )
  }
}
