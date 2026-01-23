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

package models.triggeredMigration.viewModels
import enums.TriggeredMigration.TriggeredMigrationState
import enums.TriggeredMigration.TriggeredMigrationState.{TriggeredMigrationCeased, TriggeredMigrationAdded}
import models.core.IncomeSourceId

case class CheckHmrcRecordsViewModel(soleTraderBusinesses: List[CheckHmrcRecordsSoleTraderDetails],
                                     hasActiveUkProperty: Boolean,
                                     hasActiveForeignProperty: Boolean,
                                     triggeredMigrationState: Option[TriggeredMigrationState],
                                     numberOfCeasedBusinesses: Int = 0
                                    ) {
  def showCeasedBanner: Boolean = triggeredMigrationState match {
    case Some(TriggeredMigrationCeased) => true
    case _ => false
  }

  def showAddedBanner: Boolean = triggeredMigrationState match {
    case Some(TriggeredMigrationAdded(_)) => true
    case _ => false
  }
}

case class CheckHmrcRecordsSoleTraderDetails(incomeSourceId: IncomeSourceId,
                                             incomeSource: Option[String],
                                             businessName: Option[String])
