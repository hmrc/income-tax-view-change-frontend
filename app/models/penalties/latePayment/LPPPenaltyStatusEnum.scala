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

object LPPPenaltyStatusEnum extends Enumeration {

  val Accruing: LPPPenaltyStatusEnum.Value = Value("A")
  val Posted: LPPPenaltyStatusEnum.Value = Value("P")

  implicit val format: Format[LPPPenaltyStatusEnum.Value] = new Format[LPPPenaltyStatusEnum.Value] {
    override def writes(o: LPPPenaltyStatusEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[LPPPenaltyStatusEnum.Value] = {
      json.as[String].toUpperCase match {
        case "A" => JsSuccess(Accruing)
        case "P" => JsSuccess(Posted)
        case e => JsError(s"$e not recognised")
      }
    }
  }

}
