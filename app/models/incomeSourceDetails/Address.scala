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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class Address(
                    lines:    Seq[String],
                    postcode: Option[String]
                  ) {

  def encrypted: SensitiveAddress =
    SensitiveAddress(
      lines     .map(SensitiveString),
      postcode  .map(SensitiveString)
    )


  override def toString: String =
    s"${lines.mkString(", ")}${postcode.map(stringValue => s", $stringValue").getOrElse("")}"
}

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

case class SensitiveAddress(
                             lines:    Seq[SensitiveString],
                             postcode: Option[SensitiveString]
                           ) {

  def decrypted: Address =
    Address(
      lines     .map(_.decryptedValue),
      postcode  .map(_.decryptedValue)
    )
}

object SensitiveAddress {

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  implicit def format(implicit crypto: Encrypter with Decrypter): Format[SensitiveAddress] = {
        ((__ \ "lines"    ).format[Seq[SensitiveString]]
      ~  (__ \ "postcode" ).formatNullable[SensitiveString]
      )(SensitiveAddress.apply, unlift(SensitiveAddress.unapply)
    )
  }
}
