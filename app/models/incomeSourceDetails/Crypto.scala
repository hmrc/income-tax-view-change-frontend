/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package directdebit.corjourney.crypto

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText, SymmetricCryptoFactory}

@Singleton
class Crypto @Inject() (configuration: Configuration) extends Encrypter with Decrypter {

  implicit val aesCrypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  override def encrypt(plain: PlainContent): Crypted = aesCrypto.encrypt(plain)

  override def decrypt(reversiblyEncrypted: Crypted): PlainText = aesCrypto.decrypt(reversiblyEncrypted)

  override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = aesCrypto.decryptAsBytes(reversiblyEncrypted)
}