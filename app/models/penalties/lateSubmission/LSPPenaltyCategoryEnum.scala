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

package models.penalties.lateSubmission

import play.api.libs.json._

sealed trait LSPPenaltyCategoryEnum {
  val value: String
  override def toString: String = value
}

case object Point extends LSPPenaltyCategoryEnum {
  override val value: String = "P"
}

case object Threshold extends LSPPenaltyCategoryEnum {
  override val value: String = "T"
}

case object Charge extends LSPPenaltyCategoryEnum {
  override val value: String = "C"
}

object LSPPenaltyCategoryEnum {
  implicit val writes: Writes[LSPPenaltyCategoryEnum] = Writes {
    case Point => JsString(Point.value)
    case Threshold => JsString(Threshold.value)
    case Charge => JsString(Charge.value)
  }

  implicit val reads: Reads[LSPPenaltyCategoryEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "P" => JsSuccess(Point)
      case "T" => JsSuccess(Threshold)
      case "C" => JsSuccess(Charge)
      case e => JsError(s"$e not recognised as a LSP penalty category")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[LSPPenaltyCategoryEnum] = Format(reads, writes)
}
