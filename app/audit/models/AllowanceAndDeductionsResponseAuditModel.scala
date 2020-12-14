/*
 * Copyright 2020 HM Revenue & Customs
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

case class AllowanceAndDeductionsResponseAuditModel(mtditid: String, nino: String,
                                                    saUtr: Option[String], credId: Option[String],
                                                    userType: Option[String], personalAllowance: Option[BigDecimal],
                                                    pensionContributions: Option[BigDecimal]) extends ExtendedAuditModel {

  override val transactionName: String = "allowances-deductions-details-response"
  override val auditType: String = "AllowancesDeductionsDetailsResponse"

  private case class AuditDetail(mtditid: String, nationalInsuranceNumber: String,
                                 saUtr: Option[String], credId: Option[String],
                                 userType: Option[String], personalAllowance: Option[String],
                                 pensionContributions: Option[String])

  private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

  override val detail: JsValue = Json.toJson(
    AuditDetail(
      mtditid, nino, saUtr, credId, userType,
      personalAllowance.map(_.toString()), pensionContributions.map(_.toString())
    )
  )
}
