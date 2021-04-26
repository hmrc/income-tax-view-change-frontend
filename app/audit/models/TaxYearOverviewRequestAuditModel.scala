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

import auth.MtdItUser
import play.api.libs.json.{JsValue, Json}
import utils.Utilities._
import audit.Utilities._

case class TaxYearOverviewRequestAuditModel(mtdItUser: MtdItUser[_],
                                            agentReferenceNumber: Option[String]) extends ExtendedAuditModel {

  override val transactionName: String = "tax-year-overview-request"
  override val auditType: String = "TaxYearOverviewRequest"


  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> mtdItUser.nino,
      "mtditid" -> mtdItUser.mtditid) ++
      ("agentReferenceNumber", agentReferenceNumber) ++
      ("saUtr", mtdItUser.saUtr) ++
      ("credId", mtdItUser.credId) ++
      userType(mtdItUser.userType)
  }

}
