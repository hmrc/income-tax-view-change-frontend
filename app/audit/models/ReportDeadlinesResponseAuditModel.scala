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

package audit.models

import auth.MtdItUser
import models.reportDeadlines.ReportDeadlineModel
import play.api.libs.json._
import utils.Utilities.JsonUtil

case class ReportDeadlinesResponseAuditModel(mtdItUser: MtdItUser[_],
                                             incomeSourceId: String,
                                             reportDeadlines: Seq[ReportDeadlineModel]) extends ExtendedAuditModel {

  override val transactionName: String = "view-obligations-response"
  override val auditType: String = "ViewObligationsResponse"

  private def reportDeadineJson(reportDeadline: ReportDeadlineModel): JsObject = Json.obj(
    "startDate" -> reportDeadline.start,
    "endDate" -> reportDeadline.end,
    "dueDate" -> reportDeadline.due,
    "obligationType" -> reportDeadline.obligationType,
    "periodKey" -> reportDeadline.periodKey
  ) ++ ("dateReceived", reportDeadline.dateReceived)

  private val reportDeadlinesJson: Seq[JsObject] = reportDeadlines.map(reportDeadineJson)

  override val detail: JsValue = Json.obj(
    "mtditid" -> mtdItUser.mtditid,
    "nationalInsuranceNumber" -> mtdItUser.nino,
    "incomeSourceId" -> incomeSourceId,
    "reportDeadlines" -> reportDeadlinesJson
  ) ++
    ("saUtr", mtdItUser.saUtr) ++
    ("credId", mtdItUser.credId) ++
    ("userType", mtdItUser.userType) ++
    ("agentReferenceNumber", mtdItUser.arn)
}