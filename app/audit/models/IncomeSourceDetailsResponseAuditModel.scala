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

package audit.models

import play.api.libs.json.{JsValue, Json, Writes}

case class IncomeSourceDetailsResponseAuditModel(mtditid: String,
                                                 nino: String,
                                                 selfEmploymentIds: List[String],
                                                 propertyIncomeId: Option[String]) extends ExtendedAuditModel {

  override val transactionName: String = "income-source-details-response"
  override val auditType: String = "incomeSourceDetailsResponse"

  private case class AuditDetail(mtditid: String,
                                 nino: String,
                                 selfEmploymentIncomeSourceIds: Option[List[String]],
                                 propertyIncomeSourceId: Option[String])
  private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

  private val seIds = if (selfEmploymentIds.nonEmpty) Some(selfEmploymentIds) else None

  override val detail: JsValue = Json.toJson(
    AuditDetail(
      mtditid,
      nino,
      seIds,
      propertyIncomeId
    )
  )
}