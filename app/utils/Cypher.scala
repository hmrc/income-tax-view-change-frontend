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

import uk.gov.hmrc.crypto.EncryptedValue

import java.time.{Instant, Month}

trait Cypher[A] {
  self =>

  def encrypt(value: A)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedValue

  def decrypt(encryptedValue: EncryptedValue)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): A

  def imap[B](dec: A => B, enc: B => A): Cypher[B] = new Cypher[B] {
    override def encrypt(value: B)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedValue =
      self.encrypt(enc(value))

    override def decrypt(encryptedValue: EncryptedValue)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): B =
      dec(self.decrypt(encryptedValue))
  }
}

object Cypher {

  implicit val stringCypher: Cypher[String] = new Cypher[String] {
    override def encrypt(value: String)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedValue =
      aesGcmAdCrypto.encrypt(value)

    override def decrypt(encryptedValue: EncryptedValue)(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): String =
      aesGcmAdCrypto.decrypt(encryptedValue)
  }

  implicit val booleanCypher: Cypher[Boolean] = stringCypher.imap(_.toBoolean, _.toString)

  implicit val bigDecimalCypher: Cypher[BigDecimal] = stringCypher.imap(BigDecimal(_), _.toString)

  implicit val instantCypher: Cypher[Instant] = stringCypher.imap(Instant.parse, _.toString)

  implicit val monthCypher: Cypher[Month] = stringCypher.imap(Month.valueOf, _.toString)
}

object CypherSyntax {
  implicit class EncryptableOps[A](value: A)(implicit cypher: Cypher[A]) {
    def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedValue = cypher.encrypt(value)
  }

  implicit class DecryptableOps(encryptedValue: EncryptedValue) {
    def decrypted[A](implicit cypher: Cypher[A], aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): A = cypher.decrypt(encryptedValue)
  }
}
