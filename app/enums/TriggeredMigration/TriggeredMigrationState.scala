/*
 * Copyright 2025 HM Revenue & Customs
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

package enums.TriggeredMigration

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import TriggeredMigrationState.*

enum TriggeredMigrationState(val messageKeyValue: String, val anchorLinkId: String):
  case TriggeredMigrationCeased extends TriggeredMigrationState("", "#ceased-section-heading")
  case TriggeredMigrationAdded(incomeSourceType: IncomeSourceType) extends TriggeredMigrationState(s"${incomeSourceType.key}", TriggeredMigrationState.getAnchorLinkId(incomeSourceType))

object TriggeredMigrationState:
  def getAnchorLinkId(incomeSourceType: IncomeSourceType): String = incomeSourceType match
    case SelfEmployment => "#sole-trader-heading"
    case UkProperty => "#uk-property-heading"
    case ForeignProperty => "#foreign-property-heading"

  def getStateFromString(state: Option[String]): Option[TriggeredMigrationState] =
    state.flatMap {
      case "CEASED" => Some(TriggeredMigrationCeased)
      case trigState if trigState.endsWith("-ADDED") =>
        trigState.stripSuffix("-ADDED") match {
          case SelfEmployment.key => Some(TriggeredMigrationAdded(SelfEmployment))
          case UkProperty.key => Some(TriggeredMigrationAdded(UkProperty))
          case ForeignProperty.key => Some(TriggeredMigrationAdded(ForeignProperty))
          case _ => None
        }
      case _ => None
    }

object TriggeredMigrationCeased:
  override def toString: String = "CEASED"
//TODO fix it
/*object TriggeredMigrationAdded:
  override def toString = s"${this.incomeSourceType.key}-ADDED"*/

/*sealed trait TriggeredMigrationState {
  val messageKeyValue: String
  val anchorLinkId: String
}

case object TriggeredMigrationCeased extends TriggeredMigrationState {
  override val toString = "CEASED"
  override val messageKeyValue: String = ""
  override val anchorLinkId: String = "#ceased-section-heading"
}

case class TriggeredMigrationAdded(incomeSourceType: IncomeSourceType) extends TriggeredMigrationState {
  override val toString = s"${incomeSourceType.key}-ADDED"
  override val messageKeyValue = s"${incomeSourceType.key}"
  override val anchorLinkId: String = incomeSourceType match {
    case SelfEmployment  => "#sole-trader-heading"
    case UkProperty      => "#uk-property-heading"
    case ForeignProperty => "#foreign-property-heading"
  }
}

object TriggeredMigrationState {
  def getStateFromString(state: Option[String]): Option[TriggeredMigrationState] = {
    state.flatMap {
      case TriggeredMigrationCeased.toString => Some(TriggeredMigrationCeased)
      case trigState if trigState.endsWith("-ADDED") =>
        trigState.stripSuffix("-ADDED") match {
          case SelfEmployment.key  => Some(TriggeredMigrationAdded(SelfEmployment))
          case UkProperty.key      => Some(TriggeredMigrationAdded(UkProperty))
          case ForeignProperty.key => Some(TriggeredMigrationAdded(ForeignProperty))
          case _                   => None
        }
      case _ => None
    }
  }
}*/