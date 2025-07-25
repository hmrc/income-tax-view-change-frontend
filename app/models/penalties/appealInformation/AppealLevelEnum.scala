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

sealed trait AppealLevelEnum {
  val value: String
  override def toString: String = value
}

case object HmrcAppealLevel extends AppealLevelEnum {
  override val value: String = "01"
}

case object TribunalAppealLevel extends AppealLevelEnum {
  override val value: String = "02"
}

case object ThirdAppealLevel extends AppealLevelEnum {
  override val value: String = "03"
}

object AppealLevelEnum {
  implicit val writes: Writes[AppealLevelEnum] = Writes {
    case HmrcAppealLevel => JsString(HmrcAppealLevel.value)
    case TribunalAppealLevel => JsString(TribunalAppealLevel.value)
  }

  implicit val reads: Reads[AppealLevelEnum] = Reads {
    case JsString(value) => value.toUpperCase match {
      case "01" => JsSuccess(HmrcAppealLevel)
      case "02" => JsSuccess(TribunalAppealLevel)
      case "03" => JsSuccess(ThirdAppealLevel)
      case e => JsError(s"$e not recognised as appeal level value")
    }
    case _ => JsError("Invalid JSON value")
  }

  implicit val format: Format[AppealLevelEnum] = Format(reads, writes)
}
