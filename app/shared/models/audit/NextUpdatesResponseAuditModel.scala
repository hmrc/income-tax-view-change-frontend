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

package shared.models.audit

import common.auth.MtdItUser
import common.models.obligations.SingleObligationModel
import common.models.audit.AuditEvent
import uk.gov.hmrc.auth.core.AffinityGroup

import play.api.libs.json.*
import play.api.libs.functional.syntax._
import java.time.LocalDate

case class NextUpdatesResponseAuditModel(
  mtditid: String,
  nino: String,
  incomeSourceId: String,
  reportDeadlines: Seq[SingleObligationModel],
  saUtr: Option[String],
  credId: Option[String],
  userType: Option[AffinityGroup],
  agentReferenceNumber: Option[String]
) extends AuditEvent {
  override def auditType: String = "ViewObligationsResponse"
}

object NextUpdatesResponseAuditModel:

  def apply(incomeSourceId: String, nextUpdates: Seq[SingleObligationModel])(using user: MtdItUser[?]): NextUpdatesResponseAuditModel =
    apply(user, incomeSourceId, nextUpdates)

  def apply(user: MtdItUser[?], incomeSourceId: String, nextUpdates: Seq[SingleObligationModel]): NextUpdatesResponseAuditModel =
    NextUpdatesResponseAuditModel(
      user.mtditid,
      user.nino,
      incomeSourceId,
      nextUpdates,
      user.saUtr,
      user.credId,
      user.userType,
      user.arn
    )

  given Writes[NextUpdatesResponseAuditModel] = Json.writes[NextUpdatesResponseAuditModel]
  given specificWrites: Writes[SingleObligationModel] = (
      (__ \ "startDate").write[LocalDate] and
      (__ \ "endDate").write[LocalDate] and 
      (__ \ "dueDate").write[LocalDate] and
      (__ \ "obligationType").write[String] and
      (__ \ "periodKey").write[String] and
      (__ \ "dateReceived").writeNullable[LocalDate]
    )(model => (model.start, model.end, model.due, model.obligationType, model.periodKey, model.dateReceived))
