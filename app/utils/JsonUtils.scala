/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait JsonUtils {

  def jsonObjNoNulls(fields: (String, Json.JsValueWrapper)*): JsObject =
    JsObject(Json.obj(fields:_*).fields.filterNot(_._2 == JsNull).filterNot(_._2 == Json.obj()))

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val readsLocalDateTime: Reads[LocalDateTime] = Reads[LocalDateTime] {js =>
    js.validate[String].map[LocalDateTime](dtString => LocalDateTime.parse(dtString, dateTimeFormatter))
  }

  implicit val writesLocalDateTime: Writes[LocalDateTime] = (dateTime: LocalDateTime) =>
    JsString(dateTimeFormatter.format(dateTime))

}
