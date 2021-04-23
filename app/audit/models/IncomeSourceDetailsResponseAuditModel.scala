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

import audit.Utilities._
import auth.MtdItUserWithNino
import play.api.libs.json.{JsValue, Json}
import utils.Utilities._

case class IncomeSourceDetailsResponseAuditModel(mtdItUser: MtdItUserWithNino[_],
                                                 selfEmploymentIds: List[String],
                                                 propertyIncomeId: Option[String],
                                                 yearOfMigration: Option[String]) extends ExtendedAuditModel {

  override val transactionName: String = "income-source-details-response"
  override val auditType: String = "incomeSourceDetailsResponse"

  override val detail: JsValue = {
    Json.obj("mtditid" -> mtdItUser.mtditid,
      "nationalInsuranceNumber" -> mtdItUser.nino,
      "selfEmploymentIncomeSourceIds" -> selfEmploymentIds) ++
      ("agentReferenceNumber", mtdItUser.arn) ++
      ("saUtr", mtdItUser.saUtr) ++
      userType(mtdItUser.userType) ++
      ("credId", mtdItUser.credId) ++
      ("propertyIncomeSourceId", propertyIncomeId) ++
      ("dateOfMigration", yearOfMigration)
  }
}
