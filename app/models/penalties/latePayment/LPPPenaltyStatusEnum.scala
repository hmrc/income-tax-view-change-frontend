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

package models.penalties.latePayment

import play.api.libs.json._

sealed trait LPPPenaltyStatusEnum {
  val value: String
  override def toString: String = value
}

case object Accruing extends LPPPenaltyStatusEnum {
  override val value: String = "A"
}

case object Posted extends LPPPenaltyStatusEnum {
  override val value: String = "P"
}

object LPPPenaltyStatusEnum {
  implicit val reads: Reads[LPPPenaltyStatusEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "A" => JsSuccess(Accruing)
      case "P" => JsSuccess(Posted)
      case e => JsError(s"$e not recognised as a LPP penalty status")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val writes: Writes[LPPPenaltyStatusEnum] = Writes {
    case Accruing => JsString(Accruing.value)
    case Posted => JsString(Posted.value)
  }

  implicit val format: Format[LPPPenaltyStatusEnum] = Format(reads, writes)
}
