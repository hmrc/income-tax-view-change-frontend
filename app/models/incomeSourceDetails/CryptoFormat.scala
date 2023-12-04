/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package directdebit.corjourney.crypto

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

sealed trait CryptoFormat extends Product with Serializable

object CryptoFormat {

  @Singleton
  final case class OperationalCryptoFormat @Inject() (crypto: Encrypter with Decrypter) extends CryptoFormat

  case object NoOpCryptoFormat extends CryptoFormat

}