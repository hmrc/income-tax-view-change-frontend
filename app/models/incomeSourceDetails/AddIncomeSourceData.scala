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
import monocle.Lens
import monocle.macros.GenLens

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

  val addIncomeSourceDataLens:       Lens[UIJourneySessionData, Option[AddIncomeSourceData]] = GenLens[UIJourneySessionData](_.addIncomeSourceData)
  val businessNameLens:              Lens[AddIncomeSourceData, Option[String]]               = GenLens[AddIncomeSourceData](_.businessName)
  val businessTradeLens:             Lens[AddIncomeSourceData, Option[String]]               = GenLens[AddIncomeSourceData](_.businessTrade)
  val dateStartedLens:               Lens[AddIncomeSourceData, Option[LocalDate]]            = GenLens[AddIncomeSourceData](_.dateStarted)
  val accountingPeriodStartDateLens: Lens[AddIncomeSourceData, Option[LocalDate]]            = GenLens[AddIncomeSourceData](_.accountingPeriodStartDate)
  val accountingPeriodEndDateLens:   Lens[AddIncomeSourceData, Option[LocalDate]]            = GenLens[AddIncomeSourceData](_.accountingPeriodEndDate)
  val journeyIsCompleteLens:         Lens[AddIncomeSourceData, Option[Boolean]]              = GenLens[AddIncomeSourceData](_.journeyIsComplete)
  val incomeSourceIdLens:            Lens[AddIncomeSourceData, Option[String]]               = GenLens[AddIncomeSourceData](_.incomeSourceId)
  val incomeSourceAddedLens:         Lens[AddIncomeSourceData, Option[Boolean]]              = GenLens[AddIncomeSourceData](_.incomeSourceAdded)
  val accountingMethodLens:          Lens[AddIncomeSourceData, Option[String]]               = GenLens[AddIncomeSourceData](_.incomeSourcesAccountingMethod)

  val businessNameCombinedLens: Lens[UIJourneySessionData, Option[String]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.businessName))(optStr => {
      case Some(data) => Some(data.copy(businessName = optStr))
      case None => None
    }))

  val businessTradeCombinedLens: Lens[UIJourneySessionData, Option[String]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.businessTrade))(optStr => {
      case Some(data) => Some(data.copy(businessTrade = optStr))
      case None => None
    }))

  val dateStartedCombinedLens: Lens[UIJourneySessionData, Option[LocalDate]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[LocalDate]](_.flatMap(_.dateStarted))(optStr => {
      case Some(data) => Some(data.copy(dateStarted = optStr))
      case None => None
    }))

  val journeyIsCompleteCombinedLens: Lens[UIJourneySessionData, Option[Boolean]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[Boolean]](_.flatMap(_.journeyIsComplete))(optStr => {
      case Some(data) => Some(data.copy(journeyIsComplete = optStr))
      case None => None
    }))

  val incomeSourceIdCombinedLens: Lens[UIJourneySessionData, Option[String]] =
    addIncomeSourceDataLens.andThen(Lens[Option[AddIncomeSourceData], Option[String]](_.flatMap(_.incomeSourceId))(optStr => {
      case Some(data) => Some(data.copy(incomeSourceId = optStr))
      case None => None
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
