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

import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.{SensitiveBoolean, SensitiveInstant, SensitiveString}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import models.incomeSourceDetails._

import java.time.{LocalDate, ZoneOffset}

//TODO: Few of these fields needs to be cleaned up when we remove old Income source journey.
case class AddIncomeSourceData(
                                businessName: Option[String] = None,
                                businessTrade: Option[String] = None,
                                dateStarted: Option[LocalDate] = None,
                                accountingPeriodStartDate: Option[LocalDate] = None,
                                accountingPeriodEndDate: Option[LocalDate] = None,
                                incomeSourceId: Option[String] = None,
                                chooseSoleTraderAddress: Option[ChooseSoleTraderAddressRadioAnswer] = None,
                                address: Option[Address] = None,
                                countryCode: Option[String] = None,
                                addressId: Option[String] = None,
                                addressLookupId: Option[String] = None,
                                changeReportingFrequency: Option[Boolean] = None,
                                reportingMethodTaxYear1: Option[String] = None,
                                reportingMethodTaxYear2: Option[String] = None,
                                incomeSourceAdded: Option[Boolean] = None,
                                incomeSourceCreatedJourneyComplete: Option[Boolean] = None,
                                incomeSourceRFJourneyComplete: Option[Boolean] = None,
                              ) {

  def encrypted: SensitiveAddIncomeSourceData =
    SensitiveAddIncomeSourceData(
      businessName.map(SensitiveString),
      businessTrade.map(SensitiveString),
      dateStarted.map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodStartDate.map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      accountingPeriodEndDate.map(_.atStartOfDay().toInstant(ZoneOffset.UTC)).map(SensitiveInstant),
      incomeSourceId.map(SensitiveString),
      address.map { case Address(lines, postcode) => SensitiveAddress(lines.map(SensitiveString), postcode.map(SensitiveString)) },
      chooseSoleTraderAddress.map { case ChooseSoleTraderAddressRadioAnswer(lines, postcode, countryCode, newAddress) => SensitiveChooseSoleTraderAddressRadioAnswer(lines.map(SensitiveString), postcode.map(SensitiveString), countryCode.map(SensitiveString), newAddress) },
      countryCode.map(SensitiveString),
      addressId.map(SensitiveString),
      addressLookupId.map(SensitiveString),
      changeReportingFrequency.map(SensitiveBoolean),
      reportingMethodTaxYear1.map(SensitiveString),
      reportingMethodTaxYear2.map(SensitiveString),
      incomeSourceAdded.map(SensitiveBoolean),
      incomeSourceCreatedJourneyComplete.map(SensitiveBoolean),
      incomeSourceRFJourneyComplete.map(SensitiveBoolean)
    )
}


object AddIncomeSourceData {
  val businessNameField: String = "businessName"
  val businessTradeField: String = "businessTrade"
  val dateStartedField: String = "dateStarted"
  val accountingPeriodStartDateField: String = "accountingPeriodStartDate"
  val accountingPeriodEndDateField: String = "accountingPeriodEndDate"
  val incomeSourceIdField: String = "incomeSourceId"
  val changeReportingFrequency: String = "changeReportingFrequency"
  val incomeSourceCreatedJourneyCompleteField: String = "incomeSourceCreatedJourneyComplete"
  val incomeSourceAddedField: String = "incomeSourceAdded"
  val addressIdField: String = "addressId"
  val addressLookupIdField: String = "addressLookupId"

  def getJSONKeyPath(name: String): String = s"addIncomeSourceData.$name"

  implicit val format: OFormat[AddIncomeSourceData] = Json.format[AddIncomeSourceData]
}

case class SensitiveAddIncomeSourceData(
                                         businessName: Option[SensitiveString] = None,
                                         businessTrade: Option[SensitiveString] = None,
                                         dateStarted: Option[SensitiveInstant] = None,
                                         accountingPeriodStartDate: Option[SensitiveInstant] = None,
                                         accountingPeriodEndDate: Option[SensitiveInstant] = None,
                                         incomeSourceId: Option[SensitiveString] = None,
                                         address: Option[SensitiveAddress] = None,
                                         chooseSoleTraderAddress: Option[SensitiveChooseSoleTraderAddressRadioAnswer] = None,
                                         countryCode: Option[SensitiveString] = None,
                                         addressId: Option[SensitiveString] = None,
                                         addressLookupId: Option[SensitiveString] = None,
                                         changeReportingFrequency: Option[SensitiveBoolean] = None,
                                         reportingMethodTaxYear1: Option[SensitiveString] = None,
                                         reportingMethodTaxYear2: Option[SensitiveString] = None,
                                         incomeSourceAdded: Option[SensitiveBoolean] = None,
                                         incomeSourceCreatedJourneyComplete: Option[SensitiveBoolean] = None,
                                         incomeSourceRFJourneyComplete: Option[SensitiveBoolean] = None
                                       ) {

  def decrypted: AddIncomeSourceData =
    AddIncomeSourceData(
      businessName = businessName.map(_.decryptedValue),
      businessTrade = businessTrade.map(_.decryptedValue),
      dateStarted = dateStarted.map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodStartDate = accountingPeriodStartDate.map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      accountingPeriodEndDate = accountingPeriodEndDate.map(_.decryptedValue.atZone(ZoneOffset.UTC).toLocalDate()),
      incomeSourceId = incomeSourceId.map(_.decryptedValue),
      address = address.map(_.decrypted),
      chooseSoleTraderAddress = chooseSoleTraderAddress.map(_.decrypted),
      countryCode = countryCode.map(_.decryptedValue),
      addressId = addressId.map(_.decryptedValue),
      addressLookupId = addressLookupId.map(_.decryptedValue),
      changeReportingFrequency = changeReportingFrequency.map(_.decryptedValue),
      reportingMethodTaxYear1 = reportingMethodTaxYear1.map(_.decryptedValue),
      reportingMethodTaxYear2 = reportingMethodTaxYear2.map(_.decryptedValue),
      incomeSourceAdded = incomeSourceAdded.map(_.decryptedValue),
      incomeSourceCreatedJourneyComplete = incomeSourceCreatedJourneyComplete.map(_.decryptedValue),
      incomeSourceRFJourneyComplete = incomeSourceRFJourneyComplete.map(_.decryptedValue)
    )
}

object SensitiveAddIncomeSourceData {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def sensitiveInstantFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveInstant] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveInstant.apply)

  implicit def sensitiveBooleanFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveBoolean] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveBoolean.apply)

  import play.api.libs.json.{Format, Json}

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveAddIncomeSourceData] =
    Json.format[SensitiveAddIncomeSourceData]

}