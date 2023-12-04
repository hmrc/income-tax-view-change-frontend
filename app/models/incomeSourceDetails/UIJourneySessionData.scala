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

import directdebit.corjourney.crypto
import directdebit.corjourney.crypto.CryptoFormat
import models.SensitiveFormats
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.Sensitive.{SensitiveBoolean, SensitiveInstant, SensitiveString}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

case class UIJourneySessionData(
                                 sessionId: String,
                                 journeyType: String,
                                 addIncomeSourceData: Option[AddIncomeSourceData] = None,
                                 manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                 ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                 lastUpdated: Instant = Instant.now)

object UIJourneySessionData {

  val reads: Reads[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "journeyType").read[String] and
        (__ \ "addIncomeSourceData").readNullable[AddIncomeSourceData] and
        (__ \ "manageIncomeSourceData").readNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").readNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UIJourneySessionData.apply _)
  }

  val writes: OWrites[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").write[String] and
        (__ \ "journeyType").write[String] and
        (__ \ "addIncomeSourceData").writeNullable[AddIncomeSourceData] and
        (__ \ "manageIncomeSourceData").writeNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").writeNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(UIJourneySessionData.unapply))
  }

  implicit val format: OFormat[UIJourneySessionData] = OFormat(reads, writes)
}

case class AddIncomeSourceData(
                                businessName: Option[String] = None,
                                businessTrade: Option[String] = None,
                                dateStarted: Option[LocalDate] = None,
                                accountingPeriodStartDate: Option[LocalDate] = None,
                                accountingPeriodEndDate: Option[LocalDate] = None,
                                createdIncomeSourceId: Option[String] = None,
                                address: Option[Address] = None,
                                countryCode: Option[String] = None,
                                incomeSourcesAccountingMethod: Option[String] = None,
                                hasBeenAdded: Option[Boolean] = None
                              ) {

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): Format[AddIncomeSourceData] = {

    implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    implicit val sensitiveLocalDateFormat: Format[SensitiveInstant] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)
    implicit val sensitiveBooleanFormat: Format[SensitiveBoolean] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveBoolean.apply)

    def reads: Reads[EncryptedAddIncomeSourceData] =
      (
        (__ \ "businessName").readNullable[SensitiveString] and
          (__ \ "businessTrade").readNullable[SensitiveString] and
          (__ \ "dateStarted").readNullable[SensitiveInstant] and
          (__ \ "accountingPeriodStartDate").readNullable[SensitiveInstant] and
          (__ \ "accountingPeriodEndDate").readNullable[SensitiveInstant] and
          (__ \ "createdIncomeSourceId").readNullable[SensitiveString] and
          (__ \ "address").readNullable[Address] and
          (__ \ "countryCode").readNullable[SensitiveString] and
          (__ \ "incomeSourcesAccountingMethod").readNullable[SensitiveString] and
          (__ \ "hasBeenAdded").readNullable[SensitiveBoolean]
        )(EncryptedAddIncomeSourceData.apply _)

    def writes: Writes[EncryptedAddIncomeSourceData] =
      (
        (__ \ "businessName").writeNullable[SensitiveString] and
          (__ \ "businessTrade").writeNullable[SensitiveString] and
          (__ \ "dateStarted").writeNullable[SensitiveInstant] and
          (__ \ "accountingPeriodStartDate").writeNullable[SensitiveInstant] and
          (__ \ "accountingPeriodEndDate").writeNullable[SensitiveInstant] and
          (__ \ "createdIncomeSourceId").writeNullable[SensitiveString] and
          (__ \ "address").writeNullable[Address] and
          (__ \ "countryCode").writeNullable[SensitiveString] and
          (__ \ "incomeSourcesAccountingMethod").writeNullable[SensitiveString] and
          (__ \ "hasBeenAdded").writeNullable[SensitiveBoolean]
        )(unlift(EncryptedAddIncomeSourceData.unapply))

    Format(reads orElse reads, writes)
  }
}

object AddIncomeSourceData {
  val businessNameField = "businessName"
  val businessTradeField = "businessTrade"
  val dateStartedField = "dateStarted"
  val accountingPeriodStartDateField = "accountingPeriodStartDate"
  val accountingPeriodEndDateField = "accountingPeriodEndDate"
  val createdIncomeSourceIdField: String = "createdIncomeSourceId"
  val addressField: String = "address"
  val countryCodeField: String = "countryCode"
  val incomeSourcesAccountingMethodField: String = "incomeSourcesAccountingMethod"
  val hasBeenAddedField: String = "hasBeenAdded"

  def getJSONKeyPath(name: String): String = s"addIncomeSourceData.$name"

  def reads(implicit crypto: Encrypter with Decrypter): Reads[EncryptedAddIncomeSourceData] = {

    implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    implicit val sensitiveLocalDateFormat: Format[SensitiveInstant] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)
    implicit val sensitiveBooleanFormat: Format[SensitiveBoolean] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveBoolean.apply)

    (
      (__ \ "businessName").readNullable[SensitiveString] and
        (__ \ "businessTrade").readNullable[SensitiveString] and
        (__ \ "dateStarted").readNullable[SensitiveInstant] and
        (__ \ "accountingPeriodStartDate").readNullable[SensitiveInstant] and
        (__ \ "accountingPeriodEndDate").readNullable[SensitiveInstant] and
        (__ \ "createdIncomeSourceId").readNullable[SensitiveString] and
        (__ \ "address").readNullable[Address] and
        (__ \ "countryCode").readNullable[SensitiveString] and
        (__ \ "incomeSourcesAccountingMethod").readNullable[SensitiveString] and
        (__ \ "hasBeenAdded").readNullable[SensitiveBoolean]
      )(EncryptedAddIncomeSourceData.apply _)
  }

  def writes(implicit crypto: Encrypter with Decrypter): Writes[EncryptedAddIncomeSourceData] = {

    implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    implicit val sensitiveLocalDateFormat: Format[SensitiveInstant] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)
    implicit val sensitiveBooleanFormat: Format[SensitiveBoolean] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveBoolean.apply)

    (
      (__ \ "businessName").writeNullable[SensitiveString] and
        (__ \ "businessTrade").writeNullable[SensitiveString] and
        (__ \ "dateStarted").writeNullable[SensitiveInstant] and
        (__ \ "accountingPeriodStartDate").writeNullable[SensitiveInstant] and
        (__ \ "accountingPeriodEndDate").writeNullable[SensitiveInstant] and
        (__ \ "createdIncomeSourceId").writeNullable[SensitiveString] and
        (__ \ "address").writeNullable[Address] and
        (__ \ "countryCode").writeNullable[SensitiveString] and
        (__ \ "incomeSourcesAccountingMethod").writeNullable[SensitiveString] and
        (__ \ "hasBeenAdded").writeNullable[SensitiveBoolean]
      )(unlift(EncryptedAddIncomeSourceData.unapply))
  }
}

case class EncryptedAddIncomeSourceData(
                                         businessName: Option[SensitiveString] = None,
                                         businessTrade: Option[SensitiveString] = None,
                                         dateStarted: Option[SensitiveInstant] = None,
                                         accountingPeriodStartDate: Option[SensitiveInstant] = None,
                                         accountingPeriodEndDate: Option[SensitiveInstant] = None,
                                         createdIncomeSourceId: Option[SensitiveString] = None,
                                         address: Option[Address] = None,
                                         countryCode: Option[SensitiveString] = None,
                                         incomeSourcesAccountingMethod: Option[SensitiveString] = None,
                                         hasBeenAdded: Option[SensitiveBoolean] = None
                                       ) {

  def unencrypted: AddIncomeSourceData = AddIncomeSourceData(
    businessName.map(_.decryptedValue),
    businessTrade.map(_.decryptedValue),
    dateStarted.map(x => LocalDateTime.ofInstant(x.decryptedValue, ZoneOffset.UTC).toLocalDate),
    accountingPeriodStartDate.map(x => LocalDateTime.ofInstant(x.decryptedValue, ZoneOffset.UTC).toLocalDate),
    accountingPeriodEndDate.map(x => LocalDateTime.ofInstant(x.decryptedValue, ZoneOffset.UTC).toLocalDate),
    createdIncomeSourceId.map(_.decryptedValue),
    address,
    countryCode.map(_.decryptedValue),
    incomeSourcesAccountingMethod.map(_.decryptedValue),
    hasBeenAdded.map(_.decryptedValue)
  )
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