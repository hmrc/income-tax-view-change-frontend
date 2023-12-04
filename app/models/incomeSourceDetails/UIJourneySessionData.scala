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
                                 sessionId: String,
                                 journeyType: String,
                                 addIncomeSourceData: Option[SensitiveAddIncomeSourceData] = None,
                                 manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                 ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                 lastUpdated: Instant = Instant.now)

object UIJourneySessionData {

  def reads(implicit crypto: Encrypter with Decrypter): Reads[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "journeyType").read[String] and
        (__ \ "addIncomeSourceData").readNullable[SensitiveAddIncomeSourceData](SensitiveAddIncomeSourceData.format(crypto)) and
        (__ \ "manageIncomeSourceData").readNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").readNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UIJourneySessionData.apply _)
  }

  def writes(implicit crypto: Encrypter with Decrypter): OWrites[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").write[String] and
        (__ \ "journeyType").write[String] and
        (__ \ "addIncomeSourceData").writeNullable[SensitiveAddIncomeSourceData](SensitiveAddIncomeSourceData.format(crypto)) and
        (__ \ "manageIncomeSourceData").writeNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").writeNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(UIJourneySessionData.unapply))
  }

  def format(implicit crypto: Encrypter with Decrypter): OFormat[UIJourneySessionData] = OFormat(reads, writes)
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