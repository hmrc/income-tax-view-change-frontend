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

object LSPPenaltyStatusEnum extends Enumeration {

  val Active: LSPPenaltyStatusEnum.Value = Value("ACTIVE")
  val Inactive: LSPPenaltyStatusEnum.Value = Value("INACTIVE")

  implicit val format: Format[LSPPenaltyStatusEnum.Value] = new Format[LSPPenaltyStatusEnum.Value] {
    override def writes(o: LSPPenaltyStatusEnum.Value): JsValue = {
      JsString(o.toString)
    }

    override def reads(json: JsValue): JsResult[LSPPenaltyStatusEnum.Value] = {
      json.as[String].toUpperCase match {
        case "ACTIVE" => JsSuccess(Active)
        case "INACTIVE" => JsSuccess(Inactive)
        case e => JsError(s"$e not recognised")
      }
    }
  }

}
