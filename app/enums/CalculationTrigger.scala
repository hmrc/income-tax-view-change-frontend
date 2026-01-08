/*
 * Copyright 2026 HM Revenue & Customs
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

sealed trait CalculationTrigger

case object Attended extends CalculationTrigger
case object Class2NicEvent extends CalculationTrigger
case object Unattended extends CalculationTrigger
case object CesaSAReturn extends CalculationTrigger

object CalculationTrigger {

  val values: Map[String, CalculationTrigger] =
    Map(
      "Attended" -> Attended,
      "Class2NicEvent" -> Class2NicEvent,
      "Unattended" -> Unattended,
      "CesaSAReturn" -> CesaSAReturn
    )

  implicit val reads: Reads[CalculationTrigger] =
    Reads {
      case JsString(value) =>
        values.get(value) match {
          case Some(trigger) =>
            JsSuccess(trigger)
          case None =>
            JsError(s"Unknown calculationTrigger: $value")
        }
      case _ =>
        JsError("calculationTrigger must be a string")
    }

  implicit val writes: Writes[CalculationTrigger] =
    Writes {
      case Attended => JsString("Attended")
      case Class2NicEvent => JsString("Class2NicEvent")
      case Unattended => JsString("Unattended")
      case CesaSAReturn => JsString("CesaSAReturn")
    }
}