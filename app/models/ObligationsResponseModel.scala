/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json, OFormat}

sealed trait ObligationsResponseModel

case class ObligationsModel(obligations: List[ObligationModel]) extends ObligationsResponseModel

case class ObligationModel(start: LocalDate,
                           end: LocalDate,
                           due: LocalDate,
                           met: Boolean
                          ) extends ObligationsResponseModel {
  def currentTime(): LocalDate = LocalDate.now()

  def getObligationStatus: ObligationStatus = (met, due) match {
      case (true, _)                                          => Received
      case (false, date) if !currentTime().isAfter(date)      => Open(date)
      case (false, _)                                         => Overdue
    }
}

case class ObligationsErrorModel(code: Int, message: String) extends ObligationsResponseModel

object ObligationModel {
  implicit val format: OFormat[ObligationModel] = Json.format[ObligationModel]
}

object ObligationsModel {
  implicit val format: OFormat[ObligationsModel] = Json.format[ObligationsModel]
}

object ObligationsErrorModel {
  implicit val format: OFormat[ObligationsErrorModel] = Json.format[ObligationsErrorModel]
}

object ObligationsResponseModel {
  def unapply(obsRespModel: ObligationsResponseModel): Option[(String, JsValue)] = {
    val (p: Product, sub) = obsRespModel match {
      case model: ObligationsModel => (model, Json.toJson(model)(ObligationsModel.format))
      case model: ObligationModel => (model, Json.toJson(model)(ObligationModel.format))
      case error: ObligationsErrorModel => (error, Json.toJson(error)(ObligationsErrorModel.format))
    }
    Some(p.productPrefix -> sub)
  }
  def apply(`class`: String, data: JsValue): ObligationsResponseModel ={
    (`class` match {
      case "ObligationsModel" => Json.fromJson[ObligationsModel](data)(ObligationsModel.format)
      case "ObligationModel" => Json.fromJson[ObligationModel](data)(ObligationModel.format)
      case "ObligationsErrorModel" => Json.fromJson[ObligationsErrorModel](data)(ObligationsErrorModel.format)
    }).get
  }
}
