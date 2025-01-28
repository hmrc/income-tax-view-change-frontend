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

object TaxReturnStatusEnum extends Enumeration {

  val Open: TaxReturnStatusEnum.Value = Value
  val Fulfilled: TaxReturnStatusEnum.Value = Value
  val Reversed: TaxReturnStatusEnum.Value = Value

  implicit val format: Format[TaxReturnStatusEnum.Value] = new Format[TaxReturnStatusEnum.Value] {
    override def reads(json: JsValue): JsResult[TaxReturnStatusEnum.Value] = json.as[String].toUpperCase match {
      case "OPEN" => JsSuccess(Open)
      case "FULFILLED" => JsSuccess(Fulfilled)
      case "REVERSED" => JsSuccess(Reversed)
      case e => JsError(s"$e not recognised")
    }

    override def writes(o: TaxReturnStatusEnum.Value): JsValue = JsString(o.toString)
  }

}
