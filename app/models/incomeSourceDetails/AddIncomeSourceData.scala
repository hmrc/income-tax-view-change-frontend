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

import forms.models.DateFormElement
import uk.gov.hmrc.crypto.Sensitive.{SensitiveBoolean, SensitiveInstant, SensitiveString}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.json.JsonEncryption
import play.api.libs.functional.syntax._
import play.api.libs.json._
import monocle.{Focus, Lens, Optional, Traversal}
import monocle.macros.GenLens
import monocle.Optional
import monocle.std.option.some

import java.time.{LocalDate, ZoneOffset}

case class AddIncomeSourceData(
                                businessName:                  Option[String]    = None,
                                businessTrade:                 Option[String]    = None,
                                dateStarted:                   Option[LocalDate] = None,
                                accountingPeriodStartDate:     Option[LocalDate] = None,
                                accountingPeriodEndDate:       Option[LocalDate] = None,
                                incomeSourceId:                Option[String]    = None,
                                address:                       Option[Address]   = None,
                                countryCode:                   Option[String]    = None,
                                incomeSourcesAccountingMethod: Option[String]    = None,
                                incomeSourceAdded:             Option[Boolean]   = None,
                                journeyIsComplete:             Option[Boolean]   = None
                              ) {

  def encrypted: SensitiveAddIncomeSourceData =
    SensitiveAddIncomeSourceData(
      businessName                  .map(SensitiveString),
      businessTrade                 .map(SensitiveString),
      dateStarted                   .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodStartDate     .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodEndDate       .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      incomeSourceId                .map(SensitiveString),
      address                       .map { case Address(lines, postcode) => SensitiveAddress(lines.map(SensitiveString), postcode.map(SensitiveString)) },
      countryCode                   .map(SensitiveString),
      incomeSourcesAccountingMethod .map(SensitiveString),
      incomeSourceAdded             .map(SensitiveBoolean),
      journeyIsComplete             .map(SensitiveBoolean)
    )

  def sanitiseDates: AddIncomeSourceData =
    this.copy(
      accountingPeriodStartDate = None,
      accountingPeriodEndDate = None,
      dateStarted = None
    )
}


object AddIncomeSourceData {
  val businessNameField:                  String = "businessName"
  val businessTradeField:                 String = "businessTrade"
  val dateStartedField:                   String = "dateStarted"
  val accountingPeriodStartDateField:     String = "accountingPeriodStartDate"
  val accountingPeriodEndDateField:       String = "accountingPeriodEndDate"
  val incomeSourceIdField:                String = "incomeSourceId"
  val addressField:                       String = "address"
  val countryCodeField:                   String = "countryCode"
  val incomeSourcesAccountingMethodField: String = "incomeSourcesAccountingMethod"
  val journeyIsCompleteField:             String = "journeyIsComplete"
  val incomeSourceAddedField:             String = "incomeSourceAdded"

  def getJSONKeyPath(name: String): String = s"addIncomeSourceData.$name"

  implicit val format: OFormat[AddIncomeSourceData] = Json.format[AddIncomeSourceData]

  val addIncomeSourceDataLens: Lens[UIJourneySessionData, Option[AddIncomeSourceData]] = GenLens[UIJourneySessionData](_.addIncomeSourceData)

  // Below is a broken-down version of a Lens as an attempt to explain how it works
  // Lens to access the 'businessName' within 'UIJourneySessionData'
  // Lens to extract and modify 'businessName' from 'AddIncomeSourceData'
  // Getter: Extracts 'businessName' if present
  // Setter: Updates 'businessName' in existing data
  // Setter: Creates new data if none exists

  val businessNameLens: Lens[UIJourneySessionData, Option[String]] = {

    val businessNameFromDataLens = Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.businessName))(optStr => {
      case Some(data) => Some(data.copy(businessName = optStr))
      case None => Some(AddIncomeSourceData(businessName = optStr))
    })
    addIncomeSourceDataLens.andThen(businessNameFromDataLens)
  }

  val businessTradeLens: Lens[UIJourneySessionData, Option[String]] = {

    val businessTradeLens = Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.businessTrade))(optStr => {
      case Some(data) => Some(data.copy(businessTrade = optStr))
      case None       => None
    })

    addIncomeSourceDataLens andThen businessTradeLens
  }

  val dateStartedLens: Lens[UIJourneySessionData, Option[LocalDate]] = {

    val dateStartedLens = Lens[Option[AddIncomeSourceData], Option[LocalDate]](_.flatMap(_.dateStarted))(optDate => {
      case Some(data) => Some(data.copy(dateStarted = optDate))
      case None => Some(AddIncomeSourceData(dateStarted = optDate))
    })

    addIncomeSourceDataLens andThen dateStartedLens
  }

  val journeyIsCompleteLens: Lens[UIJourneySessionData, Option[Boolean]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[Boolean]](_.flatMap(_.journeyIsComplete))(optBool => {
      case Some(data) => Some(data.copy(journeyIsComplete = optBool))
      case None       => Some(AddIncomeSourceData())
    }))

  val incomeSourceIdLens: Lens[UIJourneySessionData, Option[String]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.incomeSourceId))(optStr => {
      case Some(data) => Some(data.copy(incomeSourceId = optStr))
      case None       => None
    }))

  val incomeSourceAddedLens: Lens[UIJourneySessionData, Option[Boolean]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[Boolean]](_.flatMap(_.incomeSourceAdded))(optBool => {
      case Some(data) => Some(data.copy(incomeSourceAdded = optBool))
      case None       => Some(AddIncomeSourceData())
    }))

  val accountingMethodLens: Lens[UIJourneySessionData, Option[String]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.incomeSourcesAccountingMethod))(optStr => {
      case Some(data) => Some(data.copy(incomeSourcesAccountingMethod = optStr))
      case None       => None
    }))

  val businessAddressLens: Lens[UIJourneySessionData, (Option[Address], Option[String])] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], (Option[Address], Option[String])](
      _.map(x => (x.address, x.countryCode)).getOrElse((None, None))
    )(optAddress => {
      case Some(data) => Some(data.copy(address = optAddress._1, countryCode = optAddress._2))
      case None       => Some(AddIncomeSourceData(address = optAddress._1, countryCode = optAddress._2))
    }))

  val accountingPeriodLens: Lens[UIJourneySessionData, (Option[LocalDate], Option[LocalDate])] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], (Option[LocalDate], Option[LocalDate])](
      _.map(x => (x.accountingPeriodStartDate, x.accountingPeriodEndDate)).getOrElse((None, None))
    )(optDate => {
      case Some(data) => Some(data.copy(accountingPeriodStartDate = optDate._1, accountingPeriodEndDate = optDate._2))
      case None       => None
    }))
}

case class SensitiveAddIncomeSourceData(
                                         businessName:                  Option[SensitiveString]  = None,
                                         businessTrade:                 Option[SensitiveString]  = None,
                                         dateStarted:                   Option[SensitiveInstant] = None,
                                         accountingPeriodStartDate:     Option[SensitiveInstant] = None,
                                         accountingPeriodEndDate:       Option[SensitiveInstant] = None,
                                         incomeSourceId:                Option[SensitiveString]  = None,
                                         address:                       Option[SensitiveAddress] = None,
                                         countryCode:                   Option[SensitiveString]  = None,
                                         incomeSourcesAccountingMethod: Option[SensitiveString]  = None,
                                         incomeSourceAdded:             Option[SensitiveBoolean] = None,
                                         journeyIsComplete:             Option[SensitiveBoolean] = None
                                       ) {

  def decrypted: AddIncomeSourceData =
    AddIncomeSourceData(
      businessName                  .map(_.decryptedValue),
      businessTrade                 .map(_.decryptedValue),
      dateStarted                   .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodStartDate     .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodEndDate       .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      incomeSourceId                .map(_.decryptedValue),
      address                       .map(_.decrypted),
      countryCode                   .map(_.decryptedValue),
      incomeSourcesAccountingMethod .map(_.decryptedValue),
      incomeSourceAdded             .map(_.decryptedValue),
      journeyIsComplete             .map(_.decryptedValue)
    )
}

object SensitiveAddIncomeSourceData {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def sensitiveInstantFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveInstant] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)

  implicit def sensitiveBooleanFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveBoolean] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveBoolean.apply)

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveAddIncomeSourceData] = {

       ((__ \ "businessName"                 ).formatNullable[SensitiveString]
      ~ (__ \ "businessTrade"                ).formatNullable[SensitiveString]
      ~ (__ \ "dateStarted"                  ).formatNullable[SensitiveInstant]
      ~ (__ \ "accountingPeriodStartDate"    ).formatNullable[SensitiveInstant]
      ~ (__ \ "accountingPeriodEndDate"      ).formatNullable[SensitiveInstant]
      ~ (__ \ "incomeSourceId"               ).formatNullable[SensitiveString]
      ~ (__ \ "address"                      ).formatNullable[SensitiveAddress]
      ~ (__ \ "countryCode"                  ).formatNullable[SensitiveString]
      ~ (__ \ "incomeSourcesAccountingMethod").formatNullable[SensitiveString]
      ~ (__ \ "incomeSourceAdded"            ).formatNullable[SensitiveBoolean]
      ~ (__ \ "journeyIsComplete"            ).formatNullable[SensitiveBoolean]
      )(SensitiveAddIncomeSourceData.apply, unlift(SensitiveAddIncomeSourceData.unapply)
    )
  }
}
