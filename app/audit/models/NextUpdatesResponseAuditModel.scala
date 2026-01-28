/*
 * Copyright 2023 HM Revenue & Customs
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
import enums.AuditType.AuditType.ViewObligationsResponse
import models.obligations.SingleObligationModel
import play.api.libs.json.*

import scala.language.implicitConversions

case class NextUpdatesResponseAuditModel(mtdItUser: MtdItUser[_],
                                         incomeSourceId: String,
                                         nextUpdates: Seq[SingleObligationModel]) extends ExtendedAuditModel {

  override val transactionName: String = "view-obligations-response"
  override val auditType: String = ViewObligationsResponse

  private def nextUpdateJson(nextUpdate: SingleObligationModel): JsObject = Json.obj(
    "startDate" -> nextUpdate.start,
    "endDate" -> nextUpdate.end,
    "dueDate" -> nextUpdate.due,
    "obligationType" -> nextUpdate.obligationType,
    "periodKey" -> nextUpdate.periodKey
  ) ++ Json.obj("dateReceived"-> nextUpdate.dateReceived)

  private val nextUpdatesJson: Seq[JsObject] = nextUpdates.map(nextUpdateJson)

  override val detail: JsValue = Json.obj(
    "mtditid" -> mtdItUser.mtditid,
    "nino" -> mtdItUser.nino,
    "incomeSourceId" -> incomeSourceId,
    "reportDeadlines" -> nextUpdatesJson
  ) ++
    Json.obj("saUtr"-> mtdItUser.saUtr) ++
    Json.obj("credId"-> mtdItUser.credId) ++
    Json.obj("userType"-> mtdItUser.userType) ++
    Json.obj("agentReferenceNumber"-> mtdItUser.arn)
}
