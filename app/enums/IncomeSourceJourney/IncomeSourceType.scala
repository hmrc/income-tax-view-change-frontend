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

package enums.IncomeSourceJourney

import forms.utils.SessionKeys

sealed trait IncomeSourceType {
  val key: String
  val startDateMessagesPrefix: String
  val addStartDateCheckMessagesPrefix: String
  val endDateMessagePrefix: String
  val startDateSessionKey: String
  val endDateSessionKey: String
}

case object SelfEmployment extends IncomeSourceType {
  override val key = "SE"
  override val startDateMessagesPrefix: String = "add-business-start-date"
  override val addStartDateCheckMessagesPrefix: String = "add-business-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.selfEmployment"
  override val startDateSessionKey: String = SessionKeys.addBusinessStartDate
  override val endDateSessionKey: String = SessionKeys.ceaseBusinessEndDate

}

case object UkProperty extends IncomeSourceType {
  override val key = "UK"
  override val startDateMessagesPrefix: String = "incomeSources.add.UKPropertyStartDate"
  override val addStartDateCheckMessagesPrefix: String = "add-uk-property-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.ukProperty"
  override val startDateSessionKey: String = SessionKeys.addUkPropertyStartDate
  override val endDateSessionKey: String = SessionKeys.ceaseUKPropertyEndDate
}

case object ForeignProperty extends IncomeSourceType {
  override val key = "FP"
  override val startDateMessagesPrefix: String = "incomeSources.add.foreignProperty.startDate"
  override val addStartDateCheckMessagesPrefix: String = "add-foreign-property-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.foreignProperty"
  override val startDateSessionKey: String = SessionKeys.foreignPropertyStartDate
  override val endDateSessionKey: String = SessionKeys.ceaseForeignPropertyEndDate
}

object IncomeSourceType {
  def get(key: String): Either[Exception, IncomeSourceType] = {
    key match {
      case "FP" => Right(ForeignProperty)
      case "UK" => Right(UkProperty)
      case "SE" => Right(SelfEmployment)
      case _ => Left(new Exception("Invalid incomeSourceType"))
    }
  }
}
