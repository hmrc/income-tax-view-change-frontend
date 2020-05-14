/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json._



sealed trait ReportDeadlinesResponseModel

case class ObligationsModel(obligations: Seq[ReportDeadlinesModel]) extends ReportDeadlinesResponseModel

object ObligationsModel {
  implicit val format: OFormat[ObligationsModel] = Json.format[ObligationsModel]
}

case class ReportDeadlinesModel(identification: String, obligations: List[ReportDeadlineModel]) {
  val currentQuarterlyDeadlines: List[ReportDeadlineModel] = obligations.filter(_.obligationType == "Quarterly").sortBy(_.start.toEpochDay)
  val currentEOPsDeadlines: List[ReportDeadlineModel] = obligations.filter(_.obligationType == "EOPS").sortBy(_.start.toEpochDay)
  val currentCrystDeadlines: List[ReportDeadlineModel] = obligations.filter(_.obligationType == "Crystallised").sortBy(_.start.toEpochDay)
}

case class ReportDeadlineModel(start: LocalDate,
                               end: LocalDate,
                               due: LocalDate,
                               obligationType: String,
                               dateReceived: Option[LocalDate],
                               periodKey: String) extends ReportDeadlinesResponseModel {

  def currentTime(): LocalDate = LocalDate.now()

  def getReportDeadlineStatus: ReportDeadlineStatus = if (!currentTime().isAfter(due)) Open(due) else Overdue(due)
}

case class ReportDeadlineModelWithIncomeType(incomeType: String, obligation: ReportDeadlineModel)

case class ReportDeadlinesErrorModel(code: Int, message: String) extends ReportDeadlinesResponseModel

object ReportDeadlineModel {
  implicit val format: Format[ReportDeadlineModel] = Json.format[ReportDeadlineModel]
  val auditWrites: Writes[ReportDeadlineModel] = (
    (__ \ "startDate").write[LocalDate] and
      (__ \ "endDate").write[LocalDate] and
      (__ \ "dueDate").write[LocalDate] and
      (__ \ "obligationType").write[String] and
      (__ \ "dateReceived").writeNullable[LocalDate] and
      (__ \ "periodKey").write[String]
  )(unlift(ReportDeadlineModel.unapply))
}

object ReportDeadlinesModel {
  implicit val format: Format[ReportDeadlinesModel] = Json.format[ReportDeadlinesModel]
}

object ReportDeadlinesErrorModel {
  implicit val format: Format[ReportDeadlinesErrorModel] = Json.format[ReportDeadlinesErrorModel]
}
