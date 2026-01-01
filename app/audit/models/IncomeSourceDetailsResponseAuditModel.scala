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

import audit.Utilities._
import auth.authV2.models.AuthorisedAndEnrolledRequest
import enums.MTDSupportingAgent
import play.api.libs.json.{JsValue, Json}
case class IncomeSourceDetailsResponseAuditModel(
                                                  mtdItUser: AuthorisedAndEnrolledRequest[_],
                                                  nino: String,
                                                  selfEmploymentIds: List[String],
                                                  propertyIncomeIds: List[String],
                                                  yearOfMigration: Option[String]
                                                ) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.IncomeSourceDetailsResponse
  override val auditType: String = enums.AuditType.IncomeSourceDetailsResponse

  override val detail: JsValue = {
    Json.obj("mtditid" -> mtdItUser.mtditId,
      "selfEmploymentIncomeSourceIds" -> selfEmploymentIds,
      "propertyIncomeSourceIds" -> propertyIncomeIds) ++
      Json.obj("agentReferenceNumber"-> mtdItUser.authUserDetails.agentReferenceNumber) ++
      Json.obj("saUtr"-> mtdItUser.saUtr) ++
      userType(mtdItUser.authUserDetails.affinityGroup, mtdItUser.mtdUserRole == MTDSupportingAgent) ++
      Json.obj("credId"-> mtdItUser.authUserDetails.credId) ++
      Json.obj("nino"-> Some(nino)) ++
      Json.obj("dateOfMigration"-> yearOfMigration)
  }
}
