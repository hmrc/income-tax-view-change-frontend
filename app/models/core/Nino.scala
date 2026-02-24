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

package models.core

import play.api.libs.json._

case class Nino(value: String) extends AnyVal
case class ViewHistory(nino: String) extends AnyVal

object Nino {
  implicit val writes: Writes[Nino] = Writes(nino => JsString(nino.value))
  implicit val reads: Reads[Nino]   = Reads(js => js.validate[String].map(Nino(_)))
  implicit val format: Format[Nino] = Format(reads, writes)}

object ViewHistory {
  implicit val writes: Writes[ViewHistory] = Writes(vh => Json.obj("nino" -> vh.nino))
  implicit val reads: Reads[ViewHistory]   = Reads(js => js.validate[String].map(ViewHistory(_)))
  implicit val format: Format[ViewHistory] = Format(reads, writes)}