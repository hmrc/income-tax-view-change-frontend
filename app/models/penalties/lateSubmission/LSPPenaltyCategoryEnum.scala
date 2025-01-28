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

object LSPPenaltyCategoryEnum extends Enumeration {

  val Point: LSPPenaltyCategoryEnum.Value = Value("P")
  val Threshold: LSPPenaltyCategoryEnum.Value = Value("T")
  val Charge: LSPPenaltyCategoryEnum.Value = Value("C")

  implicit val format: Format[LSPPenaltyCategoryEnum.Value] = new Format[LSPPenaltyCategoryEnum.Value] {
    override def writes(o: LSPPenaltyCategoryEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[LSPPenaltyCategoryEnum.Value] = {
      json.as[String].toUpperCase match {
        case "P" => JsSuccess(Point)
        case " " => JsSuccess(Point)
        case "T" => JsSuccess(Threshold)
        case "C" => JsSuccess(Charge)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
