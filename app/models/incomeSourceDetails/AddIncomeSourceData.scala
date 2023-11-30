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

import play.api.Configuration
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Reads, Writes, __}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive, SymmetricCryptoFactory}
import uk.gov.hmrc.crypto.json.JsonEncryption


case class AddIncomeSourceData(
                                businessName: Option[String] = None,
                                businessTrade: Option[String] = None,
                                dateStarted: Option[String] = None,
                                accountingPeriodStartDate: Option[String] = None,
                                accountingPeriodEndDate: Option[String] = None,
                                createdIncomeSourceId: Option[String] = None,
                                address: Option[Address] = None,
                                countryCode: Option[String] = None,
                                incomeSourcesAccountingMethod: Option[String] = None)

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

  val reads: Reads[AddIncomeSourceData] =
    (
      (__ \ "businessName").readNullable[String] and
        (__ \ "businessTrade").readNullable[String] and
        (__ \ "dateStarted").readNullable[String] and
        (__ \ "accountingPeriodStartDate").readNullable[String] and
        (__ \ "accountingPeriodEndDate").readNullable[String] and
        (__ \ "createdIncomeSourceId").readNullable[String] and
        (__ \ "address").readNullable[Address] and
        (__ \ "countryCode").readNullable[String] and
        (__ \ "incomeSourcesAccountingMethod").readNullable[String]
      )(AddIncomeSourceData.apply _)

  val writes: Writes[AddIncomeSourceData] =
    (
      (__ \ "businessName").writeNullable[String] and
        (__ \ "businessTrade").writeNullable[String] and
        (__ \ "dateStarted").writeNullable[String] and
        (__ \ "accountingPeriodStartDate").writeNullable[String] and
        (__ \ "accountingPeriodEndDate").writeNullable[String] and
        (__ \ "createdIncomeSourceId").writeNullable[String] and
        (__ \ "address").writeNullable[Address] and
        (__ \ "countryCode").writeNullable[String] and
        (__ \ "incomeSourcesAccountingMethod").writeNullable[String]
      )(unlift(AddIncomeSourceData.unapply))

  implicit val format: Format[AddIncomeSourceData] = Format(reads, writes)


}

case class SensitiveAddIncomeSourceData[AddIncomeSourceData](override val decryptedValue: AddIncomeSourceData) extends Sensitive[AddIncomeSourceData]


object AddIncomeSourceDataEncDec {
  import AddIncomeSourceData.reads
  def encryptedFormat(implicit crypto: Encrypter with Decrypter): Format[AddIncomeSourceData] = {

    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    val encryptedReads: Reads[AddIncomeSourceData] =
      (
        (__ \ "businessName").readNullable[SensitiveString] and
          (__ \ "businessTrade").readNullable[SensitiveString] and
          (__ \ "dateStarted").readNullable[SensitiveString] and
          (__ \ "accountingPeriodStartDate").readNullable[SensitiveString] and
          (__ \ "accountingPeriodEndDate").readNullable[SensitiveString] and
          (__ \ "createdIncomeSourceId").readNullable[SensitiveString] and
          (__ \ "address").readNullable[Address] and
          (__ \ "countryCode").readNullable[SensitiveString] and
          (__ \ "incomeSourcesAccountingMethod").readNullable[SensitiveString]
        ) {
        (businessName,
         businessTrade,
         dateStarted,
         accountingPeriodStartDate,
         accountingPeriodEndDate,
         createdIncomeSourceId,
         address,
         countryCode,
         incomeSourcesAccountingMethod
        ) =>
          AddIncomeSourceData(
            businessName.map(_.decryptedValue),
            businessTrade.map(_.decryptedValue),
            dateStarted.map(_.decryptedValue),
            accountingPeriodStartDate.map(_.decryptedValue),
            accountingPeriodEndDate.map(_.decryptedValue),
            createdIncomeSourceId.map(_.decryptedValue),
            address,
            countryCode.map(_.decryptedValue),
            incomeSourcesAccountingMethod.map(_.decryptedValue)
          )
      }

    val encryptedWrites: Writes[AddIncomeSourceData] =
      (
        (__ \ "businessName").writeNullable[SensitiveString] and
          (__ \ "businessTrade").writeNullable[SensitiveString] and
          (__ \ "dateStarted").writeNullable[SensitiveString] and
          (__ \ "accountingPeriodStartDate").writeNullable[SensitiveString] and
          (__ \ "accountingPeriodEndDate").writeNullable[SensitiveString] and
          (__ \ "createdIncomeSourceId").writeNullable[SensitiveString] and
          (__ \ "address").writeNullable[Address] and
          (__ \ "countryCode").writeNullable[SensitiveString] and
          (__ \ "incomeSourcesAccountingMethod").writeNullable[SensitiveString]
        ) { s =>
        (
          s.businessName.map(SensitiveString),
          s.businessTrade.map(SensitiveString),
          s.dateStarted.map(SensitiveString),
          s.accountingPeriodStartDate.map(SensitiveString),
          s.accountingPeriodEndDate.map(SensitiveString),
          s.createdIncomeSourceId.map(SensitiveString),
          s.address,
          s.countryCode.map(SensitiveString),
          s.incomeSourcesAccountingMethod.map(SensitiveString)
        )
      }

    Format(encryptedReads orElse reads, encryptedWrites)
  //
  }


}
