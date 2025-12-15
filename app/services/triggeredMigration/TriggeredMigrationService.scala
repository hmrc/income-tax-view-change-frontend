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

package services.triggeredMigration

import enums.TriggeredMigration.{TriggeredMigrationCeased, TriggeredMigrationState}
import models.core.IncomeSourceId
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}

import javax.inject.Singleton

@Singleton
class TriggeredMigrationService {

  def getCheckHmrcRecordsViewModel(incomeSources: IncomeSourceDetailsModel, state: Option[TriggeredMigrationState]): CheckHmrcRecordsViewModel = {
    val activeSoleTraderBusinesses = incomeSources.businesses.filterNot(_.isCeased)

    val hasActiveUkProperty = incomeSources.properties.filterNot(_.isCeased).exists(_.isUkProperty)
    val hasActiveForeignProperty = incomeSources.properties.filterNot(_.isCeased).exists(_.isForeignProperty)

    val numberOfCeasedBusinesses = incomeSources.businesses.count(_.isCeased)

    CheckHmrcRecordsViewModel(
      soleTraderBusinesses = activeSoleTraderBusinesses.map { business =>
        CheckHmrcRecordsSoleTraderDetails(
          incomeSourceId = IncomeSourceId(business.incomeSourceId),
          incomeSource = business.incomeSource,
          businessName = business.tradingName
        )
      },
      hasActiveUkProperty = hasActiveUkProperty,
      hasActiveForeignProperty = hasActiveForeignProperty,
      triggeredMigrationState = state,
      numberOfCeasedBusinesses = numberOfCeasedBusinesses
    )
  }
}
