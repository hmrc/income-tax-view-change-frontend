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

sealed trait LSPPenaltyStatusEnum

case object ActiveLSPPenaltyStatus extends LSPPenaltyStatusEnum
case object InactiveLSPPenaltyStatus extends LSPPenaltyStatusEnum

object LSPPenaltyStatusEnum {

  implicit val writes: Writes[LSPPenaltyStatusEnum] = Writes {
    case ActiveLSPPenaltyStatus => JsString("ACTIVE")
    case InactiveLSPPenaltyStatus => JsString("INACTIVE")
  }

  implicit val reads: Reads[LSPPenaltyStatusEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "ACTIVE" => JsSuccess(ActiveLSPPenaltyStatus)
      case "INACTIVE" => JsSuccess(InactiveLSPPenaltyStatus)
      case e => JsError(s"$e not recognised as a LSP penalty status")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[LSPPenaltyStatusEnum] = Format(reads, writes)

}