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

package models.incomeSourceDetails

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class Country(
                    code:    Option[String],
                    name:    Option[String]
                  ) {

  def encrypted: SensitiveCountry =
    SensitiveCountry(
      code     .map(SensitiveString.apply),
      name     .map(SensitiveString.apply)
    )

  override def toString: String =
    s"${code.map(stringValue => s"$stringValue").getOrElse("")}${name.map(stringValue => s", $stringValue").getOrElse("")}"
}

object Country {
  implicit val format: OFormat[Country] = Json.format[Country]
}

case class SensitiveCountry(
                             code: Option[SensitiveString],
                             name: Option[SensitiveString]
                           ) {

  def decrypted: Country =
    Country(
      code.map(_.decryptedValue),
      name.map(_.decryptedValue)
    )
}

object SensitiveCountry {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveCountry] =
    Json.format[SensitiveCountry]
}
