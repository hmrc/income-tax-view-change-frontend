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

import auth.authV2.models.AuthorisedAndEnrolledRequest
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

case class AccessDeniedForSupportingAgentAuditModel(mtdItUser: AuthorisedAndEnrolledRequest[_]) extends ExtendedAuditModel {
  override val transactionName: String = enums.TransactionName.AccessDeniedForSupportingAgent
  override val detail: JsValue = {
    Json.obj("mtditid" -> mtdItUser.mtditId,
      "agentReferenceNumber" -> mtdItUser.authUserDetails.agentReferenceNumber,
      "saUtr" -> mtdItUser.saUtr,
      "userType" -> "Agent",
      "isSupportingAgent" -> true,
      "credId" -> mtdItUser.authUserDetails.credId,
      "nino" -> mtdItUser.clientDetails.map(_.nino),
    "clientName" -> mtdItUser.optClientNameAsString)
  }
  override val auditType: String = enums.AuditType.AuditType.AccessDeniedForSupportingAgent
}
