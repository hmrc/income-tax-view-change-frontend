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

package models.incomeSourceDetails

import controllers.crypto
import controllers.crypto.CryptoFormat
import play.api.Configuration
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.security.SecureRandom
import java.time.{Instant, LocalDate}
import java.util.Base64

case class UIJourneySessionData(
                                 sessionId:               String,
                                 journeyType:             String,
                                 addIncomeSourceData:     Option[AddIncomeSourceData] = None,
                                 manageIncomeSourceData:  Option[ManageIncomeSourceData] = None,
                                 ceaseIncomeSourceData:   Option[CeaseIncomeSourceData] = None,
                                 lastUpdated:             Instant = Instant.now
                               ) {

  def encrypted: SensitiveUIJourneySessionData =
    SensitiveUIJourneySessionData(
      sessionId,
      journeyType,
      addIncomeSourceData.map(_.encrypted),
      manageIncomeSourceData,
      ceaseIncomeSourceData,
      lastUpdated
    )
}

object UIJourneySessionData {

  implicit val format: OFormat[UIJourneySessionData] = {

      ( (__ \ "sessionId"             ).format[String]
      ~ (__ \ "journeyType"           ).format[String]
      ~ (__ \ "addIncomeSourceData"   ).formatNullable[AddIncomeSourceData]
      ~ (__ \ "manageIncomeSourceData").formatNullable[ManageIncomeSourceData]
      ~ (__ \ "ceaseIncomeSourceData" ).formatNullable[CeaseIncomeSourceData]
      ~ (__ \ "lastUpdated"           ).format(MongoJavatimeFormats.instantFormat)
      )(
        UIJourneySessionData.apply, unlift(UIJourneySessionData.unapply)
      )
  }
}

case class SensitiveUIJourneySessionData(
                                          sessionId: String,
                                          journeyType: String,
                                          addIncomeSourceData: Option[SensitiveAddIncomeSourceData] = None,
                                          manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                          ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                          lastUpdated: Instant = Instant.now
                                        ) {
  def decrypted: UIJourneySessionData =
    UIJourneySessionData(
      sessionId,
      journeyType,
      addIncomeSourceData.map(_.decrypted),
      manageIncomeSourceData,
      ceaseIncomeSourceData,
      lastUpdated
    )
}

object SensitiveUIJourneySessionData {

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[SensitiveUIJourneySessionData] = {

      ( (__ \ "sessionId"             ).format[String]
      ~ (__ \ "journeyType"           ).format[String]
      ~ (__ \ "addIncomeSourceData"   ).formatNullable[SensitiveAddIncomeSourceData]
      ~ (__ \ "manageIncomeSourceData").formatNullable[ManageIncomeSourceData]
      ~ (__ \ "ceaseIncomeSourceData" ).formatNullable[CeaseIncomeSourceData]
      ~ (__ \ "lastUpdated"           ).format(MongoJavatimeFormats.instantFormat)
      )(
        SensitiveUIJourneySessionData.apply, unlift(SensitiveUIJourneySessionData.unapply)
      )
  }
}

case class ManageIncomeSourceData(
                                   incomeSourceId: Option[String] = None
                                 )

object ManageIncomeSourceData {

  val incomeSourceIdField = "incomeSourceId"

  def getJSONKeyPath(name: String): String = s"manageIncomeSourceData.$name"

  implicit val format: OFormat[ManageIncomeSourceData] = Json.format[ManageIncomeSourceData]
}

case class CeaseIncomeSourceData(
                                  incomeSourceId: Option[String],
                                  endDate: Option[String],
                                  ceasePropertyDeclare: Option[String]
                                )

object CeaseIncomeSourceData {

  val incomeSourceIdField: String = "incomeSourceId"
  val dateCeasedField: String = "endDate"
  val ceasePropertyDeclare: String = "ceasePropertyDeclare"

  def getJSONKeyPath(name: String): String = s"ceaseIncomeSourceData.$name"

  implicit val format: OFormat[CeaseIncomeSourceData] = Json.format[CeaseIncomeSourceData]
}