/*
 * Copyright 2026 HM Revenue & Customs
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

package businessDetails.models

import businessDetails.models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceReportingFrequencySourceData, ManageIncomeSourceData, SensitiveAddIncomeSourceData}
import businessDetails.models.triggeredMigration.TriggeredMigrationSessionData
import obligations.models.reportingObligations.optOut.OptOutSessionData
import obligations.models.reportingObligations.signUp.SignUpSessionData
import play.api.libs.json.*
import shared.models.UIJourneySessionData
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.language.implicitConversions

case class SensitiveUIJourneySessionData(
                                          sessionId: String,
                                          journeyType: String,
                                          addIncomeSourceData: Option[SensitiveAddIncomeSourceData] = None,
                                          manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                          ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                          optOutSessionData: Option[OptOutSessionData] = None,
                                          signUpSessionData: Option[SignUpSessionData] = None,
                                          incomeSourceReportingFrequencyData: Option[IncomeSourceReportingFrequencySourceData] = None,
                                          triggeredMigrationData: Option[TriggeredMigrationSessionData] = None,
                                          lastUpdated: Instant = Instant.now,
                                          journeyIsComplete: Option[Boolean] = None
                                        ) {

  def this(uiJourneySessionData: UIJourneySessionData) = {
    this(
      uiJourneySessionData.sessionId,
      uiJourneySessionData.journeyType,
      uiJourneySessionData.addIncomeSourceData.map(_.encrypted),
      uiJourneySessionData.manageIncomeSourceData,
      uiJourneySessionData.ceaseIncomeSourceData,
      uiJourneySessionData.optOutSessionData,
      uiJourneySessionData.signUpSessionData,
      uiJourneySessionData.incomeSourceReportingFrequencyData,
      uiJourneySessionData.triggeredMigrationData,
      uiJourneySessionData.lastUpdated,
      uiJourneySessionData.journeyIsComplete
    )
  }

  def decrypted: UIJourneySessionData =
    UIJourneySessionData(
      sessionId,
      journeyType,
      addIncomeSourceData.map(_.decrypted),
      manageIncomeSourceData,
      ceaseIncomeSourceData,
      optOutSessionData,
      signUpSessionData,
      incomeSourceReportingFrequencyData,
      triggeredMigrationData,
      lastUpdated,
      journeyIsComplete
    )
}

object SensitiveUIJourneySessionData {

  implicit def format(using crypto: Encrypter & Decrypter): OFormat[SensitiveUIJourneySessionData] =
    {
      implicit val mongoInstantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
      Json.format[SensitiveUIJourneySessionData]
    }
}
