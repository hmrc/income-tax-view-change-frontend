/*
 * Copyright 2018 HM Revenue & Customs
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

package enums

import play.api.libs.json._

sealed trait CalcStatus {
  val name: String
}

object CalcStatus {
  implicit object Format extends Reads[CalcStatus] with Writes[CalcStatus] {
    override def reads(json: JsValue): JsResult[CalcStatus] =
      json.validate[JsString] map {
        case JsString(name) => statusNames(name)
      }
    override def writes(s: CalcStatus): JsValue = JsString(s.name)

    val statuses = Seq(Estimate, Crystallised)

    val statusNames: Map[String, CalcStatus with Product with Serializable] = (statuses map { s =>
      s.name -> s
    }).toMap
  }
}

case object Estimate extends CalcStatus {
  val name = "Estimate"
}
case object Crystallised extends CalcStatus {
  val name = "Crystallised"
}
