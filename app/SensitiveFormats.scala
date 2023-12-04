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

package models

import models.SensitiveFormats._
import play.api.libs.json._
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

class SensitiveFormats(encryptionEnabled: Boolean)(implicit crypto: Encrypter with Decrypter) {

  val jsObjectReads: Reads[JsObject] =
    JsonEncryption.sensitiveDecrypter(SensitiveString.apply).map(_.decrypt) orElse
      implicitly[Reads[JsObject]]

  val jsObjectWrites: Writes[JsObject] =
    if (encryptionEnabled) {
      JsonEncryption.sensitiveEncrypter[String, SensitiveString].contramap(_.encrypt)
    } else {
      implicitly[Writes[JsObject]]
    }
}

object SensitiveFormats {

  implicit class RichJsObject(jsObject: JsObject) {
    def encrypt: SensitiveString = SensitiveString(Json.stringify(jsObject))
  }

  implicit class RichSensitiveString(sensitiveString: SensitiveString) {
    def decrypt: JsObject = Json.parse(sensitiveString.decryptedValue).as[JsObject]
  }
}
