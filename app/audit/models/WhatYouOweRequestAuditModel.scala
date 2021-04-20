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

import play.api.libs.json.{JsValue, Json, Writes}


case class WhatYouOweRequestAuditModel(arn: Option[String], userType: Option[String],
                                       saUtr: Option[String], nino: String,
                                       credId: Option[String], mtditid: String) extends ExtendedAuditModel {

  override val transactionName: String = "what-you-owe-request"
  override val auditType: String = "WhatYouOweRequest"

  private case class AuditDetail(agentReferenceNumber: Option[String], userType: Option[String],
                                 saUtr: Option[String], nationalInsuranceNumber: String,
                                 credId: Option[String], mtditid: String)

private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

  override val detail: JsValue =
    Json.toJson(AuditDetail(arn, userType, saUtr, nino, credId, mtditid))

}
