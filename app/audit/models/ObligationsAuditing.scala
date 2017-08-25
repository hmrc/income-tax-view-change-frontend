/*
 * Copyright 2017 HM Revenue & Customs
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
import models.{IncomeSourcesModel}

object ObligationsAuditing {

  val obligationTransactionName = "ITVCObligations"
  val obligationAuditType = "obligationStuff"

  case class ObligationsAuditModel(user: MtdItUser, sources: IncomeSourcesModel) extends AuditModel {
    override val transactionName: String = obligationTransactionName
    override val detail: Map[String, String] = Map(
      "mtdid" -> user.mtditid,
      "nino" -> user.nino
    "prop" -> sources.propertyDetails
    )
    override val auditType: String = obligationAuditType
  }

}
