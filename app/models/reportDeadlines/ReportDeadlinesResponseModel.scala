/*
 * Copyright 2018 HM Revenue & Customs
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

package models.reportDeadlines

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait ReportDeadlinesResponseModel

case class ReportDeadlinesModel(obligations: List[ReportDeadlineModel]) extends ReportDeadlinesResponseModel

case class ReportDeadlineModel(start: LocalDate,
                               end: LocalDate,
                               due: LocalDate,
                               periodKey: String) extends ReportDeadlinesResponseModel {

  def currentTime(): LocalDate = LocalDate.now()

  def getReportDeadlineStatus: ReportDeadlineStatus = if (!currentTime().isAfter(due)) Open(due) else Overdue(due)

  def obligationType: ObligationType = if(ChronoUnit.MONTHS.between(start, end).abs > 3) EopsObligation else QuarterlyObligation
}

case class ReportDeadlinesErrorModel(code: Int, message: String) extends ReportDeadlinesResponseModel

object ReportDeadlineModel {
  implicit val format: Format[ReportDeadlineModel] = Json.format[ReportDeadlineModel]
  val reportDeadlinesAuditWrites: Writes[ReportDeadlineModel] = (
    (__ \ "start").write[LocalDate] and
      (__ \ "end").write[LocalDate] and
      (__ \ "due").write[LocalDate] and
      (__ \ "periodKey").write[String] and
      OWrites[Any](_ => Json.obj())
    )(unlift(ReportDeadlineModel.unapply))
}

object ReportDeadlinesModel {
  implicit val format: Format[ReportDeadlinesModel] = Json.format[ReportDeadlinesModel]
}

object ReportDeadlinesErrorModel {
  implicit val format: Format[ReportDeadlinesErrorModel] = Json.format[ReportDeadlinesErrorModel]
}

object ReportDeadlinesResponseModel {
  def unapply(obsRespModel: ReportDeadlinesResponseModel): Option[(String, JsValue)] = {
    val (p: Product, sub) = obsRespModel match {
      case model: ReportDeadlinesModel => (model, Json.toJson(model)(ReportDeadlinesModel.format))
      case model: ReportDeadlineModel => (model, Json.toJson(model)(ReportDeadlineModel.format))
      case error: ReportDeadlinesErrorModel => (error, Json.toJson(error)(ReportDeadlinesErrorModel.format))
    }
    Some(p.productPrefix -> sub)
  }
  def apply(`class`: String, data: JsValue): ReportDeadlinesResponseModel ={
    (`class` match {
      case "ReportDeadlinesModel" => Json.fromJson[ReportDeadlinesModel](data)(ReportDeadlinesModel.format)
      case "ReportDeadlineModel" => Json.fromJson[ReportDeadlineModel](data)(ReportDeadlineModel.format)
      case "ReportDeadlinesErrorModel" => Json.fromJson[ReportDeadlinesErrorModel](data)(ReportDeadlinesErrorModel.format)
    }).get
  }
  implicit val format: Format[ReportDeadlinesResponseModel] = Json.format[ReportDeadlinesResponseModel]
}
