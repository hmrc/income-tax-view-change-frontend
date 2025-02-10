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

sealed trait LPPPenaltyCategoryEnum {
  val value: String
  override def toString: String = value
}

case object FirstPenalty extends LPPPenaltyCategoryEnum {
  override val value: String = "LPP1"
}
case object SecondPenalty extends LPPPenaltyCategoryEnum {
  override val value: String = "LPP2"
}

object LPPPenaltyCategoryEnum {
  implicit val writes: Writes[LPPPenaltyCategoryEnum] = Writes {
    case FirstPenalty => JsString(FirstPenalty.value)
    case SecondPenalty => JsString(SecondPenalty.value)
  }

  implicit val reads: Reads[LPPPenaltyCategoryEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "LPP1" => JsSuccess(FirstPenalty)
      case "LPP2" => JsSuccess(SecondPenalty)
      case e => JsError(s"$e not recognised as a LPP category")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[LPPPenaltyCategoryEnum] = Format(reads, writes)
}
