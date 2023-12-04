/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package directdebit.corjourney

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption

package object crypto {

  val noOpSensitiveStringFormat: Format[SensitiveString] = Format(
    Reads {
      case JsString(s) => JsSuccess(SensitiveString(s))
      case other       => JsError(s"Expected JsString but got ${other.getClass.getSimpleName}")
    },
    Writes(s => JsString(s.decryptedValue))
  )

  def sensitiveStringFormat(cryptoFormat: CryptoFormat): Format[SensitiveString] = cryptoFormat match {
    case CryptoFormat.OperationalCryptoFormat(crypto) =>
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)(implicitly[Format[String]], crypto)

    case CryptoFormat.NoOpCryptoFormat =>
      noOpSensitiveStringFormat
  }

}