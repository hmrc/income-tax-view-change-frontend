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

import config.FrontendAppConfig
import uk.gov.hmrc.crypto.EncryptedValue

import javax.inject.{Inject, Singleton}

@Singleton
class AesGcmAdCrypto @Inject()(appConfig: FrontendAppConfig,
                               aesGcmAdCryptoFactory: AesGcmAdCryptoFactory) {

  private lazy val aesGcmAdCrypto = aesGcmAdCryptoFactory.instance()

  def encrypt(valueToEncrypt: String)
             (implicit associatedText: String): EncryptedValue = {
    if (appConfig.useEncryption)
      aesGcmAdCrypto.encrypt(valueToEncrypt, associatedText)
    else
      EncryptedValue(valueToEncrypt, s"$valueToEncrypt-Nonce")
  }

  def decrypt(encryptedValue: EncryptedValue)
             (implicit associatedText: String): String = {
    if (appConfig.useEncryption)
      aesGcmAdCrypto.decrypt(encryptedValue, associatedText)
    else
      encryptedValue.value
  }
}
