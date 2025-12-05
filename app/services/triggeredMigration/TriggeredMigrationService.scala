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

import enums.TriggeredMigration.TriggeredMigrationCeased
import models.core.IncomeSourceId
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}

import javax.inject.Singleton

@Singleton
class TriggeredMigrationService {

  private[triggeredMigration] def ceasedBusinessSetup(state: Option[String], incomeSources: IncomeSourceDetailsModel): (Boolean, Int) = {
    (state, incomeSources.businesses.count(_.isCeased)) match {
      case (Some(TriggeredMigrationCeased.toString), noOfCeased) => (true, noOfCeased)
      case (_, noOfCeased) if noOfCeased > 0 => (false, noOfCeased)
      case _ => (false, 0)
    }
  }

  def getCheckHmrcRecordsViewModel(incomeSources: IncomeSourceDetailsModel, state: Option[String]): CheckHmrcRecordsViewModel = {
    val activeSoleTraderBusinesses = incomeSources.businesses.filterNot(_.isCeased)

    val hasActiveUkProperty = incomeSources.properties.filterNot(_.isCeased).exists(_.isUkProperty)
    val hasActiveForeignProperty = incomeSources.properties.filterNot(_.isCeased).exists(_.isForeignProperty)

    val ceasedSetup = ceasedBusinessSetup(state, incomeSources)

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
      showCeasedBanner = ceasedSetup._1,
      numberOfCeasedBusinesses = ceasedSetup._2
    )
  }
}
