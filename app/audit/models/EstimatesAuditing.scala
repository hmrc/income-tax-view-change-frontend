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
import models.IncomeSourcesModel

object EstimatesAuditing {

  val estimatesTransactionName = "ITVCEstimates"
  val estimatesAuditType = "estimatesPageView"

  case class EstimatesAuditModel(user: MtdItUser, sources: IncomeSourcesModel, estimate: String) extends AuditModel {
    override val transactionName: String = estimatesTransactionName
    override val detail: Map[String, String] = Map(
      "mtdid" -> user.mtditid,
      "nino" -> user.nino,
      "hasBusiness" -> sources.hasBusinessIncome.toString,
      "hasProperty" -> sources.hasPropertyIncome.toString,
      "bizAccPeriodStart" -> sources.businessDetails.fold("-")(x => s"${x.accountingPeriod.start}"),
      "bizAccPeriodEnd" -> sources.businessDetails.fold("-")(x => s"${x.accountingPeriod.end}"),
      "propAccPeriodStart" -> sources.propertyDetails.fold("-")(x => s"${x.accountingPeriod.start}"),
      "propAccPeriodEnd" -> sources.propertyDetails.fold("-")(x => s"${x.accountingPeriod.end}"),
      "currentEstimate" -> estimate
    )
    override val auditType: String = estimatesAuditType
  }

}
