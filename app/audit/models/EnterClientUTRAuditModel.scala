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

case class EnterClientUTRAuditModel(isSuccessful: Boolean,
                                    nino: String,
                                    mtditid: String,
                                    arn: Option[String],
                                    saUtr: String,
                                    credId: Option[String],
                                    isSupportingAgent: Option[Boolean]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.EnterClientUTR

  override val auditType: String = enums.AuditType.EnterClientUTR

  private val successOutcome: JsObject = Json.obj(
    "outcome" ->
      Json.obj("isSuccessful" -> isSuccessful)
  )
  private val failureOutcome: JsObject = Json.obj(
    "outcome" ->
      Json.obj(
        "isSuccessful" -> isSuccessful,
        "failureCategory" -> "API_FAILURE",
        "failureReason" -> "API returned error - unable to login agent"
      )
  )

  private val outcome: JsObject = if (isSuccessful) successOutcome else failureOutcome

  private val userType: JsObject = isSupportingAgent.fold(Json.obj("userType" -> "Agent"))(isASupportingAgent => Json.obj(
    "userType" -> "Agent", "isSupportingAgent" -> isASupportingAgent
  ))

  private val userDetailsJson: JsObject = Json.obj(
    "nino" -> nino,
    "mtditid" -> mtditid,
    "saUtr" -> saUtr) ++
    userType ++
    Json.obj("credId"-> credId) ++
    Json.obj("agentReferenceNumber"-> arn)

  override val detail: JsValue = userDetailsJson ++ outcome

}
