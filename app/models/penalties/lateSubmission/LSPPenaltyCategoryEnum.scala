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

case object PointLSPPenaltyCategory extends LSPPenaltyCategoryEnum {
  override val value: String = "P"
}

case object ThresholdLSPPenaltyCategory extends LSPPenaltyCategoryEnum {
  override val value: String = "T"
}

case object ChargeLSPPenaltyCategory extends LSPPenaltyCategoryEnum {
  override val value: String = "C"
}

object LSPPenaltyCategoryEnum {
  implicit val writes: Writes[LSPPenaltyCategoryEnum] = Writes {
    case PointLSPPenaltyCategory => JsString(PointLSPPenaltyCategory.value)
    case ThresholdLSPPenaltyCategory => JsString(ThresholdLSPPenaltyCategory.value)
    case ChargeLSPPenaltyCategory => JsString(ChargeLSPPenaltyCategory.value)
  }

  implicit val reads: Reads[LSPPenaltyCategoryEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "P" => JsSuccess(PointLSPPenaltyCategory)
      case "T" => JsSuccess(ThresholdLSPPenaltyCategory)
      case "C" => JsSuccess(ChargeLSPPenaltyCategory)
      case e => JsError(s"$e not recognised as a LSP penalty category")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[LSPPenaltyCategoryEnum] = Format(reads, writes)
}
