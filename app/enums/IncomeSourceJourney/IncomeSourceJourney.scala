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

sealed trait IncomeSourceJourney {
  val key: String
  val messagesPrefix: String
}

case object SelfEmployment extends IncomeSourceJourney {
  override val key = "SE"
  override val messagesPrefix = "add-business-start-date"
}

case object UkProperty extends IncomeSourceJourney {
  override val key = "UK"
  override val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
}

case object ForeignProperty extends IncomeSourceJourney {
  override val key = "FP"
  override val messagesPrefix = "incomeSources.add.foreignProperty.startDate"
}
