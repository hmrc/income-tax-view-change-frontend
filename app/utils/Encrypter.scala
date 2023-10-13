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

package utils

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import utils.Cypher.stringCypher
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

object Encrypter {

  case class KeyValue(key: String, value: String) {

    def encryptKeyValue(key: String, value: String)(implicit sessionId: String): Unit = {
      EncryptedKeyValue(
        key = key, value = encrypt(value, key)
      )
    }

    def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedKeyValue = {

      println(s"\nENCRYPTED: ${value.encrypted}\n")

      EncryptedKeyValue(
        key = key,
        value = value.encrypted
      )
    }
  }

  case class EncryptedKeyValue(key: String, value: EncryptedValue) {

    def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): KeyValue =
      KeyValue(
        key = key,
        value = value.decrypted
      )
  }

  object EncryptedKeyValue {
    implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

    implicit val format: Format[EncryptedKeyValue] = Json.format[EncryptedKeyValue]
  }

  private def encrypt(field: String, key: String)(implicit sessionId: String): EncryptedValue =
    SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(field, sessionId)
}
