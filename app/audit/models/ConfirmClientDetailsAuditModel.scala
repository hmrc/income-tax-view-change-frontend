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

import play.api.libs.json.{JsObject, JsValue, Json}

case class ConfirmClientDetailsAuditModel(clientName: String,
                                          nino: String,
                                          mtditid: String,
                                          arn: String,
                                          saUtr: String,
                                          isSupportingAgent: Boolean,
                                          credId: Option[String]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.ClientDetailsConfirmed

  override val auditType: String = enums.AuditType.ClientDetailsConfirmed

  private val userDetailsJson: JsObject = Json.obj(
    "nino" -> nino,
    "mtditid" -> mtditid,
    "saUtr" -> saUtr,
    "agentReferenceNumber" -> arn,
    "userType" -> "Agent",
    "isSupportingAgent" -> isSupportingAgent
  ) ++
    Json.obj("credId" -> credId)

  override val detail: JsValue = userDetailsJson ++ Json.obj("clientName" -> clientName)
}
