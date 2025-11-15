/*
 * Copyright 2025 HM Revenue & Customs
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

package audit.reporting_obligations

import enums.MTDUserRole
import play.api.libs.json.{JsObject, Json, OFormat}

case class ReportingObligationsAuditModel(
                                           agentReferenceNumber: Option[String],
                                           auditType: String,
                                           credId: Option[String],
                                           mtditid: String,
                                           nino: String,
                                           saUtr: Option[String],
                                           userType: MTDUserRole,
                                           grossIncomeThreshold: String,
                                           crystallisationStatusForPreviousTaxYear: Boolean,
                                           itsaStatusTable: List[ItsaStatusTableDetails],
                                           links: List[String]
                                         ) {

  def detail: JsObject = {
    Json.obj(
      "auditType" -> auditType,
      "mtditid" -> mtditid,
      "nino" -> nino,
      "userType" -> userType.toString,
      "grossIncomeThreshold" -> grossIncomeThreshold,
      "crystallisationStatusForPreviousTaxYear" -> crystallisationStatusForPreviousTaxYear,
      "itsaStatusTable" -> Json.toJson(itsaStatusTable),
      "links" -> links
    ) ++
      agentReferenceNumber.fold(Json.obj())(value => Json.obj("agentReferenceNumber" -> value)) ++
      credId.fold(Json.obj())(value => Json.obj("credId" -> value)) ++
      saUtr.fold(Json.obj())(value => Json.obj("saUtr" -> value))
  }

}

object ReportingObligationsAuditModel {

  implicit val format: OFormat[ReportingObligationsAuditModel] = Json.format[ReportingObligationsAuditModel]
}
