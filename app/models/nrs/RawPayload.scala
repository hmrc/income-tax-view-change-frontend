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

package models.nrs

import org.apache.pekko.util.ByteString

import java.io.{ByteArrayInputStream, InputStream}
import scala.xml.NodeSeq

case class RawPayload(byteString: ByteString, encoding: Option[String]) {
  lazy val byteArray: Array[Byte] = byteString.toArray

  def length: Int = byteString.length

  def inputStream: InputStream = new ByteArrayInputStream(byteArray)
}

object RawPayload {
  // For testing
  def apply(string: String): RawPayload = RawPayload(ByteString.fromString(string), Some("UTF-8"))

  // For testing
  def apply(xml: NodeSeq): RawPayload = RawPayload(ByteString.fromString(xml.toString), None)

  // For testing
  def apply(bytes: Array[Byte], encoding: Option[String] = None): RawPayload = RawPayload(ByteString(bytes), encoding)
}
