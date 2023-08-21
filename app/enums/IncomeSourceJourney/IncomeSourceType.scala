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

sealed trait IncomeSourceType {
  val key: String
  val addIncomeSourceStartDateMessagesPrefix: String
  val addIncomeSourceStartDateCheckMessagesPrefix: String
}

case object SelfEmployment extends IncomeSourceType {
  override val key = "SE"
  override val addIncomeSourceStartDateMessagesPrefix = "add-business-start-date"
  override val addIncomeSourceStartDateCheckMessagesPrefix = "add-business-start-date-check"

}

case object UkProperty extends IncomeSourceType {
  override val key = "UK"
  override val addIncomeSourceStartDateMessagesPrefix = "incomeSources.add.UKPropertyStartDate"
  override val addIncomeSourceStartDateCheckMessagesPrefix = "add-uk-property-start-date-check"
}

case object ForeignProperty extends IncomeSourceType {
  override val key = "FP"
  override val addIncomeSourceStartDateMessagesPrefix = "incomeSources.add.foreignProperty.startDate"
  override val addIncomeSourceStartDateCheckMessagesPrefix = "add-foreign-property-start-date-check"

}
