/*
 * Copyright 2023 HM Revenue & Customs
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

package models.obligations

import play.api.libs.json._

sealed trait ObligationStatus

case object StatusOpen extends ObligationStatus {
  override def toString: String = "O"
}

case object StatusFulfilled extends ObligationStatus {
  override def toString: String = "F"
}

object ObligationStatus {
  implicit val obligationStatusWrites: Writes[ObligationStatus] = Writes[ObligationStatus] {
    case StatusOpen => JsString("O")
    case StatusFulfilled => JsString("F")
  }

  implicit val obligationStatusReads: Reads[ObligationStatus] = Reads[ObligationStatus] {
    case JsString("O") => JsSuccess(StatusOpen)
    case JsString("F") => JsSuccess(StatusFulfilled)
    case _ => JsError("Unknown Obligation Status")
  }

  implicit val obligationStatusFormat: Format[ObligationStatus] = Format(obligationStatusReads, obligationStatusWrites)
}
