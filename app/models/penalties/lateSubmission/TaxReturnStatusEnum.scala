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

sealed trait TaxReturnStatusEnum

case object Open extends TaxReturnStatusEnum
case object Fulfilled extends TaxReturnStatusEnum
case object Reversed extends TaxReturnStatusEnum

object TaxReturnStatusEnum {
  implicit val writes: Writes[TaxReturnStatusEnum] = Writes {
    case Open => JsString("Open")
    case Fulfilled => JsString("Fulfilled")
    case Reversed => JsString("Reversed")
  }

  implicit val reads: Reads[TaxReturnStatusEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "OPEN" => JsSuccess(Open)
      case "FULFILLED" => JsSuccess(Fulfilled)
      case "REVERSED" => JsSuccess(Reversed)
      case e => JsError(s"$e not recognised as a tax return status")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[TaxReturnStatusEnum] = Format(reads, writes)
}

