/*
 * Copyright 2021 HM Revenue & Customs
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

package models.chargeHistory

import models.chargeHistory.ChargesHistoryErrorModel.CODE_NO_DATA_FOUND
import play.api.libs.json._
import play.mvc.Http.Status

import scala.util.Try

sealed trait ChargeHistoryResponseModel


case class ChargesHistoryModel(idType: String,
                               idValue: String,
                               regimeType: String,
                               chargeHistoryDetails: Option[List[ChargeHistoryModel]]) extends ChargeHistoryResponseModel


object ChargesHistoryModel {
  implicit val format: Format[ChargesHistoryModel] = Json.format[ChargesHistoryModel]
}

case class ChargesHistoryErrorModel(status: Int, message: String) extends ChargeHistoryResponseModel {
  lazy val isNoDataFoundError: Boolean = {
    def apiErrorCodeOpt: Option[String] = Try((Json.parse(message) \ "code").as[String]).toOption

    status == Status.NOT_FOUND && apiErrorCodeOpt.contains(CODE_NO_DATA_FOUND)
  }
}

object ChargesHistoryErrorModel {
  val CODE_NO_DATA_FOUND: String = "NO_DATA_FOUND"
}
