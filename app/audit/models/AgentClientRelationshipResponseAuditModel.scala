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
import play.api.libs.json.{JsValue, Json}
import utils.Utilities._

case class AgentClientRelationshipResponseAuditModel(saUtr: String,
                                                     agentReferenceNumber: Option[String],
                                                     validRelationship: Boolean) extends ExtendedAuditModel {

  override val transactionName: String = "agent-client-relationship"
  override val auditType: String = "AgentClientRelationship"

  override val detail: JsValue = {
    Json.obj(
      "saUtr" -> saUtr,
      "validRelationship" -> validRelationship) ++
      ("agentReferenceNumber", agentReferenceNumber)
  }

}
