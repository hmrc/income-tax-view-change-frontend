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
  val addStartDateMessagesPrefix: String
  val addStartDateCheckMessagesPrefix: String
  val addStartDateSessionKey: String
}

case object SelfEmployment extends IncomeSourceType {
  override val key = "SE"
  override val addStartDateMessagesPrefix = "add-business-start-date"
  override val addStartDateCheckMessagesPrefix = "add-business-start-date-check"
  override val addStartDateSessionKey = SessionKeys.addBusinessStartDate

}

case object UkProperty extends IncomeSourceType {
  override val key = "UK"
  override val addStartDateMessagesPrefix = "incomeSources.add.UKPropertyStartDate"
  override val addStartDateCheckMessagesPrefix = "add-uk-property-start-date-check"
  override val addStartDateSessionKey = SessionKeys.addUkPropertyStartDate
}

case object ForeignProperty extends IncomeSourceType {
  override val key = "FP"
  override val addStartDateMessagesPrefix = "incomeSources.add.foreignProperty.startDate"
  override val addStartDateCheckMessagesPrefix = "add-foreign-property-start-date-check"
  override val addStartDateSessionKey = SessionKeys.foreignPropertyStartDate
}
