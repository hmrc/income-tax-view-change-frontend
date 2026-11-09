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

import enums.TriggeredMigration.Channel.{CustomerLed, HmrcConfirmed}
import play.api.Logging
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

sealed trait IncomeSourceDetailsResponse {
  def toJson: JsValue
}


case class ChooseSoleTraderAddressUserAnswer(
                                              addressLine1: Option[String],
                                              addressLine2: Option[String],
                                              addressLine3: Option[String],
                                              addressLine4: Option[String],
                                              postcode: Option[String],
                                              countryCode: Option[String],
                                              newAddress: Boolean
                                            )

object ChooseSoleTraderAddressUserAnswer {
  implicit val format: OFormat[ChooseSoleTraderAddressUserAnswer] = Json.format[ChooseSoleTraderAddressUserAnswer]
}

case class SensitiveChooseSoleTraderAddressRadioAnswer(
                                                        addressLine1: Option[SensitiveString],
                                                        addressLine2: Option[SensitiveString],
                                                        addressLine3: Option[SensitiveString],
                                                        addressLine4: Option[SensitiveString],
                                                        postcode: Option[SensitiveString],
                                                        countryCode: Option[SensitiveString],
                                                        newAddress: Boolean
                                                      ) {

  def decrypted: ChooseSoleTraderAddressUserAnswer =
    ChooseSoleTraderAddressUserAnswer(
      addressLine1.map(_.decryptedValue),
      addressLine2.map(_.decryptedValue),
      addressLine3.map(_.decryptedValue),
      addressLine4.map(_.decryptedValue),
      postcode.map(_.decryptedValue),
      countryCode.map(_.decryptedValue),
      newAddress
    )
}

object SensitiveChooseSoleTraderAddressRadioAnswer {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveChooseSoleTraderAddressRadioAnswer] =
    Json.format[SensitiveChooseSoleTraderAddressRadioAnswer]
}

case class IncomeSourceDetailsModel(
                                     nino: String,
                                     mtdbsa: String,
                                     yearOfMigration: Option[String],
                                     businesses: List[BusinessDetailsModel],
                                     properties: List[PropertyDetailsModel],
                                     channel: String = "Customer-led"
                                   ) extends IncomeSourceDetailsResponse with Logging {

  val hasPropertyIncome: Boolean = properties.nonEmpty
  val hasBusinessIncome: Boolean = businesses.nonEmpty
  val hasAnyIncomeSources: Boolean = hasBusinessIncome || hasPropertyIncome
  val hasOngoingBusinessOrPropertyIncome: Boolean = businesses.exists(businessDetailsModel => businessDetailsModel.cessation.forall(_.date.isEmpty)) ||
    properties.exists(propertyDetailsModel => propertyDetailsModel.cessation.forall(_.date.isEmpty))

  override def toJson: JsValue = Json.toJson(this)

  def isConfirmedUser: Boolean = {
    Set(CustomerLed.getValue, HmrcConfirmed.getValue).contains(channel)
  }
}

case class IncomeSourceDetailsError(status: Int, reason: String) extends IncomeSourceDetailsResponse {
  override def toJson: JsValue = Json.toJson(this)
}

object IncomeSourceDetailsModel {
  implicit val format: Format[IncomeSourceDetailsModel] = Json.format[IncomeSourceDetailsModel]
}

object IncomeSourceDetailsError {
  implicit val format: Format[IncomeSourceDetailsError] = Json.format[IncomeSourceDetailsError]
}
