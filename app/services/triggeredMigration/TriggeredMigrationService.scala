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

import com.google.inject.Inject
import enums.JourneyType.TriggeredMigrationJourney
import enums.TriggeredMigration.TriggeredMigrationState
import models.UIJourneySessionData
import models.core.IncomeSourceId
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.triggeredMigration.TriggeredMigrationSessionData
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggeredMigrationService @Inject()(sessionService: SessionService) {

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

  def saveConfirmedData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val sessionId = hc.sessionId.map(_.value) getOrElse {
      throw new Exception("Missing sessionId in HeaderCarrier")
    }

    sessionService.setMongoData(UIJourneySessionData(sessionId, TriggeredMigrationJourney.toString,
      triggeredMigrationData = Some(TriggeredMigrationSessionData(
        recentlyConfirmed = true
      )))) flatMap {
      case true => Future.successful(true)
      case false => Future.failed(new Exception("[TriggeredMigrationService][saveConfirmedData] Mongo update call was not acknowledged"))
    }
  }
}
