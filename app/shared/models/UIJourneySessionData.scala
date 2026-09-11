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

package shared.models

import businessDetails.models.incomeSourceDetails.*
import businessDetails.models.triggeredMigration.TriggeredMigrationSessionData
import obligations.models.reportingObligations.optOut.OptOutSessionData
import obligations.models.reportingObligations.signUp.SignUpSessionData
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.language.implicitConversions

case class UIJourneySessionData(
                                 sessionId: String,
                                 journeyType: String,
                                 addIncomeSourceData: Option[AddIncomeSourceData] = None,
                                 manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                 ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                 optOutSessionData: Option[OptOutSessionData] = None,
                                 signUpSessionData: Option[SignUpSessionData] = None,
                                 incomeSourceReportingFrequencyData: Option[IncomeSourceReportingFrequencySourceData] = None,
                                 triggeredMigrationData: Option[TriggeredMigrationSessionData] = None,
                                 lastUpdated: Instant = Instant.now,
                                 journeyIsComplete: Option[Boolean] = None
                               )

object UIJourneySessionData {

  implicit val format: OFormat[UIJourneySessionData] = {
    implicit val mongoInstantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[UIJourneySessionData]
  }
}
