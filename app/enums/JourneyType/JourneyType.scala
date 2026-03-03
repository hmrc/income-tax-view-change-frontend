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

package enums.JourneyType

import enums.IncomeSourceJourney.IncomeSourceType

sealed trait JourneyType

case class IncomeSourceJourneyType(operation: Operation, businessType: IncomeSourceType) extends JourneyType {
  override def toString: String = operation.operationType + "-" + businessType.key
}

sealed trait Operation {
  val operationType: String
}

case object Add extends Operation {
  override val operationType = "ADD"
}

case object Manage extends Operation {
  override val operationType = "MANAGE"
}

case object Cease extends Operation {
  override val operationType = "CEASE"
}

case class Opt(optJourney: OptJourney) extends JourneyType {
  override def toString: String = optJourney.toString
}

sealed trait OptJourney extends JourneyType

case object SignUpJourney extends OptJourney {
  override val toString = "SIGNUP"
}

case object OptOutJourney extends OptJourney {
  override val toString = "OPTOUT"
}

case object TriggeredMigrationJourney extends JourneyType {
  override def toString: String = "TRIGGERED-MIGRATION"
}