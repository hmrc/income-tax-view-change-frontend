/*
 * Copyright 2022 HM Revenue & Customs
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
import enums.ViewObligationsResponse
import models.nextUpdates.NextUpdateModel
import play.api.libs.json._
import utils.Utilities.JsonUtil

case class NextUpdatesResponseAuditModel(mtdItUser: MtdItUser[_],
                                         incomeSourceId: String,
                                         nextUpdates: Seq[NextUpdateModel]) extends ExtendedAuditModel {

  override val transactionName: String = "view-obligations-response"
  override val auditType: String = ViewObligationsResponse.name

  private def nextUpdateJson(nextUpdate: NextUpdateModel): JsObject = Json.obj(
    "startDate" -> nextUpdate.start,
    "endDate" -> nextUpdate.end,
    "dueDate" -> nextUpdate.due,
    "obligationType" -> nextUpdate.obligationType,
    "periodKey" -> nextUpdate.periodKey
  ) ++ ("dateReceived", nextUpdate.dateReceived)

  private val nextUpdatesJson: Seq[JsObject] = nextUpdates.map(nextUpdateJson)

  override val detail: JsValue = Json.obj(
    "mtditid" -> mtdItUser.mtditid,
    "nationalInsuranceNumber" -> mtdItUser.nino,
    "incomeSourceId" -> incomeSourceId,
    "reportDeadlines" -> nextUpdatesJson
  ) ++
    ("saUtr", mtdItUser.saUtr) ++
    ("credId", mtdItUser.credId) ++
    ("userType", mtdItUser.userType) ++
    ("agentReferenceNumber", mtdItUser.arn)
}
