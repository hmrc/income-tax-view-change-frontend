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

package testOnly.models

case class Nino(nino: String) {
  val ninoWithoutDescription: String = nino.split(" ").headOption.getOrElse("No description found")

  require(Nino.isValid(ninoWithoutDescription), s"$ninoWithoutDescription is not a valid nino.")

  override def toString = ninoWithoutDescription

  def value = ninoWithoutDescription

  val name = "nino"
}

object Nino extends (String => Nino) {

  private val validNinoFormat = "[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")

  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.startsWith)

  def isValid(nino: String): Boolean = hasValidPrefix(nino) && nino.matches(validNinoFormat)

}
