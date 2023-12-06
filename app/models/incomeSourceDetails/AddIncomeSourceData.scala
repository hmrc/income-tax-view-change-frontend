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

import uk.gov.hmrc.crypto.Sensitive.{SensitiveInstant, SensitiveString}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.json.JsonEncryption
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.{LocalDate, ZoneOffset}

case class AddIncomeSourceData(
                                businessName:                   Option[String]     = None,
                                businessTrade:                  Option[String]     = None,
                                dateStarted:                    Option[LocalDate]  = None,
                                accountingPeriodStartDate:      Option[LocalDate]  = None,
                                accountingPeriodEndDate:        Option[LocalDate]  = None,
                                createdIncomeSourceId:          Option[String]     = None,
                                address:                        Option[Address]    = None,
                                countryCode:                    Option[String]     = None,
                                incomeSourcesAccountingMethod:  Option[String]     = None
                              ) {

  def encrypted: SensitiveAddIncomeSourceData =
    SensitiveAddIncomeSourceData(
      businessName                  .map(SensitiveString),
      businessTrade                 .map(SensitiveString),
      dateStarted                   .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodStartDate     .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodEndDate       .map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      createdIncomeSourceId         .map(SensitiveString),
      address,
      countryCode                   .map(SensitiveString),
      incomeSourcesAccountingMethod .map(SensitiveString)
    )
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

  def getJSONKeyPath(name: String): String = s"addIncomeSourceData.$name"

  implicit val format: Format[AddIncomeSourceData] =
    ( (__ \ "businessName"                 ).formatNullable[String]
    ~ (__ \ "businessTrade"                ).formatNullable[String]
    ~ (__ \ "dateStarted"                  ).formatNullable[LocalDate]
    ~ (__ \ "accountingPeriodStartDate"    ).formatNullable[LocalDate]
    ~ (__ \ "accountingPeriodEndDate"      ).formatNullable[LocalDate]
    ~ (__ \ "createdIncomeSourceId"        ).formatNullable[String]
    ~ (__ \ "address"                      ).formatNullable[Address]
    ~ (__ \ "countryCode"                  ).formatNullable[String]
    ~ (__ \ "incomeSourcesAccountingMethod").formatNullable[String]
    )(
      AddIncomeSourceData.apply, unlift(AddIncomeSourceData.unapply)
    )
}

case class SensitiveAddIncomeSourceData(
                                         businessName:                  Option[SensitiveString] = None,
                                         businessTrade:                 Option[SensitiveString] = None,
                                         dateStarted:                   Option[SensitiveInstant] = None,
                                         accountingPeriodStartDate:     Option[SensitiveInstant] = None,
                                         accountingPeriodEndDate:       Option[SensitiveInstant] = None,
                                         createdIncomeSourceId:         Option[SensitiveString] = None,
                                         address:                       Option[Address]         = None,
                                         countryCode:                   Option[SensitiveString] = None,
                                         incomeSourcesAccountingMethod: Option[SensitiveString] = None
                                       ) {

  def decrypted: AddIncomeSourceData =
    AddIncomeSourceData(
      businessName                  .map(_.decryptedValue),
      businessTrade                 .map(_.decryptedValue),
      dateStarted                   .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodStartDate     .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodEndDate       .map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      createdIncomeSourceId         .map(_.decryptedValue),
      address,
      countryCode                   .map(_.decryptedValue),
      incomeSourcesAccountingMethod .map(_.decryptedValue)
  )
}

object SensitiveAddIncomeSourceData {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def sensitiveInstantFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveInstant] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveAddIncomeSourceData] = {

      ( (__ \ "businessName"                 ).formatNullable[SensitiveString]
      ~ (__ \ "businessTrade"                ).formatNullable[SensitiveString]
      ~ (__ \ "dateStarted"                  ).formatNullable[SensitiveInstant]
      ~ (__ \ "accountingPeriodStartDate"    ).formatNullable[SensitiveInstant]
      ~ (__ \ "accountingPeriodEndDate"      ).formatNullable[SensitiveInstant]
      ~ (__ \ "createdIncomeSourceId"        ).formatNullable[SensitiveString]
      ~ (__ \ "address"                      ).formatNullable[Address]
      ~ (__ \ "countryCode"                  ).formatNullable[SensitiveString]
      ~ (__ \ "incomeSourcesAccountingMethod").formatNullable[SensitiveString]
      )(
        SensitiveAddIncomeSourceData.apply, unlift(SensitiveAddIncomeSourceData.unapply)
      )
  }
}
