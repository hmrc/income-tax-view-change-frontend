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

package models.penalties.appealInformation

import play.api.libs.json._

sealed trait AppealStatusEnum {
  val value: String
  override def toString: String = value
}

case object UnderAppealStatus extends AppealStatusEnum {
  override val value: String = "A"
}
case object UpheldStatus extends AppealStatusEnum {
  override val value: String = "B"
}
case object RejectedStatus extends AppealStatusEnum {
  override val value: String = "C"
}
case object UnappealableStatus extends AppealStatusEnum {
  override val value: String = "99"
}
case object AppealRejectedChargeAlreadyReversed extends AppealStatusEnum {
  override val value: String = "91"
}
case object AppealUpheldPointAlreadyRemoved extends AppealStatusEnum {
  override val value: String = "92"
}
case object AppealUpheldChargeAlreadyReversed extends AppealStatusEnum {
  override val value: String = "93"
}
case object AppealRejectedPointAlreadyRemoved extends AppealStatusEnum {
  override val value: String = "94"
}

object AppealStatusEnum {
  implicit val writes: Writes[AppealStatusEnum] = Writes {
    case UnderAppealStatus => JsString(UnderAppealStatus.value)
    case UpheldStatus => JsString(UpheldStatus.value)
    case RejectedStatus => JsString(RejectedStatus.value)
    case UnappealableStatus => JsString(UnappealableStatus.value)
    case AppealRejectedChargeAlreadyReversed => JsString(AppealRejectedChargeAlreadyReversed.value)
    case AppealUpheldPointAlreadyRemoved => JsString(AppealUpheldPointAlreadyRemoved.value)
    case AppealUpheldChargeAlreadyReversed => JsString(AppealUpheldChargeAlreadyReversed.value)
    case AppealRejectedPointAlreadyRemoved => JsString(AppealRejectedPointAlreadyRemoved.value)
  }

  implicit val reads: Reads[AppealStatusEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "A" => JsSuccess(UnderAppealStatus)
      case "B" => JsSuccess(UpheldStatus)
      case "C" => JsSuccess(RejectedStatus)
      case "99" => JsSuccess(UnappealableStatus)
      case "91" => JsSuccess(AppealRejectedChargeAlreadyReversed)
      case "92" => JsSuccess(AppealUpheldPointAlreadyRemoved)
      case "93" => JsSuccess(AppealUpheldChargeAlreadyReversed)
      case "94" => JsSuccess(AppealRejectedPointAlreadyRemoved)
      case e => JsError(s"$e not recognised as appeal status value")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[AppealStatusEnum] = Format(reads, writes)
}
